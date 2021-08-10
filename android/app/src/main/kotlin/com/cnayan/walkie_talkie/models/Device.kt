package com.cnayan.walkie_talkie.models

class Device(var name: String, var ip: String, var id: String) {
    companion object {
        fun from(deviceStr: String): Device {
            var arr = deviceStr.split(255.toChar())
            return Device(arr[0], arr[1], arr[2])
        }
    }
}
