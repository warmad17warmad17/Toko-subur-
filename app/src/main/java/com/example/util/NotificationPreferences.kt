package com.example.util

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("toko_makmur_notifications", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIF_ENABLED = "low_stock_notif_enabled"
        private const val KEY_SOUND_URI = "low_stock_sound_uri"
        private const val KEY_SOUND_TITLE = "low_stock_sound_title"
        private const val KEY_LAST_SIGNATURE = "low_stock_last_signature"
    }

    var isNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ENABLED, value).apply()

    var soundUri: String
        get() = prefs.getString(KEY_SOUND_URI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SOUND_URI, value).apply()

    var soundTitle: String
        get() = prefs.getString(KEY_SOUND_TITLE, "Suara Sistem Standar") ?: "Suara Sistem Standar"
        set(value) = prefs.edit().putString(KEY_SOUND_TITLE, value).apply()

    var lastNotifiedSignature: String
        get() = prefs.getString(KEY_LAST_SIGNATURE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SIGNATURE, value).apply()

    fun resetSignature() {
        lastNotifiedSignature = ""
    }
}
