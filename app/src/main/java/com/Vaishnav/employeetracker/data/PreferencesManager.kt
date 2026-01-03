package com.Vaishnav.employeetracker.data

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "employee_tracker_prefs"
    private const val KEY_FIRST_TIME = "is_first_time"
    private const val KEY_LOGGED_IN_USERNAME = "logged_in_username"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun isFirstTime(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_FIRST_TIME, true)
    }
    
    fun setFirstTimeDone(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_FIRST_TIME, false).apply()
    }
    
    fun resetFirstTime(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_FIRST_TIME, true).apply()
    }
    
    fun saveLoginSession(context: Context, username: String) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_LOGGED_IN_USERNAME, username)
            apply()
        }
    }
    
    fun clearLoginSession(context: Context) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_LOGGED_IN_USERNAME)
            apply()
        }
    }
    
    fun isLoggedIn(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun getLoggedInUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_LOGGED_IN_USERNAME, null)
    }

    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean {
        // Default to false (Light Mode) or system default if you prefer
        return getPreferences(context).getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(context: Context, isEnabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_DARK_MODE, isEnabled).apply()
    }
}
