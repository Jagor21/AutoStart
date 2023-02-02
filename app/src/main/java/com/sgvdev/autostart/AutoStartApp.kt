package com.sgvdev.autostart

import android.app.Application
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutoStartApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LogWriter.log(this, "\n\n${LogWriter.APP_NAME}\n\nAutostartTvApp: app started")
        Thread.setDefaultUncaughtExceptionHandler(object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(p0: Thread, p1: Throwable) {
                ProcessPhoenix.triggerRebirth(this@AutoStartApp )
            }
        })
    }
}