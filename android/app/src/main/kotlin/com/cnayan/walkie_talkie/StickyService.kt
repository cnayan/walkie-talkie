package com.cnayan.walkie_talkie

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest

import com.cnayan.walkie_talkie.RestartServiceReceiver.Companion.CHANNEL_ID
import com.github.druk.dnssd.BrowseListener
import com.github.druk.dnssd.DNSSDService

class StickyService : Service() {
    private val TAG = "StickyService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Nayan: onStartCommand")

        createNotificationChannelsAndServers()

        return START_NOT_STICKY
    }

    private fun createNotificationChannelsAndServers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager: NotificationManager? = getSystemService(NotificationManager::class.java)

            //if already created, don't recreate
            if (manager?.activeNotifications?.any {
                    it.notification.channelId == CHANNEL_ID
                } == false) {
                val serviceChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Walkie Talkie Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )

                manager.createNotificationChannel(serviceChannel)

                showIntent()
                enqueueJob()
            }
        }
    }

    private fun showIntent() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText("Hello")
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun enqueueJob() {
        val startServersWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<ServerWorker>().build()
        WorkManager
            .getInstance(applicationContext)
            .enqueue(startServersWorkRequest)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}