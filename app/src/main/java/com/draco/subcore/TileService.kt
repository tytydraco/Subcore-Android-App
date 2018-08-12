package com.draco.subcore

import android.os.Build
import android.service.quicksettings.TileService
import android.support.annotation.RequiresApi
import android.service.quicksettings.Tile
import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.Shell


@RequiresApi(Build.VERSION_CODES.N)
class TileService : TileService() {
    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        toggleTile(false)
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    override fun onStartListening() {
        super.onStartListening()
        toggleTile(false)
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    private fun getServiceStatus(): Boolean {
        val prefs = applicationContext.getSharedPreferences("subcore", Context.MODE_PRIVATE)
        return prefs.getBoolean("enabled", false)
    }

    private fun toggleTile(toggle: Boolean = true): Boolean {
        val tile = this.qsTile
        var isActive = getServiceStatus()
        if (toggle)
            isActive = !isActive

        val newState = if (isActive) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }

        tile.state = newState
        tile.updateTile()
        return isActive
    }

    override fun onClick() {
        super.onClick()
        val active = toggleTile()

        MainActivity.prefs = getSharedPreferences("subcore", Context.MODE_PRIVATE)
        MainActivity.prefs.edit().putBoolean("enabled", active).apply()
        MainActivity.arch = Utils.getArchitecture()
        Utils.verifyCompat(applicationContext)
        MainActivity.bin = Utils.getBinName()
        MainActivity.pathBin = Utils.getBinPath(applicationContext)

        if (active) {
            Utils.writeBin(applicationContext)
            var extraArgs = ""
            if (MainActivity.prefs.getBoolean("low_mem", false))
                extraArgs += "-m "
            if (MainActivity.prefs.getBoolean("disable_power_aware", false))
                extraArgs += "-p "
            val command = "[ `pgrep ${MainActivity.bin}` ] || ${MainActivity.pathBin} $extraArgs &"
            Shell.su(command).exec()
        } else
            Shell.su("killall ${MainActivity.bin}").exec()
        val intent = Intent()
        intent.action = filter_refresh_ui
        sendBroadcast(intent)
    }
}