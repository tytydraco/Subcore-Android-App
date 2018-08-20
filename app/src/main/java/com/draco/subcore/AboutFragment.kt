package com.draco.subcore

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v7.app.AlertDialog
import android.widget.ListView

class AboutFragment : PreferenceFragment() {

    lateinit var libsu: () -> Unit
    lateinit var donate: () -> Unit
    lateinit var version: () -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.about)
        with(preferenceManager) {
            findPreference("developer").setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://forum.xda-developers.com/member.php?u=8155542")))
                return@setOnPreferenceClickListener true
            }

            findPreference("version").setOnPreferenceClickListener {
                version()
                return@setOnPreferenceClickListener true
            }
            findPreference("version").summary = "${BuildConfig.VERSION_NAME}-${if (BuildConfig.DEBUG) "debug" else "release"} (${BuildConfig.VERSION_CODE})"

            findPreference("libsu").setOnPreferenceClickListener {
                libsu()
                return@setOnPreferenceClickListener true
            }

            findPreference("contact").setOnPreferenceClickListener {
								try {
                	startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:tylernij@gmail.com")))
								} catch (_: Exception) {}
                return@setOnPreferenceClickListener true
            }

            findPreference("other_apps").setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Tyler+Nijmeh")))
                return@setOnPreferenceClickListener true
            }

            findPreference("donate").setOnPreferenceClickListener {
                donate()
                return@setOnPreferenceClickListener true
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        view?.findViewById<ListView>(android.R.id.list)?.divider = null
    }
}
