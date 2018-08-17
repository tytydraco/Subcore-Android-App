package com.draco.subcore

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import android.text.util.Linkify

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        title = "About"

        if (savedInstanceState == null) {
            val aboutFrag = AboutFragment()
            aboutFrag.retainInstance = true
            fragmentManager
                    .beginTransaction()
                    .add(R.id.aboutContainer, aboutFrag)
                    .commit()
            aboutFrag.libsu = {
                val apache2String = SpannableString(getString(R.string.apache2))
                Linkify.addLinks(apache2String, Linkify.ALL)

                AlertDialog.Builder(this)
                        .setTitle("Apache License 2.0")
                        .setMessage(apache2String)
                        .setPositiveButton("Ok", null)
                        .show()
            }
        }
    }
}