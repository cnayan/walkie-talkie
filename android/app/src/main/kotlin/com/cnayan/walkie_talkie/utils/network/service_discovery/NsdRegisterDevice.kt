package com.cnayan.walkie_talkie.utils.network.service_discovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log

class NsdRegisterDevice(private val nsdManager: NsdManager) {
    private var registrationListener: NsdManager.RegistrationListener? = null

    companion object {
        val TAG = NsdRegisterDevice::class.simpleName
    }

    fun register(port: Int) {
        if (registrationListener != null) return

        val name = ServiceNameProvider.generateServiceName()

        tearDown() // Cancel any previous registration request
        initializeRegistrationListener()

        val serviceInfo = NsdServiceInfo()
        serviceInfo.port = port
        serviceInfo.serviceName = name
        serviceInfo.serviceType = ServiceNameProvider.SERVICE_TYPE

        Log.v(TAG,
            Build.MANUFACTURER + " - registering service: $name at $port")

        initializeRegistrationListener()

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun initializeRegistrationListener() {
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.d(TAG, "Service registration failed: $errorCode")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.d(
                    TAG,
                    "Service unregistration failed: $errorCode"
                )
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service registered: [${serviceInfo?.serviceName ?: '?'}]")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                Log.d(TAG, "Service unregistered: [${serviceInfo?.serviceName ?: '?'}]")
            }
        }
    }

    private fun tearDown() {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener)
            registrationListener = null
        }
    }
}