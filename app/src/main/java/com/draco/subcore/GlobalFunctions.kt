package com.draco.subcore

import android.os.AsyncTask

const val filter_refresh_ui = "REFRESH_UI"

fun asyncExec(func: () -> Unit) {
    object : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) = func()
    }.execute()
}
