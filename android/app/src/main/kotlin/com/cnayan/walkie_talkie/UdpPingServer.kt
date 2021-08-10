package com.cnayan.walkie_talkie

import android.os.Build
import android.os.StrictMode
import android.util.Log
import java.io.IOException
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

import com.cnayan.walkie_talkie.Utils;

class UdpPingServer : Runnable {
    private val TAG = "UdpPingServer"
    private var _socket: MulticastSocket? = null
    private val _ip: String = Utils.getIPAddress(true) ?: "127.0.0.1"
    private val _mac = "" //Utils.getMACAddress("wlan0")

    private val stopServer = AtomicBoolean(false)

    override fun run() {
        Log.d(TAG, "${Thread.currentThread()} Runnable Thread Started.")

        //Keep a socket open to listen to all the UDP trafic that is destined for this port
        _socket = MulticastSocket(38512)
        if (_socket != null) {
            _socket!!.broadcast = true

            while (!stopServer.get()) {
                receive()
            }

            _socket!!.close()
        }
    }

    private fun receive() {
        try {
            val buffer = ByteArray(2048)
            val packet = DatagramPacket(buffer, buffer.size)

            //Wait for data to arrive
            _socket!!.receive(packet)
            Log.d(TAG, "receive: packet received = " + packet.data)

            sendMessage(
                _socket!!,
                Build.MODEL + "!" + _ip + "!" + _mac,
                packet.socketAddress
            )

        } catch (e: Exception) {
            Log.e(TAG, "receive: catch exception." + e.toString())
            e.printStackTrace()
        }
    }

    private fun sendMessage(
        socket: DatagramSocket, localDeviceInfo: String?, targetSocketAddress: SocketAddress
    ) {
        val text = localDeviceInfo ?: "localhost"
        // Hack Prevent crash (sending should be done using an async task)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            val sendData = text.toByteArray()
            val sendPacket = DatagramPacket(
                sendData,
                sendData.size,
                targetSocketAddress
            ) //, InetAddress.getByName(Settings.RemoteHost), Settings.RemotePort)

            socket.send(sendPacket)
            Log.d(TAG, "sendBroadcast: packet sent")
        } catch (e: IOException) {
            Log.e(TAG, "IOException: " + e.message)
        }
    }
}