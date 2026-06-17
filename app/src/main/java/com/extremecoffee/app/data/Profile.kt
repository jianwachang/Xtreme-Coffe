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
}
