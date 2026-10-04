package com.ourhouse.missions.notifications

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.ourhouse.missions.data.local.SessionManager
import com.ourhouse.missions.data.model.Task
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * Decides which tasks deserve a local notification ("a task was assigned to you" /
 * "responsibility was passed to you") and shows them. This replaces the Cloud Function +
 * FCM push path, which needs the paid Blaze plan: instead each phone watches the shared
 * task list itself — live while the app process is alive, and via [TaskWatchWorker]
 * roughly every 15 minutes otherwise.
 *
 * Every event gets a stable key; [SessionManager] remembers which keys were already
 * shown so the live watcher and the worker never notify twice for the same event.
 */
class TaskNotifier(
    private val context: Context,
    private val sessionManager: SessionManager
) {
    private val mutex = Mutex()

    private class Pending(val key: String, val title: String, val body: String)

    /** [tasks] must be a server-confirmed list (not a possibly-empty local cache). */
    suspend fun process(tasks: List<Task>, myUserId: String) = mutex.withLock {
        val candidates = tasks.mapNotNull { candidateFor(it, myUserId) }
        val saved = sessionManager.readNotificationState()

        // Forget keys of tasks that no longer exist so the stored set can't grow forever.
        val liveTaskIds = tasks.mapTo(HashSet()) { it.id }
        val notified = saved.notifiedKeys.filterTo(HashSet()) { taskIdOf(it) in liveTaskIds }

        if (!saved.baselined) {
            notified += candidates.map { it.key }
            sessionManager.saveNotificationState(SessionManager.NotificationState(true, notified))
            return@withLock
        }

        // While the app is on screen the list already updates in front of the user,
        // so mark events as seen without also popping a notification.
        val appVisible = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        for (pending in candidates) {
            if (!notified.add(pending.key)) continue
            if (!appVisible) {
                NotificationHelper.show(context, pending.title, pending.body, abs(pending.key.hashCode()))
            }
        }
        if (notified != saved.notifiedKeys) {
            sessionManager.saveNotificationState(SessionManager.NotificationState(true, notified))
        }
    }

    private fun candidateFor(task: Task, me: String): Pending? {
        if (task.completed || task.assigneeId != me) return null

        val handoffAt = task.lastHandoffAt
        if (handoffAt != null) {
            val from = task.lastHandoffFrom
            if (from == null || from == me) return null
            val fromName = task.lastHandoffFromName ?: "בן/בת הזוג"
            var body = "\"${task.title}\""
            task.lastHandoffMessage?.takeIf { it.isNotBlank() }?.let { body += " — $it" }
            return Pending("h|${task.id}|${handoffAt.time}", "$fromName העביר/ה אליך אחריות", body)
        }

        if (task.createdBy != me) {
            return Pending("n|${task.id}", "נוספה לך משימה חדשה", "\"${task.title}\"")
        }
        return null
    }

    private fun taskIdOf(key: String) = key.split("|").getOrElse(1) { "" }
}
