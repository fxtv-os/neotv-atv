package com.neoos.neotv.util

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import java.util.Locale

/**
 * Applies the user-selected app language (DE / EN / NL / TR), independent of
 * the Android TV system locale. Selection is persisted in SharedPreferences
 * under key "pref_language" (set from Settings > Sprache).
 */
object LocaleHelper {

    private const val PREF_KEY = "pref_language"
    private const val DEFAULT_LANG = "de"

    fun getLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_KEY, DEFAULT_LANG) ?: DEFAULT_LANG
    }

    fun setLanguage(context: Context, lang: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_KEY, lang).apply()
    }

    /** Wraps a base Context so all its resources resolve to the chosen language. */
    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
