package com.cnayan.walkie_talkie

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters


class ServerWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    private val TAG = "ServerWorker"
    private lateinit var multicastLock: WifiManager.MulticastLock

    private var tcpServer: TcpFileServer? = null
    private var udpListener: UdpPingServer? = null
    private var _started: Boolean = false

    companion object {
        private val NOTIFICATION_CHANNEL_ID = "com.cnayan.walkie_talkie/AudioMessageComingInChannel"
    }

    override fun doWork(): Result {
        startServers()
        return Result.success()
    }

    @SuppressLint("WifiManagerLeak")
    private fun startServers() {
        if (!_started) {
            val wifi: WifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wifi.createMulticastLock(TAG).apply {
                setReferenceCounted(true)
                acquire()
            }

            Utils.Instance.registerNSDService(applicationContext)

            startTcpServer()

            _started = true
        }
    }

    private fun startPingServer() {
        udpListener = UdpPingServer()
        val threadWithRunnable = Thread(udpListener!!)
        threadWithRunnable.start()
    }

    private fun startTcpServer() {
        tcpServer = TcpFileServer()
        tcpServer!!.listener = { bytes ->
            audioDataReceived(bytes)
        }

        val threadWithRunnable = Thread(tcpServer!!)
        threadWithRunnable.start()
    }

    private fun audioDataReceived(bytes: ByteArray) {
        Log.d(TAG, "Data received - ${bytes.size}")

        var size = bytes[0].toInt()
        var deviceBytes = bytes.copyOfRange(1, size + 1)
        var deviceStr = String(deviceBytes)
        var device = Device.from(deviceStr)

        var compressedBytes = bytes.copyOfRange(size + 1, bytes.size)

        var decompressedBytes: ByteArray? = CompressionUtils.decompressGZip(compressedBytes)
        if (decompressedBytes != null) {
            Log.d(TAG, "Decompressed data received - ${decompressedBytes.size}")
            addNotification(device)

            val player = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(16000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(decompressedBytes.size)
                .build()

            player.write(decompressedBytes, 0, decompressedBytes.size)

            player.play()
        }
    }

    private fun addNotification(host: Device) {
        var pattern = longArrayOf(VibrationEffect.DEFAULT_AMPLITUDE.toLong())

        var n = "${host.name} (${host.ip})"

        val manager: NotificationManager? =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager?.notificationChannels?.any { e -> e.id == NOTIFICATION_CHANNEL_ID } != true) {
            var notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Walkie Talkie Audio Message Channel",
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationChannel.enableVibration(true)
            manager?.createNotificationChannel(notificationChannel)
        }

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
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
