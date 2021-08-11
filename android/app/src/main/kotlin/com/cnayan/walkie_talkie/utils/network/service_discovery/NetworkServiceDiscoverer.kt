package com.cnayan.walkie_talkie.utils.network.service_discovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NetworkServiceDiscoverer(private val nsdManager: NsdManager) {
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    companion object {
        private val TAG = NetworkServiceDiscoverer::class.simpleName

        private val localDeviceName = ServiceNameProvider.generateServiceName()

        var devices: HashSet<String> = HashSet()
    }

    fun discoverDevices() {
        if (discoveryListener != null) return

        stopDiscovering() // Cancel any existing discovery request
        initializeDiscoveryListener()
        nsdManager.discoverServices(ServiceNameProvider.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovering() {
        if (discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener)
            discoveryListener = null
        }
    }

    private fun initializeDiscoveryListener() {
        if (discoveryListener != null) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                val serviceType = service.serviceType
                Log.d(TAG, "Service discovery success: " + service.serviceName)
                // For some reason the service type received has an extra dot with it, hence handling that case
                val isOurService =
                    serviceType == ServiceNameProvider.SERVICE_TYPE || serviceType == ServiceNameProvider.SERVICE_TYPE_PLUS_DOT

                if (!isOurService) {
                    Log.d(TAG, "Unknown Service Type: " + service.serviceType)

                } else if (service.serviceName == localDeviceName) {
                    Log.d(TAG, "Same machine: $localDeviceName")

                } else { //if (service.serviceName.contains(localDeviceName)) {
                    Log.d(
                        TAG,
                        "Different machine discovered: [${service.serviceName}]"
                    )

                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Resolve failed with error: $errorCode for $serviceInfo")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Log.v(TAG, "Resolve Succeeded. $serviceInfo")
                            if (serviceInfo.serviceName == localDeviceName) {
                                Log.d(TAG, "Ignoring local device $localDeviceName.")
                                return
                            }

                            devices.add(serviceInfo.serviceName)
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e(TAG, "Device lost $service")
                devices.remove(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(
                    TAG,
                    "Discovery failed: Error code:$errorCode"
                )
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(
                    TAG,
                    "Discovery failed: Error code:$errorCode"
                )
            }
        }
    }
}