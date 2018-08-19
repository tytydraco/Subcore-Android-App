package com.draco.subcore

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import android.text.util.Linkify
import com.android.billingclient.api.*

class AboutActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
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

        billingClient = BillingClient.newBuilder(this).setListener(this).build()
        setupBillingClient()
    }

    fun setupBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    setupDonateButton()
                }
            }
            override fun onBillingServiceDisconnected() {
                setupBillingClient()
            }
        })
    }

    fun setupDonateButton() {
        aboutFrag.donate = {
            val flowParams = BillingFlowParams.newBuilder()
                    .setSku("donation")
                    .setType(BillingClient.SkuType.INAPP)
                    .build()
            billingClient.launchBillingFlow(this, flowParams)
        }
    }

    override fun onPurchasesUpdated(@BillingClient.BillingResponse responseCode: Int, purchases: List<Purchase>?) {

    }
}