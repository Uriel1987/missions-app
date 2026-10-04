package com.ourhouse.missions.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.ourhouse.missions.data.model.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Reads and writes tasks under households/{householdId}/tasks.
 *
 * A task's optional photo is stored (already shrunk to a few hundred KB) as a base64
 * string in households/{householdId}/images/{taskId} — a separate document so the
 * task list that every screen listens to stays small. No Firebase Storage / Cloud
 * Functions are used, so the whole app runs on the free Spark plan.
 */
class TaskRepository(private val firestore: FirebaseFirestore) {

    private fun tasksCollection(householdId: String) =
        firestore.collection("households").document(householdId).collection("tasks")

    private fun imagesCollection(householdId: String) =
        firestore.collection("households").document(householdId).collection("images")

    /** Live, always-up-to-date task list — this is what keeps both phones in sync. */
    fun observeTasks(householdId: String): Flow<List<Task>> = callbackFlow {
        val registration = tasksCollection(householdId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { it.toObject(Task::class.java) }.orEmpty()
                trySend(tasks)
            }
        awaitClose { registration.remove() }
    }

    /** Same as [observeTasks] but only emits data confirmed by the server (never the
     *  possibly-empty local cache) — what the notification logic needs to avoid
     *  mistaking "cache not populated yet" for "nothing assigned to me". */
    fun observeServerTasks(householdId: String): Flow<List<Task>> = callbackFlow {
        val registration = tasksCollection(householdId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.metadata.isFromCache) return@addSnapshotListener
                trySend(snapshot.documents.mapNotNull { it.toObject(Task::class.java) })
            }
        awaitClose { registration.remove() }
    }

    /** One-shot server read, used by the periodic background check. */
    suspend fun fetchServerTasks(householdId: String): List<Task> =
        tasksCollection(householdId).get(Source.SERVER).await()
            .documents.mapNotNull { it.toObject(Task::class.java) }

    /** Writes the task and (if given) its photo in one atomic batch. */
    suspend fun addTask(householdId: String, task: Task, imageBase64: String? = null): Result<String> = runCatching {
        val doc = tasksCollection(householdId).document()
        val batch = firestore.batch()
        batch.set(doc, task.copy(id = doc.id, hasImage = imageBase64 != null))
        if (imageBase64 != null) {
            batch.set(imagesCollection(householdId).document(doc.id), mapOf("data" to imageBase64))
        }
        batch.commit().await()
        doc.id
    }

    suspend fun loadImage(householdId: String, taskId: String): String? = runCatching {
        imagesCollection(householdId).document(taskId).get().await().getString("data")
    }.getOrNull()

    suspend fun updateTask(householdId: String, taskId: String, updates: Map<String, Any?>): Result<Unit> =
        runCatching { tasksCollection(householdId).document(taskId).update(updates).await() ; Unit }

    /** The "pass responsibility" flow: reassigns the task and drops a note + timestamp
     *  that the task-detail screen and the partner's notification check both read. */
    suspend fun passResponsibility(
        householdId: String,
        taskId: String,
        fromUserId: String,
        fromUserName: String?,
        toUserId: String,
        message: String?
    ): Result<Unit> = updateTask(
        householdId, taskId,
        mapOf(
            "assigneeId" to toUserId,
            "lastHandoffFrom" to fromUserId,
            "lastHandoffFromName" to fromUserName,
            "lastHandoffMessage" to message,
            "lastHandoffAt" to Date()
        )
    )

    suspend fun setCompleted(householdId: String, taskId: String, byUserId: String, completed: Boolean): Result<Unit> =
        updateTask(
            householdId, taskId,
            mapOf("completed" to completed, "completedBy" to if (completed) byUserId else null)
        )

    suspend fun deleteTask(householdId: String, taskId: String): Result<Unit> = runCatching {
        firestore.batch()
            .delete(tasksCollection(householdId).document(taskId))
            .delete(imagesCollection(householdId).document(taskId))
            .commit().await()
        Unit
    }
}
