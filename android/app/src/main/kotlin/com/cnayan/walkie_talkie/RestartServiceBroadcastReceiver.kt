package com.cnayan.walkie_talkie

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat.startForegroundService

class RestartServiceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent!!.action) {
            Toast.makeText(context, "Starting Walkie Talkie!", Toast.LENGTH_LONG).show()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val i = Intent(context, WalkieTalkieForegroundService::class.java)
                startForegroundService(context, i)
            }
        }
    }
}