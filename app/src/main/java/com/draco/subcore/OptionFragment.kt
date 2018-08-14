package com.draco.subcore

import android.os.Bundle
import android.preference.PreferenceFragment
import android.widget.ListView

class OptionFragment : PreferenceFragment() {
    lateinit var applyOnBoot: () -> Unit
    lateinit var lowMem: () -> Unit
    lateinit var disablePowerAware: () -> Unit
    lateinit var keepInForeground: () -> Unit
    lateinit var about: () -> Unit
    lateinit var killAll: () -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.options)

        with (preferenceManager) {
            findPreference("apply_on_boot").setOnPreferenceClickListener {
                applyOnBoot()
                return@setOnPreferenceClickListener true
            }

            findPreference("low_mem").setOnPreferenceClickListener {
                lowMem()
                return@setOnPreferenceClickListener true
            }

            findPreference("disable_power_aware").setOnPreferenceClickListener {
                disablePowerAware()
                return@setOnPreferenceClickListener true
            }

            findPreference("keep_in_foreground").setOnPreferenceClickListener {
                keepInForeground()
                return@setOnPreferenceClickListener true
            }

            findPreference("about").setOnPreferenceClickListener {
                about()
                return@setOnPreferenceClickListener true
            }

            findPreference("kill_all").setOnPreferenceClickListener {
                killAll()
                return@setOnPreferenceClickListener true
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        view?.findViewById<ListView>(android.R.id.list)?.divider = null
    }
}