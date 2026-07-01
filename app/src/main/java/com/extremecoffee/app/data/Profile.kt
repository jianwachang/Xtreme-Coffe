package com.extremecoffee.app.data

import android.content.Context
import com.extremecoffee.app.model.AppUser
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

    /** Timestamp (updatedAt) dell'ultima risposta già "vista": evita di ri-notificare le vecchie alla riapertura. */
    fun lastRespSeen(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("lastRespSeen", 0L)

    fun setLastRespSeen(context: Context, ts: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong("lastRespSeen", ts).apply()
    }

    /** Cache locale: numeri (normalizzati) dei contatti che HANNO l'app.
     *  Serve per mostrare subito la schermata "Invita amici" senza ricalcolare via rete a ogni apertura. */
    fun cachedRegisteredPhones(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("regPhonesCache", emptySet())?.toSet() ?: emptySet()

    fun setCachedRegisteredPhones(context: Context, phones: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet("regPhonesCache", HashSet(phones)).apply()
    }

    /** Cache locale: mappa numero -> utente app (con id), per l'invito in-app della cerchia. */
    fun cachedRegisteredUsers(context: Context): Map<String, AppUser> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("regUsersCache", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        return raw.split("\n").mapNotNull { line ->
            val p = line.split("\t")
            if (p.size >= 3 && p[0].isNotBlank()) p[0] to AppUser(p[1], p[2], p[0]) else null
        }.toMap()
    }

    fun setCachedRegisteredUsers(context: Context, map: Map<String, AppUser>) {
        val raw = map.entries.joinToString("\n") { (phone, u) ->
            phone + "\t" + u.id + "\t" + u.name.replace('\t', ' ').replace('\n', ' ')
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("regUsersCache", raw).apply()
    }

    /** Cache locale della lista contatti (nome/telefono/raw): la schermata Invita amici
     *  si apre subito con l'ultimo elenco noto, poi si aggiorna in background. */
    fun cachedContacts(context: Context): List<Contact> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("contactsCache", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val p = line.split("\t")
            if (p.size >= 3) Contact(p[0], p[1], p[2]) else null
        }
    }

    fun setCachedContacts(context: Context, list: List<Contact>) {
        fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ')
        val raw = list.joinToString("\n") { c -> clean(c.name) + "\t" + clean(c.phone) + "\t" + clean(c.raw) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("contactsCache", raw).apply()
    }

    /** Cancella tutti i dati locali (profilo, foto, flag). Usato per "Elimina account". */
    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        runCatching { java.io.File(context.filesDir, "profile").deleteRecursively() }
    }
}
