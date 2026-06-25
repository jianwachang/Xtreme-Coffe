package com.extremecoffee.app.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Gestisce la lingua dell'app (it/en) e applica il locale al context. */
object LocaleManager {
    private const val PREFS = "extreme_coffee"
    private const val KEY = "app_lang"

    fun getLang(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "it") ?: "it"

    fun setLang(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** Restituisce un context con il locale scelto, così le risorse usano la lingua giusta. */
    fun wrap(context: Context): Context {
        val locale = Locale(getLang(context))
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
