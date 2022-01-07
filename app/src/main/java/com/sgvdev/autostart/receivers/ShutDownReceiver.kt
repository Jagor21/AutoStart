package com.sgvdev.autostart.receivers

import android.content.*
import android.util.Log
import com.sgvdev.autostart.LogWriter

class ShutDownReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            LogWriter.log(it, "ShutDownReceiver: ${intent?.action}")
            Log.d("MY_TAG", "ShutDownReceiver: ${intent?.action}")
        }
    }
}