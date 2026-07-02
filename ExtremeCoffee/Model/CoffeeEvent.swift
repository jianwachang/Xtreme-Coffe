import Foundation

/// Un "Extreme Coffee" lanciato da un utente.
/// mode = "CERCHIA" (cerchia ristretta) oppure "AMICIZIA" (aperto a tutti, radar nazionale)
///
/// Mirror fedele di model/CoffeeEvent.kt del progetto Android.
struct CoffeeEvent: Identifiable, Codable, Equatable {
    var id: String = ""
    var launcherId: String = ""
    var launcherName: String = ""
    var barName: String = ""
    var barLat: Double = 0
    var barLng: Double = 0
    var minutes: Int = 15
    /// millisecondi epoch, come su Android (System.currentTimeMillis())
    var createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    var mode: String = "CERCHIA"
    var acceptedCount: Int = 0
    var invitedIds: [String] = []
    /// avatar piccolo (base64) di chi ha lanciato
    var launcherPhoto: String = ""
    /// true se il lanciatore ha annullato l'Extreme Coffee
    var cancelled: Bool = false

    /// Millisecondi rimanenti prima della scadenza.
    func remainingMillis(now: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) -> Int64 {
        (createdAt + Int64(minutes) * 60_000) - now
    }

    var isExpired: Bool { remainingMillis() <= 0 || cancelled }
}

/// Posizione live di un partecipante in viaggio verso il bar.
struct ParticipantLocation: Identifiable, Codable, Equatable {
    var id: String { userId }
    var userId: String = ""
    var name: String = ""
    var lat: Double = 0
    var lng: Double = 0
    var arrived: Bool = false
    var updatedAt: Int64 = 0
    var photo: String = ""
}

/// Risposta di un invitato (accetta/rifiuta) verso chi ha lanciato il caffè.
struct InviteResponse: Identifiable, Codable, Equatable {
    var id: String { eventId + fromId }
    var eventId: String = ""
    var launcherId: String = ""
    var fromId: String = ""
    var fromName: String = ""
    /// "accepted" oppure "declined"
    var status: String = ""
    var updatedAt: Int64 = 0
    var barName: String = ""
    /// quanto lontano è arrivato il partecipante (badge "amico vero")
    var distanceKm: Double = 0
}

/// Utente registrato nel registro "users" (chi ha l'app).
struct AppUser: Identifiable, Codable, Equatable {
    var id: String = ""
    var name: String = ""
    var phone: String = ""
}
