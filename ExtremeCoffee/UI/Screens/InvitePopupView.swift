import SwiftUI

/// Schermata "Invito" — replica fedele dello screenshot Android:
/// header "Invito", "X ha lanciato un EXTREME COFFEE!!", countdown grande arancione,
/// testo, "✅ STO ARRIVANDO!" e "Stavolta passo 😴". Mirror di InvitePopupScreen.kt.
struct InvitePopupView: View {
    let event: CoffeeEvent
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var scheme

    @State private var now = Int64(Date().timeIntervalSince1970 * 1000)
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    private func t(_ it: String, _ en: String) -> String { app.lang == "en" ? en : it }

    var body: some View {
        NavigationStack {
            VStack(spacing: 18) {
                Spacer()

                Text("\u{2615}\u{1F525}").font(.system(size: 56))

                Text(t("\(event.launcherName) ha lanciato un\nEXTREME COFFEE!!",
                       "\(event.launcherName) started an\nEXTREME COFFEE!!"))
                    .font(EC.serif(30)).foregroundColor(EC.primary)
                    .fontWeight(.black)
                    .multilineTextAlignment(.center)

                let rem = max(0, event.remainingMillis(now: now))
                let mm = Int(rem / 60000), ss = Int((rem % 60000) / 1000)
                Text(String(format: "%02d:%02d", mm, ss))
                    .font(.system(size: 72, weight: .black, design: .default))
                    .foregroundColor(rem < 60000 ? EC.error : EC.primary)
                    .onReceive(timer) { _ in now = Int64(Date().timeIntervalSince1970 * 1000) }

                Text(t("Hai ancora \(mm) minuti per raggiungere gli amici da \(event.barName): il caffè è la scusa, ritrovarsi è il bello!",
                       "You still have \(mm) minutes to reach your friends at \(event.barName): coffee is the excuse, meeting up is the fun!"))
                    .font(EC.body).foregroundColor(EC.onBackground(scheme))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)

                Spacer()

                VStack(spacing: 12) {
                    PrimaryButton(title: t("\u{2705}  STO ARRIVANDO!", "\u{2705}  I'M COMING!"),
                                  enabled: rem > 0) {
                        repo.sendResponse(event: event, fromId: app.myId, fromName: app.myName, status: "accepted")
                        repo.markJoined(eventId: event.id)
                        dismiss()
                    }
                    GhostButton(title: t("Stavolta passo \u{1F634}", "I'll pass this time \u{1F634}")) {
                        repo.sendResponse(event: event, fromId: app.myId, fromName: app.myName, status: "declined")
                        repo.declineLocally(eventId: event.id)
                        dismiss()
                    }
                }
                .padding(.horizontal, 24).padding(.bottom, 12)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(EC.background(scheme).ignoresSafeArea())
            .navigationTitle(t("Invito", "Invite"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button { dismiss() } label: { Image(systemName: "arrow.left").foregroundColor(EC.ink) }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { dismiss() } label: { Image(systemName: "house.fill").foregroundColor(EC.muted) }
                }
            }
        }
    }
}
