package com.cnayan.walkie_talkie.utils.network.service_discovery

import android.os.Build
import com.cnayan.walkie_talkie.utils.network.Network

class ServiceNameProvider {
    companion object {
        const val SERVICE_TYPE = "_cnayan_walkie_talkie._udp"
        // There is an additional dot at the end of service name most probably by os, this is to rectify that problem
        const val SERVICE_TYPE_PLUS_DOT = "$SERVICE_TYPE."

        fun generateServiceName(): String {
            val deviceName: String = Build.MODEL
            val ip: String = Network.getIPAddress(true) ?: "127.0.0.1"
            val deviceID: String = Build.FINGERPRINT

            val separator = 255.toChar()
            val name = "$deviceName$separator$ip$separator$deviceID"
            return name
        }
    }
}