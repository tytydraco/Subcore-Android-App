package com.draco.subcore

import android.os.Bundle
import android.preference.PreferenceFragment
import android.widget.ListView

class OptionFragment : PreferenceFragment() {
    lateinit var about: () -> Unit
    lateinit var killAll: () -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.options)

        with (preferenceManager) {
            findPreference("about")?.setOnPreferenceClickListener {
                about()
                return@setOnPreferenceClickListener true
            }

            findPreference("kill_all")?.setOnPreferenceClickListener {
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