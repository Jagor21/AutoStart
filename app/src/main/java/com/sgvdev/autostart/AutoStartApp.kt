package com.sgvdev.autostart

import android.app.Application

class AutoStartApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LogWriter.log(this, "\n\n${LogWriter.APP_NAME}")
    }


}