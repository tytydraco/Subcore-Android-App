package com.draco.subcore

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {

    private lateinit var toggleButton: Button
    private lateinit var telegramButton: ImageButton
    private lateinit var gmailButton: ImageButton
    private lateinit var paypalButton: ImageButton
    private lateinit var lowMemSwitch: CheckBox

    private lateinit var arch: String
    private lateinit var bin: String
    private lateinit var pathBin: String

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    companion object {
        lateinit var fetching_dialog: AlertDialog.Builder
        lateinit var fetching_dialog_create: AlertDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        root = Root(this)
        runAsync = RunAsync()

        // setup get_current_objects async task
        val filter = IntentFilter(filter_get_current_options)
        this.registerReceiver(runAsync, filter)

        toggleButton = findViewById(R.id.toggle)
        telegramButton = findViewById(R.id.telegram)
        gmailButton = findViewById(R.id.gmail)
        paypalButton = findViewById(R.id.paypal)
        lowMemSwitch = findViewById(R.id.low_mem)

        prefs = getSharedPreferences("subcore", Context.MODE_PRIVATE)
        editor = prefs.edit()

        root.checkRoot()

        fetching_dialog = AlertDialog.Builder(this)
                .setTitle("Loading")
                .setMessage("Subcore is processing information. Please be patient.")
                .setCancelable(false)

        fetching_dialog_create = fetching_dialog.create()
        fetching_dialog_create.show()

        runnableAsync(this, Runnable {
            arch = getArchitecture()
            verifyCompat()

            bin = getBinName()
            pathBin = getBinPath()

            runOnUiThread {
                // set the UI elements
                if (binRunning()) {
                    toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_green)
                    toggleButton.text = resources.getText(R.string.on)
                } else {
                    toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_red)
                    toggleButton.text = resources.getText(R.string.off)
                }
            }

            writeBin()
            stopLoading()
        }, false)

        toggleButton.setOnClickListener({
            fetching_dialog_create.show()
            runnableAsync(this, Runnable {
                val running = binRunning()
                if (running) {
                    killBin()
                    stopLoading()
                    toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_red)
                    toggleButton.text = resources.getText(R.string.off)
                } else {
                    runBin()
                    stopLoading()
                    toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_green)
                    toggleButton.text = resources.getText(R.string.on)
                }
            }, true)
        })

        telegramButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/tytydraco"))
            startActivity(browserIntent)
        }

        gmailButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:tylernij@gmail.com"))
            startActivity(browserIntent)
        }

        paypalButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/TylerNijmeh"))
            startActivity(browserIntent)
        }

        lowMemSwitch.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("low_mem", isChecked)
            editor.apply()

            AlertDialog.Builder(this)
                    .setTitle("Toggle Required")
                    .setMessage("To apply this change, you must turn Subcore OFF and back ON again.")
                    .setPositiveButton("Ok", null)
                    .show()
        }

        if (prefs.getBoolean("first_run", true)) {
            if (Build.MANUFACTURER.toLowerCase().contains("samsung"))
                editor.putBoolean("low_mem", true)
            editor.putBoolean("first_run", false)
            editor.apply()
        }

        lowMemSwitch.isChecked = prefs.getBoolean("low_mem", false)
    }

    public override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(runAsync)
    }

    private fun runBin() {
        if (!binRunning())
            runnableAsync(this, Runnable {
                var command = "setsid $pathBin &"
                if (prefs.getBoolean("low_mem", false))
                    command += " -m"
                root.run(command, true)
            }, false)
    }

    private fun killBin() {
        runnableAsync(this, Runnable {
            root.run("killall $bin", true)
        }, false)
    }

    private fun binRunning(): Boolean {
        val status = root.run("[ `pgrep $bin` ] && echo 1 || echo 0", true)
        return status.contains("1")
    }

    private fun writeBin() {
        val ins =  when (arch) {
            "arm" -> resources.openRawResource(R.raw.subcore_arm)
            "arm64" -> resources.openRawResource(R.raw.subcore_arm64)
            "x86" -> resources.openRawResource(R.raw.subcore_x86)
            "x86_64" -> resources.openRawResource(R.raw.subcore_x86_64)
            else -> resources.openRawResource(R.raw.subcore_arm)
        }

        val binName = getBinName()
        val buffer = ByteArray(ins.available())
        ins.read(buffer)
        ins.close()
        try {
            val fos = openFileOutput(binName, Context.MODE_PRIVATE)
            fos.write(buffer)
            fos.close()

            val file = getFileStreamPath(binName)
            file.setExecutable(true)
        } catch (e: Exception) {}
    }

    private fun getArchitecture(): String {
        return when (root.run("getprop ro.product.cpu.abi", true)) {
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

    private fun verifyCompat() {
        if (arch == "mips" || arch == "mips64" || arch == "other") {
            AlertDialog.Builder(this)
                .setTitle("Unsupported Architecture")
                .setMessage("Your device is unsupported (MIPS).")
                .setPositiveButton("Ok", { _, _ ->
                    finish()
                })
                .setCancelable(false)
                .show()
        }
    }

    private fun getBinPath(): String {
        val appFileDirectory = filesDir.path
        val executableFilePath = "$appFileDirectory/"
        return executableFilePath + bin
    }

    private fun getBinName(): String {
        return when (arch) {
            "arm" -> "subcore_arm"
            "arm64" -> "subcore_arm64"
            "x86" -> "subcore_x86"
            "x86_64" -> "subcore_x86_64"
            else -> "subcore_arm"
        }
    }

}
