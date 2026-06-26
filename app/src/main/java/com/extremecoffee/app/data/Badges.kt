package com.extremecoffee.app.data

import androidx.annotation.StringRes
import com.extremecoffee.app.R

/** Un traguardo sbloccabile, con progresso verso la soglia. Titoli/descrizioni via risorse (i18n). */
data class Badge(
    val id: String,
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val earned: Boolean,
    val progress: Int,   // valore attuale (limitato alla soglia)
    val target: Int      // soglia per sbloccarlo
)

/** Calcola lo stato di tutti i badge a partire dalle statistiche dell'utente. */
fun evaluateBadges(s: MyStats): List<Badge> {
    fun b(id: String, emoji: String, @StringRes titleRes: Int, @StringRes descRes: Int, value: Int, target: Int) =
        Badge(id, emoji, titleRes, descRes, value >= target, value.coerceAtMost(target), target)
    return listOf(
        b("primo", "\u2615", R.string.bdg_primo_t, R.string.bdg_primo_d, s.total, 1),
        b("re", "\uD83D\uDC51", R.string.bdg_re_t, R.string.bdg_re_d, s.launched, 20),
        b("anima", "\uD83D\uDE4C", R.string.bdg_anima_t, R.string.bdg_anima_d, s.joined, 20),
        b("amico", "\uD83D\uDE97", R.string.bdg_amico_t, R.string.bdg_amico_d, s.maxDistanceKm.toInt(), 10),
        b("fedele", "\uD83D\uDD25", R.string.bdg_fedele_t, R.string.bdg_fedele_d, s.streakWeeks, 4),
        b("esploratore", "\uD83D\uDDFA\uFE0F", R.string.bdg_espl_t, R.string.bdg_espl_d, s.distinctBars, 5),
        b("veterano", "\uD83C\uDFC5", R.string.bdg_vet_t, R.string.bdg_vet_d, s.total, 50)
    )
}

/** Il traguardo più prestigioso già sbloccato (mostrato nel Selfie); null se nessuno. */
fun topEarnedBadge(s: MyStats): Badge? {
    val priority = listOf("veterano", "re", "anima", "amico", "fedele", "esploratore", "primo")
    val earned = evaluateBadges(s).filter { it.earned }
    return priority.firstNotNullOfOrNull { id -> earned.firstOrNull { it.id == id } }
}
