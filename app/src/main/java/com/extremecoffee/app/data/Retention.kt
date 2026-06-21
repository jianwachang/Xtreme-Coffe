package com.extremecoffee.app.data

import java.util.Calendar

/** Statistiche di retention dell'utente, calcolate dagli eventi lanciati e dagli inviti accettati. */
data class MyStats(
    val total: Int = 0,
    val thisMonth: Int = 0,
    val streakWeeks: Int = 0,
    val atRisk: Boolean = false,        // questa settimana ancora nessun caffè: streak da difendere
    val favoriteBar: String = ""
)

/** Indice di settimana (lunedì primo giorno) stabile: settimane consecutive differiscono di 1. */
fun weekIndex(millis: Long): Int {
    val epochDay = Math.floorDiv(millis, 86_400_000L)
    // epochDay 0 = giovedì 1/1/1970; +3 sposta il confine al lunedì
    return Math.floorDiv(epochDay + 3, 7L).toInt()
}

/** Chiave anno*100+mese (1..12), per contare "questo mese". */
fun monthKey(millis: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return c.get(Calendar.YEAR) * 100 + (c.get(Calendar.MONTH) + 1)
}

/**
 * Streak settimanale "indulgente": conta le settimane attive consecutive partendo dalla più
 * recente, tollerando UNA settimana saltata (gap fino a 2). Se l'ultima settimana attiva è più
 * vecchia di 2 settimane rispetto a quella corrente, lo streak è considerato perso (0).
 */
fun computeStreak(activeWeeks: Set<Int>, currentWeek: Int): Int {
    if (activeWeeks.isEmpty()) return 0
    val sorted = activeWeeks.toSortedSet()
    val last = sorted.last()
    if (currentWeek - last > 2) return 0          // troppo tempo fa: streak perso
    var streak = 1
    var prev = last
    for (w in sorted.headSet(last).reversed()) {  // settimane precedenti, dalla più recente
        if (prev - w <= 2) { streak++; prev = w } else break
    }
    return streak
}
