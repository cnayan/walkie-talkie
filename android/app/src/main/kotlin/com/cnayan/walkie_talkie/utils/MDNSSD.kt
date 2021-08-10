package com.cnayan.walkie_talkie.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.github.druk.dnssd.*
import org.jetbrains.annotations.NotNull

class MDNSSD(@NotNull context: Context) : BrowseListener {
    private val dnssd: DNSSDBindable = DNSSDBindable(context)

    companion object {
        const val SERVICE_TYPE = "_cnayan_walkie_talkie._udp"
        val TAG = "MDNSSD"
        var devices: HashSet<String> = HashSet()
    }

    fun registerNSDService(port: Int = 38512) {
        try {
            val deviceName: String = Build.MODEL
            val ip: String = Network.getIPAddress(true) ?: "127.0.0.1"
            val deviceID: String = Build.FINGERPRINT

            val separator = 255.toChar()
            val name = "$deviceName$separator$ip$separator$deviceID"

            registerService(name, port)
        } catch (ignored: java.lang.Exception) {
            Log.e(MDNSSD.TAG, ignored.toString())
        }
    }

    fun registerDeviceDiscoveryListener() {
        try {
            dnssd.browse(MDNSSD.SERVICE_TYPE, this)
        } catch (e: DNSSDException) {
            Log.e(MDNSSD.TAG, "error", e)
        }
    }

    private fun registerService(
        serviceName: String,
        port: Int,
    ) {
        try {
            dnssd.register(serviceName, MDNSSD.SERVICE_TYPE, port,
                object : RegisterListener {
                    override fun serviceRegistered(
                        registration: DNSSDRegistration, flags: Int,
                        serviceName: String, regType: String, domain: String,
                    ) {
                        Log.i("TAG", "Register successfully ")
                    }

                    override fun operationFailed(service: DNSSDService, errorCode: Int) {
                        Log.e("TAG", "error $errorCode")
                    }
                })
        } catch (e: DNSSDException) {
            Log.e("TAG", "NSD Registration - error", e)
        }
    }

    override fun operationFailed(service: DNSSDService, errorCode: Int) {
        Log.e(TAG, "error: $errorCode")
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
            devices.add(serviceName)
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
            devices.remove(serviceName)
        }
    }
}