package com.extremecoffee.app.data

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone

data class Contact(val name: String, val phone: String, val raw: String)

/** Normalizza un numero a sole cifre (per confronto col registro utenti). */
fun normalizePhone(raw: String): String = raw.filter { it.isDigit() }

/** Legge i contatti col numero di telefono (richiede permesso READ_CONTACTS già concesso). */
fun readContacts(context: Context): List<Contact> {
    val proj = arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER)
    val out = LinkedHashMap<String, Contact>()
    context.contentResolver.query(Phone.CONTENT_URI, proj, null, null, Phone.DISPLAY_NAME + " ASC")
        ?.use { c ->
            val ni = c.getColumnIndex(Phone.DISPLAY_NAME)
            val pi = c.getColumnIndex(Phone.NUMBER)
            while (c.moveToNext()) {
                val name = if (ni >= 0) c.getString(ni) else null
                val raw = if (pi >= 0) c.getString(pi) else null
                if (name.isNullOrBlank() || raw.isNullOrBlank()) continue
                val norm = normalizePhone(raw)
                if (norm.length < 6) continue
                out.putIfAbsent(norm, Contact(name, norm, raw))
            }
        }
    return out.values.toList()
}
