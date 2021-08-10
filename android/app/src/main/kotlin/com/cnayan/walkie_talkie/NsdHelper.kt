package com.cnayan.walkie_talkie


/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.druk.dnssd.*


class NsdHelper(private var _context: Context) {
    private val _broadcaster: LocalBroadcastManager
    private var _nsdManager: NsdManager

    //    private var _resolveListener: NsdManager.ResolveListener? = null
//    private var _discoveryListener: NsdManager.DiscoveryListener? = null
    private var _registrationListener: NsdManager.RegistrationListener? = null

    //    private var _chosenServiceInfo: NsdServiceInfo? = null
    private var _serviceName = ""

    init {
        _nsdManager = _context.getSystemService(Context.NSD_SERVICE) as NsdManager
        _broadcaster = LocalBroadcastManager.getInstance(_context)
    }

    companion object {
        //        const val BROADCAST_TAG = "NSDBroadcast"
//        const val KEY_SERVICE_INFO = "serviceinfo"
        const val SERVICE_TYPE = "_cnayan_walkie_talkie._udp"

        // There is an additional dot at the end of service name most probably by os, this is to
        // rectify that problem
//        const val SERVICE_TYPE_PLUS_DOT = SERVICE_TYPE + "."
        const val TAG = "NsdHelper"
    }

    fun registerService(serviceName: String, port: Int) {
        try {
            val dnssd: DNSSD = DNSSDBindable(_context)
            // "_rxdnssd._tcp"
            dnssd.register(serviceName, SERVICE_TYPE, port,
                object : RegisterListener {
                    override fun serviceRegistered(
                        registration: DNSSDRegistration, flags: Int,
                        serviceName: String, regType: String, domain: String
                    ) {
                        Log.i("TAG", "Register successfully ")
                    }

                    override fun operationFailed(service: DNSSDService, errorCode: Int) {
                        Log.e("TAG", "error $errorCode")
                    }
                })
        } catch (e: DNSSDException) {
            Log.e("TAG", "error", e)
        }
    }

//    fun initializeNsd() {
//        _resolveListener = object : NsdManager.ResolveListener {
//            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
//                Log.e(TAG, "Resolve failed$errorCode")
//            }
//
//            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
//                Log.v(TAG, "Resolve Succeeded. $serviceInfo")
//                if (serviceInfo.serviceName == serviceName) {
//                    Log.d(TAG, "Same IP.")
//                    return
//                }
//
//                _chosenServiceInfo = serviceInfo
//                val intent = Intent(BROADCAST_TAG)
//                _broadcaster.sendBroadcast(intent)
//            }
//        }
//    }

//    fun discoverServices() {
//        stopDiscovery() // Cancel any existing discovery request
//        initializeDiscoveryListener()
//        _nsdManager.discoverServices(
//            SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, _discoveryListener
//        )
//    }

    fun registerService_good(serviceName: String, port: Int) {
        tearDown() // Cancel any previous registration request
        initializeRegistrationListener()

        this._serviceName = serviceName

        val serviceInfo = NsdServiceInfo()
        serviceInfo.port = port
        serviceInfo.serviceName = serviceName
        serviceInfo.serviceType = SERVICE_TYPE

        Log.v(TAG, Build.MANUFACTURER + " registering service: " + port)
        _nsdManager.registerService(
            serviceInfo, NsdManager.PROTOCOL_DNS_SD, _registrationListener
        )
    }

//    private fun stopDiscovery() {
//        if (_discoveryListener != null) {
//            try {
//                _nsdManager.stopServiceDiscovery(_discoveryListener)
//            } finally {
//            }
//
//            _discoveryListener = null
//        }
//    }

    fun tearDown() {
        if (_registrationListener != null) {
            try {
                _nsdManager.unregisterService(_registrationListener)
            } finally {
            }

            _registrationListener = null
        }
    }

//    private fun initializeDiscoveryListener() {
//        _discoveryListener = object : NsdManager.DiscoveryListener {
//            override fun onDiscoveryStarted(regType: String) {
//                Log.d(TAG, "Service discovery started")
//            }
//
//            override fun onServiceFound(service: NsdServiceInfo) {
//                Log.d(TAG, "Service discovery success$service")
//                val serviceType = service.serviceType
//                Log.d(TAG, "Service discovery success: " + service.serviceName)
//                // For some reason the service type received has an extra dot with it, hence
//                // handling that case
//                val isOurService =
//                    serviceType == SERVICE_TYPE || serviceType == SERVICE_TYPE_PLUS_DOT
//                if (!isOurService) {
//                    Log.d(TAG, "Unknown Service Type: " + service.serviceType)
//                } else if (service.serviceName == serviceName) {
//                    Log.d(TAG, "Same machine: $serviceName")
//                } else if (service.serviceName.contains(serviceName)) {
//                    Log.d(
//                        TAG,
//                        "different machines. (" + service.serviceName + "-" + serviceName + ")"
//                    )
//
//                    _nsdManager.resolveService(service, _resolveListener)
//                }
//            }
//
//            override fun onServiceLost(service: NsdServiceInfo) {
//                Log.e(TAG, "service lost$service")
//                if (_chosenServiceInfo == service) {
//                    _chosenServiceInfo = null
//                }
//            }
//
//            override fun onDiscoveryStopped(serviceType: String) {
//                Log.i(TAG, "Discovery stopped: $serviceType")
//            }
//
//            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
//                Log.e(
//                    TAG,
//                    "Discovery failed: Error code:$errorCode"
//                )
//            }
//
//            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
//                Log.e(
//                    TAG,
//                    "Discovery failed: Error code:$errorCode"
//                )
//            }
//        }
//    }

    private fun initializeRegistrationListener() {
        _registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                _serviceName = nsdServiceInfo.serviceName
                Log.d(TAG, "Service registered: $nsdServiceInfo")
//                NotificationToast.showToast(_context, "Service registered")
            }

            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Log.d(TAG, "Service registration failed: $arg1")
//                NotificationToast.showToast(_context, "Service registration failed")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: " + arg0.serviceName)
//                NotificationToast.showToast(_context, "Service unregistered")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.d(
                    TAG,
                    "Service unregistration failed: $errorCode"
                )
//                NotificationToast.showToast(_context, "Service un-registration failed")
            }
        }
    }
}