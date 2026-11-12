package com.droid.googlebooktoolbox.data

import android.content.Context

interface ToolboxPreferences {
  var hasSeenWelcomePopup: Boolean
}

class SharedPrefsToolboxPreferences(context: Context) : ToolboxPreferences {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override var hasSeenWelcomePopup: Boolean
    get() = prefs.getBoolean(KEY_HAS_SEEN_WELCOME, false)
    set(value) {
      prefs.edit().putBoolean(KEY_HAS_SEEN_WELCOME, value).apply()
    }

  companion object {
    const val PREFS_NAME = "googlebook_toolbox_prefs"
    const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome_popup"
  }
}
