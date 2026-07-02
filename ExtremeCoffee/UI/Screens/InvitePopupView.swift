import SwiftUI

/// Schermata "Invito" — replica fedele dello screenshot reale dell'app Android:
/// "X ha lanciato un EXTREME COFFEE!!", countdown grande, testo (senza "caffè gratis"),
/// pulsante "✅ STO ARRIVANDO!" e "Stavolta passo 😴".
/// Mirror di ui/screens/InvitePopupScreen.kt.
struct InvitePopupView: View {
    let event: CoffeeEvent
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var scheme

    @State private var now = Int64(Date().timeIntervalSince1970 * 1000)
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: 18) {
            Spacer()

            Text("\u{2615} \u{1F525}").font(.system(size: 52))

            Text("\(event.launcherName) ha lanciato un\nEXTREME COFFEE!!")
                .font(EC.serif(30))
                .foregroundColor(EC.primary)
                .multilineTextAlignment(.center)

            let rem = max(0, event.remainingMillis(now: now))
            let mm = Int(rem / 60000), ss = Int((rem % 60000) / 1000)
            Text(String(format: "%02d:%02d", mm, ss))
                .font(.system(size: 78, weight: .bold, design: .rounded))
                .foregroundColor(EC.primaryDark)
                .onReceive(timer) { _ in now = Int64(Date().timeIntervalSince1970 * 1000) }

            Text("Hai ancora \(Int(rem/60000)) minuti per raggiungere gli amici da \(event.barName): il caffè è la scusa, ritrovarsi è il bello!")
                .font(EC.body)
                .foregroundColor(EC.onBackground(scheme))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Spacer()

            VStack(spacing: 8) {
                PrimaryButton(title: "\u{2705} STO ARRIVANDO!") {
                    repo.sendResponse(event: event, fromId: app.myId,
                                          fromName: app.myName, status: "accepted")
                    repo.markJoined(eventId: event.id)
                    dismiss()
                }
                GhostButton(title: "Stavolta passo \u{1F634}") {
                    repo.sendResponse(event: event, fromId: app.myId,
                                          fromName: app.myName, status: "declined")
                    repo.declineLocally(eventId: event.id)
                    dismiss()
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(EC.background(scheme).ignoresSafeArea())
    }
}
