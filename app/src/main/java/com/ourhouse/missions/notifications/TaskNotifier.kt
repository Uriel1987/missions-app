package com.ourhouse.missions.notifications

import android.content.Context
import com.ourhouse.missions.data.local.SessionManager
import com.ourhouse.missions.data.model.Task
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/** One "this was just assigned to you" event, identified by a stable [key]. */
internal class NotificationCandidate(val key: String, val title: String, val body: String)

/**
 * The notification, if any, that [task] should produce on [me]'s phone. One event per
 * (task, assignment): [Task.assignedAt] changes every time the assignee is set, whether
 * by creating the task, editing it, or passing it over.
 */
internal fun notificationCandidateFor(task: Task, me: String): NotificationCandidate? {
    val assignedAt = task.assignedAt ?: return null
    if (task.completed || task.assigneeId != me) return null
    if (task.assignedBy == null || task.assignedBy == me) return null // I assigned it to myself

    val fromName = task.assignedByName ?: "בן/בת הזוג"
    val isHandoff = task.lastHandoffAt?.time == assignedAt.time
    var body = "\"${task.title}\""
    if (isHandoff) task.lastHandoffMessage?.takeIf { it.isNotBlank() }?.let { body += " — $it" }

    val title = if (isHandoff) "$fromName העביר/ה אליך אחריות" else "$fromName הוסיף/ה לך משימה"
    return NotificationCandidate("a|${task.id}|${assignedAt.time}", title, body)
}

/**
 * Shows a local notification when a task is assigned to this phone ("assigned to you" /
 * "responsibility was passed to you"). This replaces the Cloud Function + FCM push path,
 * which needs the paid Blaze plan: instead each phone watches the shared task list itself —
 * live while the app process is alive (kept alive by [WatchService]), and via
 * [TaskWatchWorker] roughly every 15 minutes as a safety net.
 *
 * Every event gets a stable key; [SessionManager] remembers which keys were already
 * shown so the live watcher and the worker never notify twice for the same event.
 */
class TaskNotifier(
    private val context: Context,
    private val sessionManager: SessionManager
) {
    private val mutex = Mutex()

    /** [tasks] must be a server-confirmed list (not a possibly-empty local cache). */
    suspend fun process(tasks: List<Task>, myUserId: String) = mutex.withLock {
        val candidates = tasks.mapNotNull { notificationCandidateFor(it, myUserId) }
        val saved = sessionManager.readNotificationState()

        // Forget keys of tasks that no longer exist so the stored set can't grow forever.
        val liveTaskIds = tasks.mapTo(HashSet()) { it.id }
        val notified = saved.notifiedKeys.filterTo(HashSet()) { taskIdOf(it) in liveTaskIds }

        if (!saved.baselined) {
            // First server-confirmed list after pairing: record what already exists silently.
            notified += candidates.map { it.key }
            sessionManager.saveNotificationState(SessionManager.NotificationState(true, notified))
            return@withLock
        }

        for (candidate in candidates) {
            if (!notified.add(candidate.key)) continue
            NotificationHelper.show(context, candidate.title, candidate.body, abs(candidate.key.hashCode()))
        }
        if (notified != saved.notifiedKeys) {
            sessionManager.saveNotificationState(SessionManager.NotificationState(true, notified))
        }
    }

    private fun taskIdOf(key: String) = key.split("|").getOrElse(1) { "" }
}
