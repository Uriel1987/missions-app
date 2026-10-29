package com.ourhouse.missions.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ourhouse.missions.MissionsApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Restarts the background watcher after the phone reboots (boot receivers are one of the
 *  few places Android lets an app start a foreground service from the background). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val session = (context.applicationContext as MissionsApp).container.sessionManager
        val shouldRun = runBlocking { session.householdId.first() != null && session.backgroundWatchEnabled.first() }
        if (shouldRun) WatchService.start(context)
    }
}
