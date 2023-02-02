package com.sgvdev.autostart

import android.util.Log
import kotlinx.coroutines.*

class Timer(private val delay: Long = 0, private val repeat: Long = 20000) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    var foregroundBlock: () -> Unit = {}

    private fun startTimer(action: () -> Unit) = scope.launch(Dispatchers.IO) {
        delay(delay)
        if (repeat > 0) {
            while (true) {
                action()
                delay(repeat)
            }
        } else {
            action()
        }
    }

    private val timer: Job = startTimer {
        Log.d("TIMER_INFO", "background tick")
        scope.launch(Dispatchers.Main) {
            Log.d("TIMER_INFO", "foreground tick")
            foregroundBlock()
        }
    }

    fun start() {
        timer.start()
    }

    fun stop() {
        timer.cancel()
    }
}