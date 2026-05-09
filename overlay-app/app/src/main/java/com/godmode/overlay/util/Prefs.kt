package com.godmode.overlay.util

import android.content.Context

object Prefs {
    private const val FILE = "godmode_prefs"
    private const val KEY_AUTO_START = "auto_start"

    fun autoStartOnBoot(context: Context): Boolean {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_START, false)
    }

    fun setAutoStartOnBoot(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }
}
