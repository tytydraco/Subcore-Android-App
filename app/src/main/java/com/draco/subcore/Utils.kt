package com.draco.subcore

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.support.v7.app.AlertDialog
import com.topjohnwu.superuser.Shell

class Utils {
    companion object {
        lateinit var arch: String
        lateinit var bin: String
        lateinit var pathBin: String

        lateinit var prefs: SharedPreferences
        lateinit var editor: SharedPreferences.Editor
        lateinit var securePrefs: SecurePreferences

        fun runBin() {
            asyncExec {
                var extraArgs = ""
                if (prefs.getBoolean("low_mem", false))
                    extraArgs += "-m "
                if (prefs.getBoolean("disable_power_aware", false))
                    extraArgs += "-p "
                if (prefs.getBoolean("keep_in_foreground", false))
                    extraArgs += "-f "
                val command = "[ `pgrep $bin` ] || $pathBin $extraArgs"
                Shell.su(command).exec()
            }
        }

        fun killBin() {
            asyncExec {
                Shell.su("killall $bin").exec()
            }
        }

        fun binRunning(): Boolean {
            return Shell.su("[ `pgrep $bin` ] && echo 1").exec().isSuccess
        }

        fun writeBin(context: Context) {
            val ins =  when (arch) {
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
            for (abi: String in Build.SUPPORTED_ABIS) {
                when (abi) {
                    "armeabi" -> return "arm"
                    "armeabi-v7a" -> return "arm"
                    "x86" -> return "x86"
                    "arm64-v8a" -> return "arm64"
                    "x86_64" -> return "x64"
                    "mips" -> return "mips"
                    "mips64" -> return "mips64"
                }
            }
            return "other"
        }

        fun verifyCompat(context: Context) {
            if (arch == "mips" || arch == "mips64" || arch == "other") {
                try {
                    AlertDialog.Builder(context, R.style.DialogTheme)
                            .setTitle("Unsupported Architecture")
                            .setMessage("Your device is unsupported (MIPS).")
                            .setPositiveButton("Ok") { _, _ ->
                                System.exit(1)
                            }
                            .setCancelable(false)
                            .show()
                } catch (e: Exception) {}
            }
        }

        fun getBinPath(context: Context): String {
            val appFileDirectory = context.filesDir.path
            val executableFilePath = "$appFileDirectory/"
            return executableFilePath + bin
        }

        fun getBinName(): String {
            return when (arch) {
                "arm" -> "subcore_arm"
                "arm64" -> "subcore_arm64"
                "x86" -> "subcore_x86"
                "x86_64" -> "subcore_x86_64"
                else -> "subcore_arm"
            }
        }
    }
}