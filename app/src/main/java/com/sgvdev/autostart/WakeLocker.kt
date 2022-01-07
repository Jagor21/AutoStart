package com.sgvdev.autostart

import android.content.Context
import android.os.PowerManager

class WakeLocker {

    private var wakeLock: PowerManager.WakeLock? = null

    fun aquire(context: Context) {
        if (wakeLock != null) {
            wakeLock?.release()
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, MainActivity.APP_TAG
        )
        wakeLock?.acquire()
    }

    fun release() {
        if (wakeLock != null) {
            wakeLock?.release()
            wakeLock = null
        }
    }
}