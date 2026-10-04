package com.ourhouse.missions

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ourhouse.missions.notifications.NotificationHelper
import com.ourhouse.missions.notifications.TaskWatchWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MissionsApp : Application() {

    /** Shared by the UI and the background notification logic. */
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Firebase initializes automatically from google-services.json (see README).
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        scheduleBackgroundCheck()
        startLiveWatcher()
    }

    /** Catches assignments made while this process wasn't alive to listen live. */
    private fun scheduleBackgroundCheck() {
        val request = PeriodicWorkRequestBuilder<TaskWatchWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("task-watch", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** While the app process is alive (even in the background) keep listening to the
     *  shared task list and notify as soon as something is assigned to this phone. */
    private fun startLiveWatcher() {
        appScope.launch {
            container.sessionManager.householdId.filterNotNull().distinctUntilChanged().collectLatest { householdId ->
                while (isActive) {
                    // A listener error (offline, permission) just ends the flow; retry shortly.
                    runCatching {
                        container.taskRepository.observeServerTasks(householdId).collect { tasks ->
                            val myUserId = container.authRepository.currentUserId ?: return@collect
                            container.taskNotifier.process(tasks, myUserId)
                        }
                    }
                    delay(30_000)
                }
            }
        }
    }
}
