package com.sgvdev.autostart.receivers

import android.content.*
import android.os.Build
import android.util.Log
import com.sgvdev.autostart.LogWriter
import com.sgvdev.autostart.MyService

class BootUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
//        val i = Intent(context, MainActivity::class.java)
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        Log.d("MY_TAG", "BootUpReceiver onReceive")
////        startActivity(context!!, i, null)
//        context?.startService(Intent(context, MyService::class.java))

        val i = Intent(context, MyService::class.java)
        context?.let {
            LogWriter.log(
                it,
                "BootUpReceiver: Starting MyService\nIntent action: ${intent?.action}"
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(i)
        } else {
            context?.startService(i)
        }
        Log.i("Autostart", "started")
    }
}