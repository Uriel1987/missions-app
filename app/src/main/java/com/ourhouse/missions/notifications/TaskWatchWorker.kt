package com.ourhouse.missions.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ourhouse.missions.MissionsApp
import kotlinx.coroutines.flow.first

/** Periodic background check (WorkManager's minimum interval is 15 minutes) that catches
 *  assignments made while the app process isn't alive to listen live. */
class TaskWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MissionsApp).container
        val householdId = container.sessionManager.householdId.first() ?: return Result.success()
        val myUserId = container.authRepository.currentUserId ?: return Result.success()

        return runCatching {
            container.taskNotifier.process(container.taskRepository.fetchServerTasks(householdId), myUserId)
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
