package com.draco.subcore

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.text.SpannableString
import android.text.util.Linkify

class AboutActivity : AppCompatActivity() {

    private lateinit var aboutFrag: AboutFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        title = "About"
        if (savedInstanceState == null) {
            aboutFrag = AboutFragment()
            aboutFrag.retainInstance = true
            fragmentManager
                    .beginTransaction()
                    .add(R.id.aboutContainer, aboutFrag)
                    .commit()
        }

        aboutFrag.libsu = {
            val apache2String = SpannableString(getString(R.string.apache2))
            Linkify.addLinks(apache2String, Linkify.ALL)

            AlertDialog.Builder(this)
                    .setTitle("Apache License 2.0")
                    .setMessage(apache2String)
                    .setPositiveButton("Ok", null)
                    .show()
        }

        aboutFrag.version = {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                startActivity(intent)
            }
        }
    }
}