package com.draco.subcore

import android.annotation.SuppressLint
import android.content.*
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
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
        lateinit var updateUIReceiver: BroadcastReceiver
        lateinit var optFrag: OptionFragment
        fun isFragInit(): Boolean {
            return try {
                optFrag
                true
            } catch (_: UninitializedPropertyAccessException) {
                false
            }
        }

        const val RSA_PRIVATE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjNtzFmxMD6g+5pRzMh1P4V/3dIx88FPRalUZ+c2YnH9jI1k5NsM/fxrNpJVojRLkvmq2L9EIASacZ9pp3XS1f9JtCtyzVXIXUpyEJrTm5Ntm9vaw3YlBOKmyU0FmSEQ4KRCU77V3dxGzNdadsMaWz/ooccidNE28yISFqYT++tRD2lD4FzUfHSqZv+P6L89ZmILlQ71sGv5TDVzIAadqlLrvp6E639NTBFdjSNjXXwVEcSDFBmmqq6YDsvLYSMf9SGX8YsCDAo2MSlzaGV92CwiMUhuxZNIbcawPeA1raQq8KpQ0zNTchcw/GbXQSO1b6jx/2MiseJlkuICq9msglwIDAQAB"
        val SALT = byteArrayOf(-81, 40, 92, 27, -18, -14, 98, 8, 91, 95, -5, 21, 26, -24, 54, -88, 62, 16, -42, -86)
    }

    private lateinit var mLicenseCheckerCallback: LicenseCheckerCallback
    private lateinit var mChecker: LicenseChecker

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Utils.prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Utils.editor = Utils.prefs.edit()

        val opts = PreferenceManager.getDefaultSharedPreferences(baseContext)
        opts.registerOnSharedPreferenceChangeListener(this)

        updateUIReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent_: Intent) {
                try {
                    unregisterReceiver(updateUIReceiver)
                } catch (_: Exception) {}
                recreate()
            }
        }

        try {
            val filter2 = IntentFilter(filter_refresh_ui)
            registerReceiver(updateUIReceiver, filter2)
        } catch (_: Exception) {}

        // could end up with overlapping fragments
        if (savedInstanceState == null || !isFragInit()) {
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

        optFrag.about = {
            startActivity(Intent(MainActivity@this, AboutActivity::class.java))
        }

        optFrag.killAll = {
            AlertDialog.Builder(this, R.style.DialogTheme)
                    .setTitle("Kill All")
                    .setMessage("Are you sure you would like to kill all instances of Subcore?")
                    .setPositiveButton("Yes") { _, _ ->
                        if (Utils.prefs.getBoolean("enabled", false)) {
                            val popAnim = AnimationUtils.loadAnimation(this, R.anim.pop)
                            toggleButton.startAnimation(popAnim)

                            toggleButton.background = ContextCompat.getDrawable(this, R.drawable.transition_enable_disable)
                            val transition = toggleButton.background as TransitionDrawable

                            transition.startTransition(300)
                        }

                        asyncExec {
                            Utils.killBin()
                            runOnUiThread {
                                toggleButton.text = resources.getText(R.string.off)
                                (MainActivity.optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = true
                                (MainActivity.optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = true
                            }
                            Utils.editor.putBoolean("enabled", false)
                            Utils.editor.apply()
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
        }

        mChecker = LicenseChecker(
                this,
                ServerManagedPolicy(this, AESObfuscator(SALT,
                        packageName,
                        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))),
                        RSA_PRIVATE_KEY)
        mLicenseCheckerCallback = MyLicenseCheckerCallback()
        Utils.securePrefs = SecurePreferences(this, "subcore-secure", RSA_PRIVATE_KEY, true)

        /*if (securePrefs.getString("licensed") != "1")
            doCheck()
        */

        toggleButton = findViewById(R.id.toggle)
        toggleButton.isEnabled = false

        asyncExec {
            if (!Shell.rootAccess()) {
                runOnUiThread {
                    AlertDialog.Builder(MainActivity@ this, R.style.DialogTheme)
                            .setTitle("Root Denied")
                            .setMessage("Root is required to use this application. Please root your device.")
                            .setPositiveButton("Ok") { _, _ ->
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                }
                return@asyncExec
            }

            Utils.arch = Utils.getArchitecture()
            Utils.verifyCompat(this)

            Utils.bin = Utils.getBinName()
            Utils.pathBin = Utils.getBinPath(this)

            runOnUiThread {
                // set the UI elements
                if (Utils.binRunning()) {
                    Utils.editor.putBoolean("enabled", true)
                    (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = false
                    (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = false
                    toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_green)
                    toggleButton.text = resources.getText(R.string.on)
                } else {
                    Utils.editor.putBoolean("enabled", false)
                    (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = true
                    (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = true
                    toggleButton.background = ContextCompat.getDrawable(MainActivity@this, R.drawable.rounded_drawable_red)
                    toggleButton.text = resources.getText(R.string.off)
                }
                (optFrag.preferenceManager.findPreference("apply_on_boot") as CheckBoxPreference).isEnabled = true
                (optFrag.preferenceManager.findPreference("about") as Preference).isEnabled = true
                (optFrag.preferenceManager.findPreference("kill_all") as Preference).isEnabled = true

                toggleButton.isEnabled = true
            }

            Utils.writeBin(this)
            Utils.editor.apply()
        }

        toggleButton.setOnClickListener {
            val popAnim = AnimationUtils.loadAnimation(this, R.anim.pop)
            toggleButton.startAnimation(popAnim)

            toggleButton.background = ContextCompat.getDrawable(this, R.drawable.transition_enable_disable)
            val transition = toggleButton.background as TransitionDrawable

            if (Utils.prefs.getBoolean("enabled", false)) {
                transition.startTransition(300)
            } else {
                transition.reverseTransition(0)
                transition.reverseTransition(300)
            }

            asyncExec {
                if (Utils.prefs.getBoolean("enabled", false)) {
                    Utils.editor.putBoolean("enabled", false)
                    Utils.killBin()
                    runOnUiThread {
                        toggleButton.text = resources.getText(R.string.off)
                        (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = true
                        (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = true
                    }
                } else {
                    Utils.editor.putBoolean("enabled", true)
                    Utils.runBin()
                    runOnUiThread {
                        toggleButton.text = resources.getText(R.string.on)
                        (optFrag.preferenceManager.findPreference("low_mem") as CheckBoxPreference).isEnabled = false
                        (optFrag.preferenceManager.findPreference("disable_power_aware") as CheckBoxPreference).isEnabled = false
                    }
                }
                Utils.editor.apply()
            }
        }

        if (Utils.prefs.getBoolean("first_run", true)) {
            if (Build.MANUFACTURER.toLowerCase().contains("samsung") || Build.MANUFACTURER.toLowerCase().contains("lg"))
                Utils.editor.putBoolean("low_mem", true)
            Utils.editor.putBoolean("first_run", false)
            Utils.editor.apply()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(updateUIReceiver)
        } catch (_: Exception) {}
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences?, key: String?) {

    }

    private fun doCheck() {
        mChecker.checkAccess(mLicenseCheckerCallback)
    }

    private fun unlicensedDialog() {
        AlertDialog.Builder(MainActivity@this, R.style.DialogTheme)
                .setTitle("Unlicensed")
                .setMessage("This app is potentially stolen, or Google is having an internal server issue.")
                .setCancelable(false)
                .setOnDismissListener {
                    System.exit(1)
                }.setPositiveButton("Ok", null)
                .show()
    }

    private inner class MyLicenseCheckerCallback : LicenseCheckerCallback {

        override fun allow(reason: Int) {
            if (isFinishing) {
                return
            }
            //Log.i("License", "Accepted!")

            if (Utils.securePrefs.getString("licensed") != "1")
                Utils.securePrefs.put("licensed", "1")
        }

        override fun dontAllow(reason: Int) {
            if (isFinishing) {
                return
            }
            //Log.i("License", "Denied!")
            //Log.i("License", "Reason for denial: $reason")

            if (Utils.securePrefs.getString("licensed") != "1")
                Utils.securePrefs.put("licensed", "0")
            unlicensedDialog()
        }

        override fun applicationError(reason: Int) {
            //Log.i("License", "Error: $reason")
            if (isFinishing) {
                return
            }

            if (Utils.securePrefs.getString("licensed") != "1")
                Utils.securePrefs.put("licensed", "0")
            unlicensedDialog()
        }
    }

}
