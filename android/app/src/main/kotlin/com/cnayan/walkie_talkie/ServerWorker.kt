package com.cnayan.walkie_talkie

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cnayan.walkie_talkie.models.Device
import com.cnayan.walkie_talkie.servers.TcpFileServer
import com.cnayan.walkie_talkie.utils.Compression
import com.cnayan.walkie_talkie.utils.NotificationHelper
import com.cnayan.walkie_talkie.utils.network.service_discovery.NSD

class ServerWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private var tcpServer: TcpFileServer? = null
    private var _started: Boolean = false

    companion object {
        private val TAG = "ServerWorker"
    }

    override fun doWork(): Result {
        startServers()
        return Result.success()
    }

    private fun startServers() {
        if (!_started) {
            NSD(applicationContext).apply {
                registerDevice()
                startDiscovering()
            }

            startTcpServer()
            _started = true
        }
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

        val decompressedBytes: ByteArray = Compression.decompressGZip(bytes) ?: return

        Log.d(TAG, "Decompressed data received - ${decompressedBytes.size}")

        val size = decompressedBytes[0].toInt()
        val deviceStr = String(decompressedBytes.copyOfRange(1, size + 1))
        val device = Device.from(deviceStr)

        val audioBytes = decompressedBytes.copyOfRange(size + 1, bytes.size)
        Log.d(TAG, "Audio data received - ${audioBytes.size}")

        NotificationHelper.addNotification(applicationContext, device)

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
            .setBufferSizeInBytes(audioBytes.size)
            .build()

        player.write(audioBytes, 0, audioBytes.size)

        player.play()
    }
}
