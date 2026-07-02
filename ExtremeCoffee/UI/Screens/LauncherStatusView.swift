import SwiftUI

/// Stato del caffè che HAI lanciato: countdown, quante persone hanno accettato,
/// possibilità di annullare. Mirror di ui/screens/LauncherStatusScreen.kt.
struct LauncherStatusView: View {
    let eventId: String
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var scheme

    @State private var now = Int64(Date().timeIntervalSince1970 * 1000)
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var event: CoffeeEvent? { repo.eventById(eventId) }

    var body: some View {
        VStack(spacing: 18) {
            if let e = event {
                Spacer()
                Text("Il tuo Extreme Coffee è live \u{1F525}")
                    .font(EC.headline).foregroundColor(EC.primary)
                    .multilineTextAlignment(.center)

                let rem = max(0, e.remainingMillis(now: now))
                Text(String(format: "%02d:%02d", Int(rem/60000), Int((rem%60000)/1000)))
                    .font(.system(size: 72, weight: .bold, design: .rounded))
                    .foregroundColor(EC.primaryDark)
                    .onReceive(timer) { _ in now = Int64(Date().timeIntervalSince1970 * 1000) }

                Text(e.barName).font(EC.body)
                    .foregroundColor(EC.onBackground(scheme))
                    .multilineTextAlignment(.center).padding(.horizontal, 24)

                // Chi ha accettato
                let accepted = repo.responsesForMe(myId: app.myId)
                    .filter { $0.eventId == e.id && $0.status == "accepted" }
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill").foregroundColor(EC.primary)
                    Text("\(accepted.count + e.acceptedCount) in arrivo")
                        .font(EC.titleM).foregroundColor(EC.onBackground(scheme))
                }
                .padding(.top, 4)

                Spacer()

                NavigationLink { SelfieCoffeeView(barName: e.barName) } label: {
                    Text("\u{1F4F8} Scatta la Selfie Coffee")
                        .font(EC.sans(16, weight: .bold)).foregroundColor(EC.onPrimary)
                        .frame(maxWidth: .infinity).frame(height: 54)
                        .background(EC.primary)
                        .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge))
                }
                .padding(.horizontal, 24)

                Button(role: .destructive) {
                    repo.cancelCoffee(eventId: e.id); dismiss()
                } label: {
                    Text("Annulla il caffè").font(EC.sans(15, weight: .semibold))
                        .frame(maxWidth: .infinity).frame(height: 48)
                }
                .padding(.horizontal, 24).padding(.bottom, 12)
            } else {
                Text("Questo caffè non è più attivo.")
                    .font(EC.body).foregroundColor(EC.muted)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(EC.background(scheme).ignoresSafeArea())
        .navigationTitle("Stato caffè")
        .navigationBarTitleDisplayMode(.inline)
    }
}
