package com.ourhouse.missions.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ourhouse.missions.MainActivity
import com.ourhouse.missions.R

/**
 * Does nothing itself except keep the app process alive at foreground priority. The actual
 * listening is the Firestore listener in [com.ourhouse.missions.MissionsApp]; without this
 * service Android freezes or kills a backgrounded app within minutes and assignment
 * notifications stop arriving. The price is one small, silent, permanent notification.
 */
class WatchService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        val openApp = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_missions)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.watch_service_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        return START_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "missions_watch"
        private const val NOTIFICATION_ID = 4242

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.watch_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        /** Safe to call from anywhere: Android refuses to start a foreground service from the
         *  background in some situations, in which case the periodic check still covers us. */
        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, WatchService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WatchService::class.java))
        }
    }
}
