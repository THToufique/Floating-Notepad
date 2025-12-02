package com.ripp3r.floatingnotepad.utils

import android.content.Context

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_BUBBLE_SIZE = "bubble_size"
    
    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }
    
    fun setDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }
    
    fun getFontSize(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_FONT_SIZE, 16f)
    }
    
    fun setFontSize(context: Context, size: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_FONT_SIZE, size).apply()
    }
    
    fun getBubbleSize(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BUBBLE_SIZE, 64)
    }
    
    fun setBubbleSize(context: Context, size: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BUBBLE_SIZE, size).apply()
    }
}
