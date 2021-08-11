package com.cnayan.walkie_talkie.utils.network.service_discovery

import android.content.Context
import android.net.nsd.NsdManager

class NSD(context: Context) {
    private var nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val registrationHelper: NsdRegisterDevice = NsdRegisterDevice(nsdManager)
    private val discoveryHelper: NetworkServiceDiscoverer = NetworkServiceDiscoverer(nsdManager)

    companion object {
        val devices: HashSet<String> get() = NetworkServiceDiscoverer.devices
    }

    fun registerDevice(port: Int = 38512) {
        registrationHelper.register(port)
    }

    fun startDiscovering() {
        discoveryHelper.discoverDevices()
    }

    fun stopDiscovering() {
        discoveryHelper.stopDiscovering()
    }
}