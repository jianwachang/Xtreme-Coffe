import Foundation

/// Identità locale dell'utente, persistita in UserDefaults.
/// Mirror di data/Profile.kt (che su Android usa SharedPreferences).
enum Profile {
    private static let d = UserDefaults.standard
    private static let kId = "ec_id"
    private static let kName = "ec_name"
    private static let kPhone = "ec_phone"
    private static let kRegistered = "ec_registered"
    private static let kDeclined = "ec_declined"
    private static let kJoined = "ec_joined_event"
    private static let kPhoto64 = "ec_photo64"

    /// Id stabile del dispositivo/utente, generato una volta sola.
    static var id: String {
        if let v = d.string(forKey: kId) { return v }
        let v = UUID().uuidString
        d.set(v, forKey: kId)
        return v
    }

    static var name: String {
        get { d.string(forKey: kName) ?? "" }
        set { d.set(newValue, forKey: kName) }
    }

    static var phone: String {
        get { d.string(forKey: kPhone) ?? "" }
        set { d.set(newValue, forKey: kPhone) }
    }

    static var isRegistered: Bool {
        get { d.bool(forKey: kRegistered) }
        set { d.set(newValue, forKey: kRegistered) }
    }

    /// avatar dell'utente in base64 (per i segnaposto / le risposte)
    static var photo64: String {
        get { d.string(forKey: kPhoto64) ?? "" }
        set { d.set(newValue, forKey: kPhoto64) }
    }

    static var declined: Set<String> {
        get { Set(d.stringArray(forKey: kDeclined) ?? []) }
        set { d.set(Array(newValue), forKey: kDeclined) }
    }

    static func addDeclined(_ id: String) { declined.insert(id) }

    static var joinedEvent: String? {
        get { d.string(forKey: kJoined) }
        set {
            if let v = newValue { d.set(v, forKey: kJoined) }
            else { d.removeObject(forKey: kJoined) }
        }
    }
}

/// Normalizzazione numeri di telefono italiani (mirror leggero di Phones.normalizeIt).
enum Phones {
    /// Restituisce il numero in formato +39XXXXXXXXXX se valido, altrimenti nil.
    static func normalizeIt(_ raw: String) -> String? {
        var s = raw.filter { $0.isNumber || $0 == "+" }
        if s.hasPrefix("+") {
            s = "+" + s.dropFirst().filter { $0.isNumber }
        }
        if s.hasPrefix("+39") { s = String(s.dropFirst(3)) }
        else if s.hasPrefix("0039") { s = String(s.dropFirst(4)) }
        else if s.hasPrefix("39") && s.count > 10 { s = String(s.dropFirst(2)) }
        s = s.filter { $0.isNumber }
        guard s.count >= 9 && s.count <= 11 else { return nil }
        return "+39" + s
    }
}
