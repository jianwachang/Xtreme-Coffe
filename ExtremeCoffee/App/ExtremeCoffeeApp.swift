import SwiftUI

/// Stato globale condiviso: repository + profilo corrente.
/// Equivalente al pattern MainActivity + Profile + CoffeeRepository su Android.
final class AppState: ObservableObject {
    /// Riferimento stabile: il repo è osservato dalle view come environmentObject a sé,
    /// così i suoi @Published aggiornano la UI (gli ObservableObject annidati non lo fanno).
    let repo = InMemoryCoffeeRepository()
    @Published var isRegistered: Bool = Profile.isRegistered
    @Published var myName: String = Profile.name
    var myId: String { Profile.id }

    func completeRegistration(name: String, phone: String) {
        Profile.name = name
        Profile.phone = phone
        Profile.isRegistered = true
        repo.registerMe(phone: phone, id: myId, name: name)
        myName = name
        isRegistered = true
    }
}

@main
struct ExtremeCoffeeApp: App {
    @StateObject private var app = AppState()

    init() {
        // FIREBASE (da attivare quando aggiungi GoogleService-Info.plist + FirebaseApp SDK):
        //   FirebaseApp.configure()
        // Vedi README_iOS.md → "Collegare Firebase".
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(app)
                .environmentObject(app.repo)
                .tint(EC.primary)
        }
    }
}
