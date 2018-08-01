package com.draco.subcore

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import com.google.android.vending.licensing.AESObfuscator
import com.google.android.vending.licensing.LicenseChecker
import com.google.android.vending.licensing.LicenseCheckerCallback
import com.google.android.vending.licensing.ServerManagedPolicy

class MainActivity : AppCompatActivity() {

    private lateinit var toggleButton: Button
    private lateinit var telegramButton: ImageButton
    private lateinit var gmailButton: ImageButton
    private lateinit var paypalButton: ImageButton
    private lateinit var lowMemSwitch: CheckBox
    private lateinit var applyOnBoot: CheckBox

    companion object {
        lateinit var arch: String
        lateinit var bin: String
        lateinit var pathBin: String

        lateinit var prefs: SharedPreferences
        lateinit var editor: SharedPreferences.Editor

        lateinit var securePrefs: SecurePreferences

        var running = false

        val RSA_PRIVATE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjNtzFmxMD6g+5pRzMh1P4V/3dIx88FPRalUZ+c2YnH9jI1k5NsM/fxrNpJVojRLkvmq2L9EIASacZ9pp3XS1f9JtCtyzVXIXUpyEJrTm5Ntm9vaw3YlBOKmyU0FmSEQ4KRCU77V3dxGzNdadsMaWz/ooccidNE28yISFqYT++tRD2lD4FzUfHSqZv+P6L89ZmILlQ71sGv5TDVzIAadqlLrvp6E639NTBFdjSNjXXwVEcSDFBmmqq6YDsvLYSMf9SGX8YsCDAo2MSlzaGV92CwiMUhuxZNIbcawPeA1raQq8KpQ0zNTchcw/GbXQSO1b6jx/2MiseJlkuICq9msglwIDAQAB"
        val SALT = byteArrayOf(-81, 40, 92, 27, -18, -14, 98, 8, 91, 95, -5, 21, 26, -24, 54, -88, 62, 16, -42, -86)
    }

    private lateinit var mLicenseCheckerCallback: LicenseCheckerCallback
    private lateinit var mChecker: LicenseChecker

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mChecker = LicenseChecker(
                this,
                ServerManagedPolicy(this, AESObfuscator(SALT,
                        packageName,
                        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))),
                        RSA_PRIVATE_KEY)
        mLicenseCheckerCallback = MyLicenseCheckerCallback()
        securePrefs = SecurePreferences(this, "subcore-secure", RSA_PRIVATE_KEY, true)

        prefs = getSharedPreferences("subcore", Context.MODE_PRIVATE)
        editor = prefs.edit()

        /*if (securePrefs.getString("licensed") != "1")
            doCheck()
        */

        root = Root(this)
        runAsync = RunAsync()

        // setup get_current_objects async task
        val filter = IntentFilter(filter_get_current_options)
        try {
            registerReceiver(runAsync, filter)
        } catch (e: Exception) {}

        toggleButton = findViewById(R.id.toggle)
        telegramButton = findViewById(R.id.telegram)
        gmailButton = findViewById(R.id.gmail)
        paypalButton = findViewById(R.id.paypal)
        lowMemSwitch = findViewById(R.id.low_mem)
        applyOnBoot = findViewById(R.id.apply_on_boot)

        toggleButton.isEnabled = false

        runnableAsync(this, Runnable {
            root.checkRoot()
            runnableAsync(this, Runnable {
                arch = Utils.getArchitecture()
                Utils.verifyCompat(this)

                bin = Utils.getBinName()
                pathBin = Utils.getBinPath(this)

                runOnUiThread {
                    // set the UI elements
                    if (Utils.binRunning()) {
                        running = true
                        lowMemSwitch.isEnabled = false
                        toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_green)
                        toggleButton.text = resources.getText(R.string.on)
                    } else {
                        running = false
                        lowMemSwitch.isEnabled = true
                        toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_red)
                        toggleButton.text = resources.getText(R.string.off)
                    }
                }

                Utils.writeBin(this)
                toggleButton.isEnabled = true
            }, true)
        }, false)

        toggleButton.setOnClickListener({
            val popAnim = AnimationUtils.loadAnimation(this, R.anim.pop)
            toggleButton.startAnimation(popAnim)

            toggleButton.background = ContextCompat.getDrawable(this, R.drawable.transition_enable_disable)
            val transition = toggleButton.background as TransitionDrawable

            if (running) {
                transition.startTransition(300)
            } else {
                transition.reverseTransition(0)
                transition.reverseTransition(300)
            }

            runnableAsync(this, Runnable {
                if (running) {
                    Utils.killBin(this)
                    toggleButton.text = resources.getText(R.string.off)
                    lowMemSwitch.isEnabled = true
                } else {
                    Utils.runBin(this)
                    toggleButton.text = resources.getText(R.string.on)
                    lowMemSwitch.isEnabled = false
                }
                running = !running
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

        lowMemSwitch.setOnClickListener {
            val isChecked = lowMemSwitch.isChecked
            editor.putBoolean("low_mem", isChecked)
            editor.apply()
        }

        applyOnBoot.setOnClickListener {
            val isChecked = applyOnBoot.isChecked
            editor.putBoolean("apply_on_boot", isChecked)
            editor.apply()
        }

        if (prefs.getBoolean("first_run", true)) {
            if (Build.MANUFACTURER.toLowerCase().contains("samsung"))
                editor.putBoolean("low_mem", true)
            editor.putBoolean("first_run", false)
            editor.apply()
        }

        lowMemSwitch.isChecked = prefs.getBoolean("low_mem", false)
        applyOnBoot.isChecked = prefs.getBoolean("apply_on_boot", false)
    }

    public override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(runAsync)
        } catch (e: Exception) {}
    }


    private fun doCheck() {
        mChecker.checkAccess(mLicenseCheckerCallback)
    }

    private fun unlicensedDialog() {
        AlertDialog.Builder(MainActivity@this)
                .setTitle("Unlicensed")
                .setMessage("This app is potentially stolen, or Google is having an internal server issue.")
                .setCancelable(false)
                .setOnDismissListener({
                    System.exit(1)
                }).setPositiveButton("Ok", null)
                .show()
    }

    private inner class MyLicenseCheckerCallback : LicenseCheckerCallback {

        override fun allow(reason: Int) {
            if (isFinishing) {
                return
            }
            Log.i("License", "Accepted!")

            if (securePrefs.getString("licensed") != "1")
                securePrefs.put("licensed", "1")
        }

        override fun dontAllow(reason: Int) {
            if (isFinishing) {
                return
            }
            Log.i("License", "Denied!")
            Log.i("License", "Reason for denial: $reason")

            if (securePrefs.getString("licensed") != "1")
                securePrefs.put("licensed", "0")
            unlicensedDialog()
        }

        override fun applicationError(reason: Int) {
            Log.i("License", "Error: $reason")
            if (isFinishing) {
                return
            }

            if (securePrefs.getString("licensed") != "1")
                securePrefs.put("licensed", "0")
            unlicensedDialog()
        }
    }

}
