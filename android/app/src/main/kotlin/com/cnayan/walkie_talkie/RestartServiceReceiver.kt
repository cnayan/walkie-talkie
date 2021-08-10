package com.cnayan.walkie_talkie

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startForegroundService


class RestartServiceReceiver : BroadcastReceiver() {
    private val TAG = "RestartServiceReceiver"

    companion object {
        @JvmStatic
        var CHANNEL_ID: String =  "StickyServiceChannel"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Nayan: onReceive")

        if (Intent.ACTION_BOOT_COMPLETED == intent!!.action) {
            Toast.makeText(context, "Starting Walkie Talkie!", Toast.LENGTH_LONG).show()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val i = Intent(context, StickyService::class.java)
                startForegroundService(context, i)
            }
        }
    }
}