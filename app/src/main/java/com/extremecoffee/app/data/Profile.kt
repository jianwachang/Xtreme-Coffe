package com.extremecoffee.app.data

import android.content.Context
import java.util.UUID

/** Identità locale del telefono: un id stabile + un nome modificabile dall'utente. */
object Profile {
    private const val PREFS = "extreme_coffee"

    fun id(context: Context): String {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getString("uid", null) ?: UUID.randomUUID().toString().also {
            p.edit().putString("uid", it).apply()
        }
    }

    fun name(context: Context): String {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getString("name", null) ?: ("Utente-" + id(context).take(4)).also {
            p.edit().putString("name", it).apply()
        }
    }

    fun phone(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("phone", "") ?: ""

    fun setPhone(context: Context, phone: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("phone", phone).apply()
    }

    fun declined(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("declined", emptySet()) ?: emptySet()

    fun addDeclined(context: Context, id: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = HashSet(p.getStringSet("declined", emptySet()) ?: emptySet())
        cur.add(id)
        p.edit().putStringSet("declined", cur).apply()
    }

    /** Id dell'invito che ho accettato e a cui sto andando (null se nessuno). */
    fun joinedEvent(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("joinedEventId", null)

    fun setJoinedEvent(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("joinedEventId", id).apply()
    }

    fun clearJoinedEvent(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove("joinedEventId").apply()
    }

    fun setName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("name", name.ifBlank { "Utente" }).apply()
    }

    fun isRegistered(context: Context): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.getBoolean("registered", false) && phone(context).isNotBlank()
    }

    fun setRegistered(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("registered", value).apply()
    }

    fun photoPath(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("photoPath", null)

    fun setPhotoPath(context: Context, path: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("photoPath", path).apply()
    }

    fun photo64(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("photo64", "") ?: ""

    fun setPhoto64(context: Context, b64: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("photo64", b64 ?: "").apply()
    }

    /** Ultima settimana per cui è stato inviato il recap (per non duplicarlo). */
    fun lastRecapWeek(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("lastRecapWeek", -1)

    fun setLastRecapWeek(context: Context, week: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt("lastRecapWeek", week).apply()
    }

    /** Cancella tutti i dati locali (profilo, foto, flag). Usato per "Elimina account". */
    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        runCatching { java.io.File(context.filesDir, "profile").deleteRecursively() }
    }
}
