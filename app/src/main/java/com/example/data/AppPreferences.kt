package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.R

object AppPreferences {
    private const val PREFS_NAME = "productivity_app_prefs"
    const val KEY_THEME = "key_theme_mode" // "system", "dark", "light"
    const val KEY_FONT = "key_font_family" // "space_grotesk", "eb_garamond", "exo2", "zilla_slab", "acme", "system"

    val FONT_OPTIONS = listOf(
        "space_grotesk",
        "eb_garamond",
        "exo2",
        "zilla_slab",
        "acme",
        "system"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): String {
        return getPrefs(context).getString(KEY_THEME, "dark") ?: "dark"
    }

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_THEME, mode).apply()
        applyThemeMode(context)
    }

    fun applyThemeMode(context: Context) {
        val mode = getThemeMode(context)
        val nightMode = when (mode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun getFontKey(context: Context): String {
        return getPrefs(context).getString(KEY_FONT, "space_grotesk") ?: "space_grotesk"
    }

    fun setFontKey(context: Context, fontKey: String) {
        getPrefs(context).edit().putString(KEY_FONT, fontKey).apply()
    }

    fun getFontThemeResId(context: Context): Int {
        return when (getFontKey(context)) {
            "eb_garamond" -> R.style.Theme_Productivity_FontEBGaramond
            "exo2" -> R.style.Theme_Productivity_FontExo2
            "zilla_slab" -> R.style.Theme_Productivity_FontZillaSlab
            "acme" -> R.style.Theme_Productivity_FontAcme
            "system" -> R.style.Theme_Productivity_FontSystem
            else -> R.style.Theme_Productivity_FontSpaceGrotesk
        }
    }

    fun getFontDisplayName(key: String): String {
        return when (key) {
            "space_grotesk" -> "Space Grotesk (Recommended)"
            "eb_garamond" -> "EB Garamond"
            "exo2" -> "Exo 2"
            "zilla_slab" -> "Zilla Slab"
            "acme" -> "Acme"
            else -> "System Default"
        }
    }

    fun getFontResource(key: String): Int? {
        return when (key) {
            "space_grotesk" -> R.font.space_grotesk
            "eb_garamond" -> R.font.eb_garamond
            "exo2" -> R.font.exo2
            "zilla_slab" -> R.font.zilla_slab
            "acme" -> R.font.acme
            else -> null
        }
    }
}
