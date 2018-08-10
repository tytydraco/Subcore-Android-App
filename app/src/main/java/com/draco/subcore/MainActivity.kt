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
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.Button
import com.google.android.vending.licensing.AESObfuscator
import com.google.android.vending.licensing.LicenseChecker
import com.google.android.vending.licensing.LicenseCheckerCallback
import com.google.android.vending.licensing.ServerManagedPolicy
import com.topjohnwu.superuser.Shell

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var toggleButton: Button

    companion object {
        lateinit var arch: String
        lateinit var bin: String
        lateinit var pathBin: String

        lateinit var prefs: SharedPreferences
        lateinit var editor: SharedPreferences.Editor

        lateinit var securePrefs: SecurePreferences

        var running = false

        lateinit var optFrag: OptionFragment

        val RSA_PRIVATE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjNtzFmxMD6g+5pRzMh1P4V/3dIx88FPRalUZ+c2YnH9jI1k5NsM/fxrNpJVojRLkvmq2L9EIASacZ9pp3XS1f9JtCtyzVXIXUpyEJrTm5Ntm9vaw3YlBOKmyU0FmSEQ4KRCU77V3dxGzNdadsMaWz/ooccidNE28yISFqYT++tRD2lD4FzUfHSqZv+P6L89ZmILlQ71sGv5TDVzIAadqlLrvp6E639NTBFdjSNjXXwVEcSDFBmmqq6YDsvLYSMf9SGX8YsCDAo2MSlzaGV92CwiMUhuxZNIbcawPeA1raQq8KpQ0zNTchcw/GbXQSO1b6jx/2MiseJlkuICq9msglwIDAQAB"
        val SALT = byteArrayOf(-81, 40, 92, 27, -18, -14, 98, 8, 91, 95, -5, 21, 26, -24, 54, -88, 62, 16, -42, -86)
    }

    private lateinit var mLicenseCheckerCallback: LicenseCheckerCallback
    private lateinit var mChecker: LicenseChecker

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val opts = PreferenceManager.getDefaultSharedPreferences(baseContext)
        opts.registerOnSharedPreferenceChangeListener(this)

        // could end up with overlapping fragments
        if (savedInstanceState == null) {
            optFrag = OptionFragment()
            optFrag.retainInstance = true

            fragmentManager
                    .beginTransaction()
                    .add(R.id.optContainer, optFrag)
                    .commit()
        }

        // You can configure Shell here
        Shell.Config.setFlags(Shell.FLAG_REDIRECT_STDERR)
        Shell.Config.verboseLogging(BuildConfig.DEBUG)

        optFrag.applyOnBoot = {
            val isChecked = (optFrag.preferenceManager.findPreference("apply_on_boot") as CheckBoxPreference).isChecked
            editor.putBoolean("apply_on_boot", isChecked)
            editor.apply()
        }

        optFrag.lowMem = {
            val isChecked = (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isChecked
            editor.putBoolean("low_mem", isChecked)
            editor.apply()
        }

        optFrag.disablePowerAware = {
            val isChecked = (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isChecked
            editor.putBoolean("disable_power_aware", isChecked)
            editor.apply()
        }

        optFrag.info = {
            startActivity(Intent(MainActivity@this, InfoActivity::class.java))
        }

        optFrag.killAll = {
            if (running) {
                val popAnim = AnimationUtils.loadAnimation(this, R.anim.pop)
                toggleButton.startAnimation(popAnim)

                toggleButton.background = ContextCompat.getDrawable(this, R.drawable.transition_enable_disable)
                val transition = toggleButton.background as TransitionDrawable

                transition.startTransition(300)
            }

            runnableAsync(this, Runnable {
                Utils.killBin(this)
                toggleButton.text = resources.getText(R.string.off)
                (MainActivity.optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = true
                (MainActivity.optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = true
                running = false
            }, true)
        }

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

        runAsync = RunAsync()

        // setup get_current_objects async task
        val filter = IntentFilter(filter_get_current_options)
        try {
            registerReceiver(runAsync, filter)
        } catch (e: Exception) {}

        toggleButton = findViewById(R.id.toggle)
        toggleButton.isEnabled = false

        runnableAsync(this, Runnable {

            if (!Shell.rootAccess()) {
                AlertDialog.Builder(MainActivity@ this)
                        .setTitle("Root Denied")
                        .setMessage("Root is required to use this application. Please root your device.")
                        .setPositiveButton("Ok") { _, _ ->
                            System.exit(1)
                        }
                        .setCancelable(false)
                        .show()
                return@Runnable
            }

            arch = Utils.getArchitecture()
            Utils.verifyCompat(this)

            bin = Utils.getBinName()
            pathBin = Utils.getBinPath(this)

            runOnUiThread {
                // set the UI elements
                if (Utils.binRunning()) {
                    running = true
                    (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = false
                    (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = false
                    toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_green)
                    toggleButton.text = resources.getText(R.string.on)
                } else {
                    running = false
                    (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = true
                    (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = true
                    toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_red)
                    toggleButton.text = resources.getText(R.string.off)
                }
                (optFrag.preferenceManager.findPreference("apply_on_boot") as CheckBoxPreference).isEnabled = true
                (optFrag.preferenceManager.findPreference("info") as Preference).isEnabled = true
                (optFrag.preferenceManager.findPreference("kill_all") as Preference).isEnabled = true
            }

            Utils.writeBin(this)
            toggleButton.isEnabled = true
        }, true)

        toggleButton.setOnClickListener {
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
                    (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = true
                    (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = true
                } else {
                    Utils.runBin(this)
                    toggleButton.text = resources.getText(R.string.on)
                    (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = false
                    (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = false
                }
                running = !running
            }, true)
        }

        if (prefs.getBoolean("first_run", true)) {
            if (Build.MANUFACTURER.toLowerCase().contains("samsung"))
                editor.putBoolean("low_mem", true)
            editor.putBoolean("first_run", false)
            editor.apply()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(runAsync)
        } catch (e: Exception) {}
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            R.id.donate -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/TylerNijmeh"))
                startActivity(browserIntent)
                true
            }
            R.id.contact -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:tylernij@gmail.com"))
                startActivity(browserIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
            //Log.i("License", "Accepted!")

            if (securePrefs.getString("licensed") != "1")
                securePrefs.put("licensed", "1")
        }

        override fun dontAllow(reason: Int) {
            if (isFinishing) {
                return
            }
            //Log.i("License", "Denied!")
            //Log.i("License", "Reason for denial: $reason")

            if (securePrefs.getString("licensed") != "1")
                securePrefs.put("licensed", "0")
            unlicensedDialog()
        }

        override fun applicationError(reason: Int) {
            //Log.i("License", "Error: $reason")
            if (isFinishing) {
                return
            }

            if (securePrefs.getString("licensed") != "1")
                securePrefs.put("licensed", "0")
            unlicensedDialog()
        }
    }

}
