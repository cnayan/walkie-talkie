package com.cnayan.walkie_talkie

import android.content.Context
import android.os.Build
import android.util.Log
import com.github.druk.dnssd.BrowseListener
import java.net.InetAddress
import java.net.NetworkInterface

class Utils {
    private constructor()

//    private var _helper: NsdHelper? = null

//    object : BroadcastReceiver() {
//        fun onReceive(context: Context?, intent: Intent) {
//            when (intent.getAction()) {
//                NsdHelper.BROADCAST_TAG -> {
//                    val serviceInfo: NsdServiceInfo = mNsdHelper.getChosenServiceInfo()
//                    val ipAddress: String = serviceInfo.getHost().getHostAddress()
//                    val port: Int = serviceInfo.getPort()
//                    DataSender.sendCurrentDeviceData(context, ipAddress, port, true)
//                }
//
//                DataHandler.DEVICE_LIST_CHANGED -> {
//                    val devices: ArrayList<DeviceDTO> = DBAdapter.getInstance(this@LocalDashNSD)
//                        .getDeviceList()
//                    val peerCount = if (devices == null) 0 else devices.size()
//                    if (peerCount > 0) {
//                        progressBarLocalDash.setVisibility(View.GONE)
//                        deviceListFragment = PeerListFragment()
//                        val args = Bundle()
//                        args.putSerializable(PeerListFragment.ARG_DEVICE_LIST, devices)
//                        deviceListFragment.setArguments(args)
//                        val ft: FragmentTransaction = getSupportFragmentManager().beginTransaction()
//                        ft.replace(R.id.deviceListHolder, deviceListFragment)
//                        ft.setTransition(FragmentTransaction.TRANSIT_NONE)
//                        ft.commit()
//                    }
//                }
//
//                DataHandler.CHAT_REQUEST_RECEIVED -> {
//                    val chatRequesterDevice: DeviceDTO =
//                        intent.getSerializableExtra(DataHandler.KEY_CHAT_REQUEST) as DeviceDTO
//                    //showChatRequestedDialog(chatRequesterDevice);
//                    DialogUtils.getChatRequestDialog(this@LocalDashNSD, chatRequesterDevice).show()
//                }
//                DataHandler.CHAT_RESPONSE_RECEIVED -> {
//                    val isChatRequestAccepted: Boolean =
//                        intent.getBooleanExtra(DataHandler.KEY_IS_CHAT_REQUEST_ACCEPTED, false)
//                    if (!isChatRequestAccepted) {
//                        NotificationToast.showToast(
//                            this@LocalDashNSD, "Chat request " +
//                                    "rejected"
//                        )
//                    } else {
//                        val chatDevice: DeviceDTO =
//                            intent.getSerializableExtra(DataHandler.KEY_CHAT_REQUEST) as DeviceDTO
//                        DialogUtils.openChatActivity(this@LocalDashNSD, chatDevice)
//                        NotificationToast.showToast(
//                            this@LocalDashNSD, chatDevice
//                                .getPlayerName().toString() + "Accepted Chat request"
//                        )
//                    }
//                }
//                else -> {
//                }
//            }
//        }
//    }

    fun registerNSDService(applicationContext: Context, port: Int = 38512) {
//        if (_helper != null) return
//
//        _helper = NsdHelper(applicationContext)

        try {
            val deviceName: String = Build.MODEL
            val ip: String = getIPAddress(true) ?: "127.0.0.1"
            val deviceID: String = Build.FINGERPRINT

            val separator = 255.toChar()
            val name = "$deviceName$separator$ip$separator$deviceID"

            NsdHelper.registerService(applicationContext, name, port)
        } catch (ignored: java.lang.Exception) {
            Log.e(TAG, ignored.toString())
        }
    }

//    fun stopNSDService() {
//        _helper?.tearDown()
//        _helper = null
//    }

    fun registerDeviceDiscoveryListener(
        applicationContext: Context,
        browseListener: BrowseListener,
    ) {
        NsdHelper.findDevice(applicationContext, browseListener)
    }

    companion object {
        private var _instance: Utils? = null
        val Instance: Utils
            get() {
                _instance = _instance ?: Utils()
                return _instance!!
            }

        val TAG = "Utils"

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