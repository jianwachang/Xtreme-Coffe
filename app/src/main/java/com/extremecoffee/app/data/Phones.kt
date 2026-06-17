package com.extremecoffee.app.data

/**
 * Normalizza i numeri di telefono in formato E.164 (+39...) in modo DETERMINISTICO,
 * così lo stesso numero salvato in formati diversi combacia tra utenti diversi.
 * Regione predefinita: Italia.
 */
object Phones {
    fun normalizeIt(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val plus = raw.trim().startsWith("+")
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val e164 = when {
            plus -> "+$digits"
            digits.startsWith("00") -> "+" + digits.drop(2)
            digits.startsWith("39") && digits.length >= 12 -> "+$digits"
            else -> "+39$digits"
        }
        return if (e164.length < 8) null else e164
    }
}
