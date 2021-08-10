package com.cnayan.walkie_talkie.utils

import java.net.InetAddress
import java.net.NetworkInterface

class Network {
    companion object {
        val TAG = "NetworkUtils"

        /**
         * Get IP address from first non-localhost interface
         * @param useIPv4   true=return ipv4, false=return ipv6
         * @return  address or empty string
         */
        @JvmStatic
        fun getIPAddress(useIPv4: Boolean): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces().toList()
                for (networkInterface: NetworkInterface in interfaces) {
                    val addresses = networkInterface.inetAddresses.toList()
                    for (address: InetAddress in addresses) {
                        if (!address.isLoopbackAddress) {
                            val strAddrress = address.hostAddress
                            val isIPv4 = strAddrress.indexOf(':') < 0
                            if (useIPv4) {
                                if (isIPv4) {
                                    return strAddrress
                                }
                            } else {
                                if (!isIPv4) {
                                    val delim = strAddrress.indexOf('%') // drop ip6 zone suffix
                                    return if (delim < 0) {
                                        strAddrress.uppercase()
                                    } else {
                                        strAddrress.substring(
                                            0,
                                            delim
                                        ).uppercase()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (ignored: java.lang.Exception) {
            } // for now eat exceptions

            return ""
        }
    }
}