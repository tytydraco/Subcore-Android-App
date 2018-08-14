package com.draco.subcore

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceFragment
import android.widget.ListView

class AboutFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.about)
        with(preferenceManager) {
            findPreference("version").setOnPreferenceClickListener {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                    startActivity(intent)
                }

                return@setOnPreferenceClickListener true
            }
            findPreference("version").summary = "${BuildConfig.VERSION_NAME}-${if (BuildConfig.DEBUG) "debug" else "release"} (${BuildConfig.VERSION_CODE})"

            findPreference("contact").setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:tylernij@gmail.com")))
                return@setOnPreferenceClickListener true
            }

            findPreference("other_apps").setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Tyler+Nijmeh")))
                return@setOnPreferenceClickListener true
            }
            findPreference("donate").setOnPreferenceClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/TylerNijmeh")))
                return@setOnPreferenceClickListener true
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        view?.findViewById<ListView>(android.R.id.list)?.divider = null
    }
}