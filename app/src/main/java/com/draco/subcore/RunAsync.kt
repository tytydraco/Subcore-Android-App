package com.draco.subcore

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.concurrent.thread

class RunAsync : BroadcastReceiver() {

    companion object {
        lateinit var runnable: Runnable
        var finished: Boolean = true
        var ui: Boolean = true
        lateinit var handler: android.os.Handler
    }

    init {
        handler = android.os.Handler()
    }

    override fun onReceive(context: Context, intent: Intent) {
        thread {
            finished = false
            if (ui) {
                handler.post(runnable)
            } else {
                runnable.run()
            }
            finished = true
        }
    }
}