package com.cnayan.walkie_talkie

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.cnayan.walkie_talkie.utils.SoundRecorder
import com.cnayan.walkie_talkie.utils.network.service_discovery.NSD
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class MainActivity : FlutterActivity() {
    private lateinit var deviceDiscoveryMethodChannel: MethodChannel
    private lateinit var recordingMethodChannel: MethodChannel
    private val soundRecorder: SoundRecorder = SoundRecorder()

    companion object {
        private const val AUDIO_PUBLISH_METHOD_CHANNEL: String =
            "com.cnayan.walkie-talkie/audio-publish"
        private const val NSD_PUBLISH_METHOD_CHANNEL: String =
            "com.cnayan.walkie-talkie/nsd-publish"
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupMDNSChannel()
        setupRecordingChannel()

        startForegroundService()
    }

    private fun setupRecordingChannel() {
        recordingMethodChannel = MethodChannel(
            flutterEngine!!.dartExecutor.binaryMessenger,
            AUDIO_PUBLISH_METHOD_CHANNEL
        )

        recordingMethodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "startRecording" -> {
                    soundRecorder.listener = { bytes ->
                        Log.d(TAG, "Recorder: Sending bytes to Flutter: ${bytes.size}")
                        CoroutineScope(Main).launch {
                            recordingMethodChannel.invokeMethod("audioBytes", bytes)
                        }
                    }

                    soundRecorder.startRecording()

                    result.success(true)
                }
                "stopRecording" -> {
                    soundRecorder.listener = null
                    soundRecorder.stopRecording()
                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun setupMDNSChannel() {
        deviceDiscoveryMethodChannel = MethodChannel(
            flutterEngine!!.dartExecutor.binaryMessenger,
            NSD_PUBLISH_METHOD_CHANNEL
        )

        deviceDiscoveryMethodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "getDevices" -> {
                    result.success(NSD.devices.toList())
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, WalkieTalkieForegroundService::class.java))
        }
    }
}
