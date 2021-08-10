package com.cnayan.walkie_talkie

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.cnayan.walkie_talkie.utils.MDNSSD
import com.cnayan.walkie_talkie.utils.SoundRecorder
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class MainActivity : FlutterActivity() {
    private lateinit var _deviceDiscoveryMethodChannel: MethodChannel
    private lateinit var _recordingMethodChannel: MethodChannel
    private val _soundRecorder: SoundRecorder = SoundRecorder()

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
        _recordingMethodChannel = MethodChannel(
            flutterEngine!!.dartExecutor.binaryMessenger,
            AUDIO_PUBLISH_METHOD_CHANNEL
        )

        _recordingMethodChannel.setMethodCallHandler(object : MethodChannel.MethodCallHandler {
            override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
                when (call.method) {
                    "startRecording" -> {
                        _soundRecorder.listener = { bytes ->
                            Log.d(TAG, "Recorder: Sending bytes to Flutter: ${bytes.size}")
                            CoroutineScope(Main).launch {
                                _recordingMethodChannel.invokeMethod("audioBytes", bytes)
                            }
                        }

                        _soundRecorder.startRecording()

                        result.success(true)
                    }
                    "stopRecording" -> {
                        _soundRecorder.listener = null
                        _soundRecorder.stopRecording()
                        result.success(true)
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
        })
    }

    private fun setupMDNSChannel() {
        _deviceDiscoveryMethodChannel = MethodChannel(
            flutterEngine!!.dartExecutor.binaryMessenger,
            NSD_PUBLISH_METHOD_CHANNEL
        )

        _deviceDiscoveryMethodChannel.setMethodCallHandler(object :
            MethodChannel.MethodCallHandler {
            override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
                when (call.method) {
                    "getDevices" -> {
                        result.success(MDNSSD.devices.toList())
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
        })
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, WalkieTalkieForegroundService::class.java))
        }
    }
}
