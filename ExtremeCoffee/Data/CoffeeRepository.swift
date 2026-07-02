import Foundation
import Combine

/// Statistiche personali (mirror di MyStats su Android).
struct MyStats: Equatable {
    var launched: Int = 0
    var joined: Int = 0
    var streakWeeks: Int = 0
    var thisMonth: Int = 0
    var favoriteBar: String = ""
    var atRisk: Bool = false
}

/// Voce di classifica.
struct LeaderEntry: Identifiable, Equatable {
    var id: String
    var name: String
    var launched: Int
    var joined: Int
    var total: Int { launched + joined }
}

enum RegisterResult: Equatable {
    case success(nickname: String, phone: String)
    case nicknameTaken
    case invalidNickname
    case invalidPhone
    case error
}

/// Contratto del repository. Oggi è implementato in-memory (InMemoryCoffeeRepository);
/// domani si può aggiungere FirebaseCoffeeRepository senza toccare la UI, esattamente
/// come sull'Android lo stato locale (StateFlow) è "davanti" a Firestore.
protocol CoffeeRepositoryProtocol: ObservableObject {
    // Stato osservabile
    var events: [String: CoffeeEvent] { get }
    var responses: [InviteResponse] { get }
    var registeredUsers: [AppUser] { get }

    // Lettura derivata
    func eventById(_ id: String) -> CoffeeEvent?
    func incomingInvites(myId: String) -> [CoffeeEvent]
    func myActiveEvent(myId: String) -> CoffeeEvent?
    func myAcceptedActiveEvent(myId: String) -> CoffeeEvent?
    func radarEvents(myId: String) -> [CoffeeEvent]        // AMICIZIA aperti
    func responsesForMe(myId: String) -> [InviteResponse]

    // Azioni
    @discardableResult func launchCoffee(_ event: CoffeeEvent) -> String
    func cancelCoffee(eventId: String)
    func sendResponse(event: CoffeeEvent, fromId: String, fromName: String, status: String)
    func markJoined(eventId: String)
    func declineLocally(eventId: String)
    func registerOnce(nickname: String, phone: String, myId: String) -> RegisterResult
    func registerMe(phone: String, id: String, name: String)

    // Aggregati
    func loadMyStats(myId: String) -> MyStats
    func leaderboard() -> [LeaderEntry]
}

/// Implementazione locale reattiva. L'app è istantanea e non dipende dalla rete.
final class InMemoryCoffeeRepository: CoffeeRepositoryProtocol {
    @Published private(set) var events: [String: CoffeeEvent] = [:]
    @Published private(set) var responses: [InviteResponse] = []
    @Published private(set) var registeredUsers: [AppUser] = []

    private var joinedEventId: String?

    init(seed: Bool = true) {
        if seed { seedDemo() }
    }

    // MARK: Lettura

    func eventById(_ id: String) -> CoffeeEvent? { events[id] }

    private func activeEvents() -> [CoffeeEvent] {
        events.values.filter { !$0.isExpired }
    }

    /// Inviti in arrivo per me (CERCHIA in cui sono tra gli invitati), non ancora rifiutati.
    func incomingInvites(myId: String) -> [CoffeeEvent] {
        activeEvents()
            .filter { $0.mode == "CERCHIA" && $0.invitedIds.contains(myId) && $0.launcherId != myId }
            .filter { !Profile.declined.contains($0.id) }
            .sorted { $0.createdAt > $1.createdAt }
    }

    /// Il caffè attivo che HO lanciato io.
    func myActiveEvent(myId: String) -> CoffeeEvent? {
        activeEvents().first { $0.launcherId == myId }
    }

    /// L'invito attivo che HO accettato.
    func myAcceptedActiveEvent(myId: String) -> CoffeeEvent? {
        guard let jid = joinedEventId ?? Profile.joinedEvent else { return nil }
        return activeEvents().first { $0.id == jid && $0.launcherId != myId }
    }

    /// Radar nazionale: caffè in "AMICIZIA" aperti a tutti.
    func radarEvents(myId: String) -> [CoffeeEvent] {
        activeEvents()
            .filter { $0.mode == "AMICIZIA" && $0.launcherId != myId }
            .sorted { $0.createdAt > $1.createdAt }
    }

    func responsesForMe(myId: String) -> [InviteResponse] {
        responses.filter { $0.launcherId == myId }.sorted { $0.updatedAt > $1.updatedAt }
    }

    // MARK: Azioni

    @discardableResult
    func launchCoffee(_ event: CoffeeEvent) -> String {
        let id = UUID().uuidString
        var e = event
        e.id = id
        events[id] = e
        return id
    }

    func cancelCoffee(eventId: String) {
        if var e = events[eventId] {
            e.cancelled = true
            events[eventId] = e
        }
    }

    func sendResponse(event: CoffeeEvent, fromId: String, fromName: String, status: String) {
        let r = InviteResponse(
            eventId: event.id, launcherId: event.launcherId,
            fromId: fromId, fromName: fromName, status: status,
            updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
            barName: event.barName
        )
        responses.removeAll { $0.eventId == event.id && $0.fromId == fromId }
        responses.append(r)
        if status == "accepted", var e = events[event.id] {
            e.acceptedCount += 1
            events[event.id] = e
        }
    }

    func markJoined(eventId: String) {
        joinedEventId = eventId
        Profile.joinedEvent = eventId
    }

    func declineLocally(eventId: String) {
        Profile.addDeclined(eventId)
        objectWillChange.send()
    }

    func registerOnce(nickname: String, phone: String, myId: String) -> RegisterResult {
        let nick = nickname.trimmingCharacters(in: .whitespaces)
        guard nick.count >= 2 else { return .invalidNickname }
        guard let norm = Phones.normalizeIt(phone) else { return .invalidPhone }
        if registeredUsers.contains(where: { $0.name.lowercased() == nick.lowercased() && $0.id != myId }) {
            return .nicknameTaken
        }
        return .success(nickname: nick, phone: norm)
    }

    func registerMe(phone: String, id: String, name: String) {
        registeredUsers.removeAll { $0.id == id }
        registeredUsers.append(AppUser(id: id, name: name, phone: phone))
    }

    // MARK: Aggregati

    func loadMyStats(myId: String) -> MyStats {
        let launched = events.values.filter { $0.launcherId == myId }.count
        let joined = responses.filter { $0.fromId == myId && $0.status == "accepted" }.count
        let bars = events.values.filter { $0.launcherId == myId }.map { $0.barName }
        let fav = Dictionary(grouping: bars, by: { $0 }).max { $0.value.count < $1.value.count }?.key ?? ""
        return MyStats(launched: launched, joined: joined,
                       streakWeeks: (launched + joined) > 0 ? 1 : 0,
                       thisMonth: launched + joined, favoriteBar: fav, atRisk: false)
    }

    func leaderboard() -> [LeaderEntry] {
        registeredUsers.map { u in
            let l = events.values.filter { $0.launcherId == u.id }.count
            let j = responses.filter { $0.fromId == u.id && $0.status == "accepted" }.count
            return LeaderEntry(id: u.id, name: u.name, launched: l, joined: j)
        }
        .sorted { $0.total > $1.total }
    }

    // MARK: Dati demo (così il Simulatore mostra subito qualcosa)

    private func seedDemo() {
        let me = Profile.id
        registeredUsers = [
            AppUser(id: me, name: Profile.name.isEmpty ? "Tu" : Profile.name, phone: Profile.phone),
            AppUser(id: "marco", name: "Marco", phone: "+393330000001"),
            AppUser(id: "giulia", name: "Giulia", phone: "+393330000002"),
            AppUser(id: "luca", name: "Luca", phone: "+393330000003")
        ]
        // Un invito CERCHIA in arrivo per me (come lo screenshot reale)
        let invite = CoffeeEvent(
            id: UUID().uuidString, launcherId: "marco", launcherName: "Marco",
            barName: "Bar Luce, Fondazione Prada, Largo Isarco 2, Milano",
            barLat: 45.4440, barLng: 9.2052, minutes: 15,
            createdAt: Int64(Date().timeIntervalSince1970 * 1000) - 6 * 60_000, // ~9 min rimasti
            mode: "CERCHIA", acceptedCount: 2, invitedIds: [me]
        )
        events[invite.id] = invite
        // Un caffè AMICIZIA nel radar
        let radar = CoffeeEvent(
            id: UUID().uuidString, launcherId: "giulia", launcherName: "Giulia",
            barName: "Gran Caffè Gambrinus, Napoli", barLat: 40.8375, barLng: 14.2489,
            minutes: 20, createdAt: Int64(Date().timeIntervalSince1970 * 1000) - 3 * 60_000,
            mode: "AMICIZIA", acceptedCount: 5, invitedIds: []
        )
        events[radar.id] = radar
    }
}
