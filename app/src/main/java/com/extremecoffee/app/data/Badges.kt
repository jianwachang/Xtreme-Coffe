package com.extremecoffee.app.data

/** Un traguardo sbloccabile, con progresso verso la soglia. */
data class Badge(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val earned: Boolean,
    val progress: Int,   // valore attuale (limitato alla soglia)
    val target: Int      // soglia per sbloccarlo
)

/** Calcola lo stato di tutti i badge a partire dalle statistiche dell'utente. */
fun evaluateBadges(s: MyStats): List<Badge> {
    fun b(id: String, emoji: String, title: String, desc: String, value: Int, target: Int) =
        Badge(id, emoji, title, desc, value >= target, value.coerceAtMost(target), target)
    return listOf(
        b("primo", "\u2615", "Primo caffè", "Prendi parte al tuo primo Extreme Coffee", s.total, 1),
        b("re", "\uD83D\uDC51", "Re del caffè", "Lancia 20 Extreme Coffee", s.launched, 20),
        b("anima", "\uD83D\uDE4C", "Anima della compagnia", "Partecipa a 20 caffè altrui", s.joined, 20),
        b("amico", "\uD83D\uDE97", "Amico vero", "Arriva da almeno 10 km per un caffè insieme", s.maxDistanceKm.toInt(), 10),
        b("fedele", "\uD83D\uDD25", "Fedelissimo", "Mantieni uno streak di 4 settimane", s.streakWeeks, 4),
        b("esploratore", "\uD83D\uDDFA\uFE0F", "Esploratore", "Visita 5 locali diversi", s.distinctBars, 5),
        b("veterano", "\uD83C\uDFC5", "Veterano", "Totalizza 50 caffè", s.total, 50)
    )
}
