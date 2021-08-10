package com.cnayan.walkie_talkie

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.github.druk.dnssd.BrowseListener
import com.github.druk.dnssd.DNSSDService
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FlutterActivity(), BrowseListener {
    private lateinit var _mnsdMethodChannel: MethodChannel
    private lateinit var _recordingMethodChannel: MethodChannel
    private var _devices: ArrayList<String> = ArrayList()
    private val _soundRecorder: SoundRecorder = SoundRecorder()

    companion object {
        private const val NSD_PUBLISH_CHANNEL: String = "com.cnayan.walkie-talkie/nsd-publish"
        private const val AUDIO_PUBLISH_CHANNEL: String = "com.cnayan.walkie-talkie/audio-publish"
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _mnsdMethodChannel = MethodChannel(
            flutterEngine!!.dartExecutor.binaryMessenger,
            NSD_PUBLISH_CHANNEL
        )

        _recordingMethodChannel = MethodChannel(
            flutterEngine!!.dartExecutor.binaryMessenger,
            AUDIO_PUBLISH_CHANNEL
        )

        _mnsdMethodChannel.setMethodCallHandler(object : MethodChannel.MethodCallHandler {
            override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
                when (call.method) {
                    "getDevices" -> {
                        _devices = ArrayList(_devices.distinct())
                        result.success(_devices)
                    }
                    else -> {
                        result.notImplemented()
                    }
                }
            }
        })

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

        val self = this
        CoroutineScope(IO).launch {
            startMyService()
            Utils.Instance.registerDeviceDiscoveryListener(context, self)
        }
    }

    private fun startMyService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, StickyService::class.java))
        }
    }

    override fun serviceFound(
        browser: DNSSDService?,
        flags: Int,
        ifIndex: Int,
        serviceName: String?,
        regType: String?,
        domain: String?,
    ) {
        Log.d(TAG, "Found $serviceName")
        if (serviceName?.isNotEmpty() == true) {
            _devices.add(serviceName)
        }
    }

    override fun serviceLost(
        browser: DNSSDService?,
        flags: Int,
        ifIndex: Int,
        serviceName: String?,
        regType: String?,
        domain: String?,
    ) {
        Log.d(TAG, "Lost $serviceName")
        if (serviceName?.isNotEmpty() == true) {
            _devices.remove(serviceName)
        }
    }

    override fun operationFailed(service: DNSSDService, errorCode: Int) {
        Log.e("TAG", "error: $errorCode")
    }
}
