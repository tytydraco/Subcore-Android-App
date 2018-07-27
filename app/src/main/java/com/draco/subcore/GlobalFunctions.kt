package com.draco.subcore

import android.content.Context
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater

lateinit var root: Root
lateinit var runAsync: RunAsync

const val filter_get_current_options = "RUN_ASYNC"

fun stopLoading() {
    MainActivity.fetching_dialog_create.dismiss()
}

fun runnableAsync(context: Context, runnable: Runnable, ui: Boolean = true) {
    RunAsync.runnable = runnable
    RunAsync.ui = ui

    // call get_current_objects async task
    val setupOptions = Intent(filter_get_current_options)
    setupOptions.flags = Intent.FLAG_RECEIVER_FOREGROUND
    context.sendBroadcast(setupOptions)
}

fun Boolean.toInt() = if (this) 1 else 0