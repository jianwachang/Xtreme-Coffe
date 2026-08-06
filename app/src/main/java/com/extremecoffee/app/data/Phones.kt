package com.extremecoffee.app.data

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * Normalizza i numeri di telefono in formato internazionale E.164 (es. +39..., +33..., +1...)
 * in modo DETERMINISTICO, così lo stesso numero salvato in formati diversi combacia tra
 * utenti diversi (fondamentale per il riconoscimento contatti e gli inviti).
 *
 * Funziona in tutto il mondo: se il numero include già il prefisso internazionale ("+" o "00")
 * viene riconosciuto correttamente in qualsiasi paese. Se invece l'utente scrive un numero
 * "locale" senza prefisso, viene interpretato usando come riferimento la regione impostata
 * sul dispositivo (non più sempre l'Italia), così un francese che scrive il suo numero
 * francese senza indicativo ottiene comunque il numero corretto.
 *
 * Il nome "normalizeIt" è storico (da quando la funzione gestiva solo numeri italiani):
 * non l'ho rinominato per non dover toccare tutti i punti dell'app che la richiamano.
 */
object Phones {
    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    /** Regione di riferimento per i numeri scritti senza prefisso internazionale. */
    private fun defaultRegion(): String {
        val c = Locale.getDefault().country
        return if (c.isNullOrBlank()) "IT" else c.uppercase(Locale.ROOT)
    }

    fun normalizeIt(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim()
        val viaLib = runCatching {
            val number = util.parse(cleaned, defaultRegion())
            if (util.isValidNumber(number) || util.isPossibleNumber(number)) {
                util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else null
        }.getOrNull()
        if (viaLib != null) return viaLib

        // Ripiego (solo se libphonenumber non riesce proprio a interpretare il numero):
        // stessa logica robusta di prima, ma senza presupporre più l'Italia per i numeri
        // che iniziano già con "+" o "00" (quelli restano quello che l'utente ha scritto).
        val plus = cleaned.startsWith("+")
        val digits = cleaned.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val e164 = when {
            plus -> "+$digits"
            digits.startsWith("00") -> "+" + digits.drop(2)
            else -> "+$digits"
        }
        return if (e164.length < 8) null else e164
    }
}
