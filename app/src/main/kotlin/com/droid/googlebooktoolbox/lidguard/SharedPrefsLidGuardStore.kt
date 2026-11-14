package com.droid.googlebooktoolbox.lidguard

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists user preferences for "Allow Antigravity and Terminal tasks to continue when lid close".
 * Default [LidGuardConfig.enabled] is always false unless explicitly enabled by the user.
 */
class SharedPrefsLidGuardStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadConfig(): LidGuardConfig = LidGuardConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        cooldownSeconds = prefs.getInt(KEY_COOLDOWN_SECONDS, LidGuardConfig.DEFAULT_COOLDOWN_SECONDS)
            .coerceIn(LidGuardConfig.MIN_COOLDOWN_SECONDS, LidGuardConfig.MAX_COOLDOWN_SECONDS),
        lowBatteryCutoffPercent = prefs.getInt(
            KEY_LOW_BATTERY_CUTOFF,
            LidGuardConfig.DEFAULT_LOW_BATTERY_CUTOFF_PERCENT,
        ).coerceIn(5, 50),
        maxHoldMinutes = prefs.getInt(KEY_MAX_HOLD_MINUTES, LidGuardConfig.DEFAULT_MAX_HOLD_MINUTES)
            .coerceIn(15, 480),
        sharedFolderUri = prefs.getString(KEY_SHARED_FOLDER_URI, null),
    )

    fun saveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun saveCooldownSeconds(seconds: Int) {
        prefs.edit().putInt(
            KEY_COOLDOWN_SECONDS,
            seconds.coerceIn(LidGuardConfig.MIN_COOLDOWN_SECONDS, LidGuardConfig.MAX_COOLDOWN_SECONDS),
        ).apply()
    }

    fun saveLowBatteryCutoffPercent(percent: Int) {
        prefs.edit().putInt(KEY_LOW_BATTERY_CUTOFF, percent.coerceIn(5, 50)).apply()
    }

    fun saveMaxHoldMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_MAX_HOLD_MINUTES, minutes.coerceIn(15, 480)).apply()
    }

    fun saveSharedFolderUri(uri: String?) {
        prefs.edit().apply {
            if (uri.isNullOrBlank()) remove(KEY_SHARED_FOLDER_URI)
            else putString(KEY_SHARED_FOLDER_URI, uri)
        }.apply()
    }

    fun getLastReleaseReason(): String? = prefs.getString(KEY_LAST_RELEASE_REASON, null)

    fun saveLastReleaseReason(reason: String?) {
        prefs.edit().apply {
            if (reason.isNullOrBlank()) remove(KEY_LAST_RELEASE_REASON)
            else putString(KEY_LAST_RELEASE_REASON, reason)
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "lid_guard_prefs"
        private const val KEY_ENABLED = "lid_guard_enabled"
        private const val KEY_COOLDOWN_SECONDS = "cooldown_seconds"
        private const val KEY_LOW_BATTERY_CUTOFF = "low_battery_cutoff"
        private const val KEY_MAX_HOLD_MINUTES = "max_hold_minutes"
        private const val KEY_SHARED_FOLDER_URI = "shared_folder_uri"
        private const val KEY_LAST_RELEASE_REASON = "last_release_reason"
    }
}
