package com.extremecoffee.app.model

/**
 * Un "Extreme Coffee" lanciato da un utente.
 * mode = "CERCHIA" (cerchia ristretta) oppure "AMICIZIA" (un caffè in amicizia, aperto a tutti)
 */
data class CoffeeEvent(
    val id: String = "",
    val launcherId: String = "",
    val launcherName: String = "",
    val barName: String = "",
    val barLat: Double = 0.0,
    val barLng: Double = 0.0,
    val minutes: Int = 15,
    val createdAt: Long = System.currentTimeMillis(),
    val mode: String = "CERCHIA",
    val acceptedCount: Int = 0,
    val invitedIds: List<String> = emptyList()
) {
    /** Millisecondi rimanenti prima della scadenza del caffè gratis */
    fun remainingMillis(now: Long = System.currentTimeMillis()): Long =
        (createdAt + minutes * 60_000L) - now
}

/** Posizione live di un partecipante in viaggio verso il bar */
data class ParticipantLocation(
    val userId: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val arrived: Boolean = false,
    val updatedAt: Long = 0L
)

/** Risposta di un invitato (accetta/rifiuta) verso chi ha lanciato il caffè */
data class InviteResponse(
    val eventId: String = "",
    val launcherId: String = "",
    val fromId: String = "",
    val fromName: String = "",
    val status: String = "",   // "accepted" oppure "declined"
    val updatedAt: Long = 0L
)

/** Utente registrato nel registro "users" (chi ha l'app). */
data class AppUser(
    val id: String = "",
    val name: String = "",
    val phone: String = ""
)
