package com.draco.subcore

import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import android.service.quicksettings.Tile
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager

@RequiresApi(Build.VERSION_CODES.N)
class TileService : TileService() {
    override fun onTileAdded() {
        super.onTileAdded()
        toggleTile(false)
    }

    override fun onStartListening() {
        super.onStartListening()
        toggleTile(false)
    }

    private fun getServiceStatus(): Boolean {
        Utils.prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return Utils.prefs.getBoolean("enabled", false)
    }

    private fun toggleTile(toggle: Boolean = true): Boolean {
        val tile = qsTile
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
        Utils.prefs.edit().putBoolean("enabled", active).apply()
        Utils.arch = Utils.getArchitecture()
        Utils.verifyCompat(applicationContext)
        Utils.bin = Utils.getBinName()
        Utils.pathBin = Utils.getBinPath(applicationContext)

        if (active) {
            Utils.writeBin(applicationContext)
            Utils.runBin()
        } else
            Utils.killBin()

        val intent = Intent()
        intent.action = filter_refresh_ui
        sendBroadcast(intent)
    }
}