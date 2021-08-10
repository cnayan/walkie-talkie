package com.cnayan.walkie_talkie

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean


class TcpFileServer : Runnable {
    private val TAG = "TcpFileServer"
    private val stopServer: AtomicBoolean = AtomicBoolean(false)
    private val _ip: String = Utils.getIPAddress(true) ?: "127.0.0.1"
    private val _mac = ""
    var listener: ((ByteArray) -> Unit)? = null

    override fun run() {
        Log.d(TAG, "${Thread.currentThread()} Runnable Thread Started.")

        var server: ServerSocket? = null
        try {
            val inetAddress = InetSocketAddress(InetAddress.getByName(_ip), 38513)

            server = ServerSocket()
            server.bind(inetAddress)
            server.reuseAddress = true

            while (!stopServer.get()) {
                try {
                    val data: ByteArray? = receive(server)
                    if (data != null) {
                        Log.d(TAG, "receive: data received = " + data.size.toString())
                        listener?.invoke(data)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "run: Exception: $e")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "run: Exception: $e")
            e.printStackTrace()

        } finally {
            server?.close()
        }
    }

    private fun receive(server: ServerSocket): ByteArray? {
        //Wait for data to arrive
        val socket = server.accept()
        val stream = socket.getInputStream()
        return getData(stream)
    }

    private fun getData(inputStream: InputStream): ByteArray? {
        try {
            val input: ByteArray? = getInputStreamByteArray(inputStream)
            var oin: ByteArrayInputStream? = null
            try {
                oin = ByteArrayInputStream(input)
                return oin.readBytes()
            } catch (cnfe: ClassNotFoundException) {
                Log.e(TAG, cnfe.toString())
                cnfe.printStackTrace()
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            } finally {
                oin?.close()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    private fun getInputStreamByteArray(input: InputStream): ByteArray? {
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int
        try {
            while (input.read(buffer).also { len = it } > -1) {
                baos.write(buffer, 0, len)
            }

            baos.flush()
        } catch (ioe: IOException) {
            Log.e(TAG, "getInputStreamByteArray: " + ioe.toString())
            ioe.printStackTrace()
        }

        return baos.toByteArray()
    }

//    fun sendMessage(socket: DatagramSocket, messageStr: String) {
//        // Hack Prevent crash (sending should be done using an async task)
//        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
//        StrictMode.setThreadPolicy(policy)
//        try {
//            val sendData = messageStr.toByteArray()
//            val sendPacket = DatagramPacket(
//                sendData,
//                sendData.size
//            ) //, InetAddress.getByName(Settings.RemoteHost), Settings.RemotePort)
//            socket.send(sendPacket)
//            println("sendBroadcast: packet sent")
//        } catch (e: IOException) {
//            Log.e("UdpPingServer", "IOException: " + e.message)
//        }
//    }
}