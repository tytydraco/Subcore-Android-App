package com.draco.subcore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.topjohnwu.superuser.Shell

class BootServiceNotification : Service() {

    val CHANNEL_ID = "subcore_boot_notification"
    lateinit var channel: NotificationChannel
    lateinit var notificationManager: NotificationManager
    lateinit var conatiner: Shell.Container

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
        conatiner = Shell.Config.newContainer()
        displayNotification()

        if (MainActivity.running || !Shell.rootAccess()) {
            dismissNotification()
            return START_NOT_STICKY
        }

        Utils.prefs = getSharedPreferences("subcore", Context.MODE_PRIVATE)
        Utils.editor = Utils.prefs.edit()
        Utils.arch = Utils.getArchitecture()
        Utils.verifyCompat(applicationContext)
        Utils.bin = Utils.getBinName()
        Utils.pathBin = Utils.getBinPath(applicationContext)
        Utils.writeBin(applicationContext)
        Utils.runBin()
        Utils.editor.putBoolean("enabled", true).apply()

        dismissNotification()
        return START_NOT_STICKY
    }
}