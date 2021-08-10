package com.cnayan.walkie_talkie.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat
import com.cnayan.walkie_talkie.MainActivity
import com.cnayan.walkie_talkie.R
import com.cnayan.walkie_talkie.models.Device

class NotificationHelper {
    companion object {
        private val WALKIE_TALKIE_NOTIFICATION_CHANNEL_ID =
            "com.cnayan.walkie_talkie/AudioMessageComingInChannel"

        fun addNotification(applicationContext: Context, host: Device) {
            var pattern = longArrayOf(VibrationEffect.DEFAULT_AMPLITUDE.toLong())

            var n = "${host.name} (${host.ip})"

            val manager: NotificationManager? =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager?.notificationChannels?.any { e -> e.id == WALKIE_TALKIE_NOTIFICATION_CHANNEL_ID } != true) {
                var notificationChannel = NotificationChannel(
                    WALKIE_TALKIE_NOTIFICATION_CHANNEL_ID,
                    "Walkie Talkie Audio Message Channel",
                    NotificationManager.IMPORTANCE_HIGH
                )

                notificationChannel.enableVibration(true)
                manager?.createNotificationChannel(notificationChannel)
            }

            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(applicationContext,
                    WALKIE_TALKIE_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_icon) //set icon for notification
                    .setContentTitle("Walkie Talkie") //set title of notification
                    .setContentText("Incoming message from $n") //this is notification message
                    .setAutoCancel(true) // makes auto cancel of notification
                    .setVibrate(pattern)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT) //set priority of notification

            val notificationIntent = Intent(applicationContext, MainActivity::class.java)
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            //notification message will get at NotificationView
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                applicationContext, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            builder.setContentIntent(pendingIntent)

            // Add as notification
            manager?.notify(0, builder.build())
        }
    }
}