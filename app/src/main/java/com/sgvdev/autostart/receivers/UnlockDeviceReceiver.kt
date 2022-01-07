package com.sgvdev.autostart.receivers

import android.content.*
import android.util.Log
import com.sgvdev.autostart.LogWriter
import com.sgvdev.autostart.WakeLocker

private const val TIME_UNLOCK_8_00 = 8

class UnlockDeviceReceiver/*(private val calendar: Calendar)*/ : BroadcastReceiver() {

    private lateinit var wakeLocker: WakeLocker

    companion object {
        const val REQUEST_UNLOCK_CODE = 67890
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        wakeLocker = WakeLocker()
        context?.let {
            LogWriter.log(it, "UnlockDeviceReceiver: time to wake up. Good morning!\nScreen unlock")
            Log.d("MY_TAG", "UnlockDeviceReceiver: time to wake up. Good morning!\nScreen unlock")
            wakeLocker.aquire(it)
        }
    }

//    private fun checkTime() {
////        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
////        if (currentHour >= TIME_LOCK_23_00) {
////            lockDevice(true)
////        }
////        if (currentHour >= TIME_UNLOCK_8_00) {
////            lockDevice(false)
////        }
//
//        val currentMinutes = calendar.get(Calendar.MINUTE)
//        if ((currentMinutes % 2) == 0) {
//            lockDevice(true)
//        } else {
//            lockDevice(false)
//        }
//    }
}