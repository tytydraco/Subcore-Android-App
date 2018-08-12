package com.draco.subcore

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

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
        }
    }
}