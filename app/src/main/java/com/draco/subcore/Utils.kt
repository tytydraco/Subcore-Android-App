package com.draco.subcore

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.app.AlertDialog

class Utils {
    companion object {
        fun runBin(context: Context) {
            runnableAsync(context, Runnable {
                var extraArgs = ""
                if (MainActivity.prefs.getBoolean("low_mem", false))
                    extraArgs += "-m "
                if (MainActivity.prefs.getBoolean("disable_power_aware", false))
                    extraArgs += "-p "
                val command = "[ `pgrep ${MainActivity.bin}` ] || ${MainActivity.pathBin} $extraArgs &"
                root.run(command, true)
            }, false)
        }

        fun killBin(context: Context) {
            runnableAsync(context, Runnable {
                root.run("killall ${MainActivity.bin}", true)
            }, false)
        }

        fun binRunning(): Boolean {
            val status = root.run("[ `pgrep ${MainActivity.bin}` ] && echo 1", true)
            return status.contains("1")
        }

        fun writeBin(context: Context) {
            val ins =  when (MainActivity.arch) {
                "arm" -> context.resources.openRawResource(R.raw.subcore_arm)
                "arm64" -> context.resources.openRawResource(R.raw.subcore_arm64)
                "x86" -> context.resources.openRawResource(R.raw.subcore_x86)
                "x86_64" -> context.resources.openRawResource(R.raw.subcore_x86_64)
                else -> context.resources.openRawResource(R.raw.subcore_arm)
            }

            val binName = getBinName()
            val buffer = ByteArray(ins.available())
            ins.read(buffer)
            ins.close()
            try {
                val fos = context.openFileOutput(binName, Context.MODE_PRIVATE)
                fos.write(buffer)
                fos.close()

                val file = context.getFileStreamPath(binName)
                file.setExecutable(true)
            } catch (e: Exception) {}
        }

        fun getArchitecture(): String {
            return when (getProp("ro.product.cpu.abi")) {
                "armeabi" -> "arm"
                "armeabi-v7a" -> "arm"
                "x86" -> "x86"
                "arm64-v8a" -> "arm64"
                "x86_64" -> "x64"
                "mips" -> "mips"
                "mips64" -> "mips64"
                else -> "other"
            }
        }

        fun verifyCompat(context: Context) {
            if (MainActivity.arch == "mips" || MainActivity.arch == "mips64" || MainActivity.arch == "other") {
                try {
                    AlertDialog.Builder(context)
                            .setTitle("Unsupported Architecture")
                            .setMessage("Your device is unsupported (MIPS).")
                            .setPositiveButton("Ok", { _, _ ->
                                System.exit(1)
                            })
                            .setCancelable(false)
                            .show()
                } catch (e: Exception) {}
            }
        }

        fun getBinPath(context: Context): String {
            val appFileDirectory = context.filesDir.path
            val executableFilePath = "$appFileDirectory/"
            return executableFilePath + MainActivity.bin
        }

        fun getBinName(): String {
            return when (MainActivity.arch) {
                "arm" -> "subcore_arm"
                "arm64" -> "subcore_arm64"
                "x86" -> "subcore_x86"
                "x86_64" -> "subcore_x86_64"
                else -> "subcore_arm"
            }
        }

        @SuppressLint("PrivateApi")
        private fun getProp(prop: String): String? {
            return try {
                val clazz = Class.forName("android.os.SystemProperties")
                val method = clazz.getDeclaredMethod("get", java.lang.String::class.java)
                method.invoke(null, prop) as String
            } catch (e: Exception) {
                ""
            }
        }
    }
}