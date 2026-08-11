package com.extremecoffee.app.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Gestisce la lingua dell'app (it/en).
 *
 * PERCHE' QUESTO FILE E' STATO RISCRITTO:
 * il vecchio metodo (salvo la preferenza + avvolgo il Context con createConfigurationContext
 * dentro attachBaseContext) NON funziona piu' da Android 13 (API 33) quando l'app ha
 * targetSdk >= 33: il sistema, subito dopo attachBaseContext, riapplica alla Activity la
 * "lingua per app" del sistema e sovrascrive il nostro override. Risultato: Locale.setDefault
 * cambiava (per questo i nomi dei paesi diventavano inglesi) ma le stringhe di res/values-en
 * non venivano mai caricate e l'interfaccia restava in italiano.
 *
 * Soluzione:
 *  - Android 13+  -> si usa l'API ufficiale android.app.LocaleManager.setApplicationLocales().
 *                    Il sistema applica la lingua a TUTTA l'app (schermate, notifiche, worker)
 *                    e ricrea le Activity da solo.
 *  - Android 8-12 -> si resta sul metodo classico (preferenza + Context avvolto + recreate()),
 *                    che su quelle versioni funziona correttamente.
 */
object LocaleManager {
    private const val PREFS = "extreme_coffee"
    private const val KEY = "app_lang"
    private val SUPPORTED = setOf("it", "en")
    private const val DEFAULT = "it"

    /** Lingua del telefono, usata solo come valore iniziale al primissimo avvio. */
    private fun deviceDefault(): String {
        val lang = runCatching {
            android.content.res.Resources.getSystem().configuration.locales[0].language
        }.getOrNull()
        return if (lang != null && lang in SUPPORTED) lang else DEFAULT
    }

    private fun stored(context: Context): String {
        val v = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return if (v != null && v in SUPPORTED) v else deviceDefault()
    }

    /** Lingua attualmente in uso. Su Android 13+ la fonte di verita' e' il sistema. */
    fun getLang(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val fromSystem = runCatching {
                context.getSystemService(android.app.LocaleManager::class.java)
                    ?.applicationLocales
                    ?.takeIf { !it.isEmpty }
                    ?.get(0)
                    ?.language
            }.getOrNull()
            if (fromSystem != null && fromSystem in SUPPORTED) return fromSystem
        }
        return stored(context)
    }

    /**
     * Imposta la lingua dell'app.
     * @return true se l'Activity va ricreata a mano (Android 8-12);
     *         false se ci pensa il sistema (Android 13+).
     */
    fun setLang(context: Context, lang: String): Boolean {
        val value = if (lang in SUPPORTED) lang else DEFAULT
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, value).apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val applied = runCatching {
                val lm = context.getSystemService(android.app.LocaleManager::class.java)
                if (lm != null) { lm.applicationLocales = LocaleList.forLanguageTags(value); true }
                else false
            }.getOrDefault(false)
            if (applied) return false   // il sistema ricrea le Activity da solo
        }

        Locale.setDefault(Locale(value))
        return true
    }

    /**
     * Context con il locale scelto. Serve fuori dalle Activity (notifiche, worker, servizi):
     * li' il context di sistema non conosce la scelta dell'utente su Android 8-12.
     */
    fun wrap(context: Context): Context {
        val locale = Locale(getLang(context))
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}
