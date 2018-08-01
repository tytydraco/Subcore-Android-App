package com.draco.subcore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat

class BootServiceNotification : Service() {

    val CHANNEL_ID = "subcore_boot_notification"
    lateinit var channel: NotificationChannel
    lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun displayNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = NotificationChannel(CHANNEL_ID,
            "Initializing",
            NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle("Initializing")
                    .setContentText("Starting daemon on boot.").build()

            startForeground(1, notification)
        }
    }

    private fun dismissNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        displayNotification()

        if (MainActivity.running) {
            dismissNotification()
            return START_NOT_STICKY
        }

        root = Root(applicationContext)
        runAsync = RunAsync()

        MainActivity.prefs = getSharedPreferences("subcore", Context.MODE_PRIVATE)

        // setup get_current_objects async task
        val filter = IntentFilter(filter_get_current_options)
        try {
            registerReceiver(runAsync, filter)
        } catch (e: Exception) {}

        // manually check root
        val rootGranted = root.run("id", true)
        if (!rootGranted.contains("root")) {
            dismissNotification()
            return START_NOT_STICKY
        }

        MainActivity.arch = Utils.getArchitecture()
        Utils.verifyCompat(applicationContext)

        MainActivity.bin = Utils.getBinName()
        MainActivity.pathBin = Utils.getBinPath(applicationContext)

        Utils.writeBin(applicationContext)

        // Utils.runBin() is written out raw here
        var extraArgs = ""
        if (MainActivity.prefs.getBoolean("low_mem", false))
            extraArgs += "-m "
        if (MainActivity.prefs.getBoolean("disable_power_aware", false))
            extraArgs += "-p "
        val command = "[ `pgrep ${MainActivity.bin}` ] || ${MainActivity.pathBin} $extraArgs &"
        root.run(command, true)

        dismissNotification()
        this.unregisterReceiver(com.draco.subcore.runAsync)

        return START_NOT_STICKY
    }
}