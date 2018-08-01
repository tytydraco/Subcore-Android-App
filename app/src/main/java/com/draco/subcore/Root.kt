package com.draco.subcore

import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class Root(var context: Context) {
    fun checkRoot(): Boolean {
        val rootGranted = run("id", true)
        return if (rootGranted.contains("root")) {
            true
        } else {
            rootErrorDialog()
            false
        }
    }

    fun run(cmd: String, asRoot: Boolean = false): String {
        // debug
        //Log.d("RUN_COMMAND", cmd)

        try {
            var command = cmd
            if (asRoot) {
                command = "su -c $command"
            }
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(
                    InputStreamReader(process.inputStream))

            val output = StringBuffer()
            do {
                val line = reader.readLine() ?: break
                if (output.toString() != "") {
                    output.append("\n" + line)
                } else {
                    output.append(line)
                }
            } while (true)
            reader.close()
            process.waitFor()
            return output.toString()
        } catch (e: Exception) {
            //println(e.message)
        }
        return ""
    }

    private fun rootErrorDialog() {
        AlertDialog.Builder(context)
            .setTitle("Root Denied")
            .setMessage("Root is required to use this application. Please root your device.")
            .setPositiveButton("Ok", { _, _ ->
                System.exit(1)
            })
            .setCancelable(false)
            .show()
    }
}