import SwiftUI

/// Home: pulsante per lanciare un Extreme Coffee, il tuo caffè attivo (se presente)
/// e gli inviti in arrivo. Mirror di ui/screens/HomeScreen.kt.
struct HomeView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme
    @State private var showLaunch = false
    @State private var openInvite: CoffeeEvent?

    var mine: CoffeeEvent? { repo.myActiveEvent(myId: app.myId) }
    var incoming: [CoffeeEvent] { repo.incomingInvites(myId: app.myId) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {

                    // Hero + CTA
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            BrandLogo(size: 54, framed: false)
                            Text("Extreme Coffee")
                                .font(EC.serif(26)).foregroundColor(EC.primary)
                        }
                        Text("Un caffè, adesso. Lancia l'invito e vedi chi risponde \u{2615}\u{1F525}")
                            .font(EC.bodyM).foregroundColor(EC.muted)
                        PrimaryButton(title: "Lancia un Extreme Coffee") { showLaunch = true }
                            .padding(.top, 4)
                    }
                    .padding(16)
                    .background(EC.card(scheme))
                    .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge))

                    // Il mio caffè attivo
                    if let e = mine {
                        SectionTitle("Il tuo caffè in corso")
                        NavigationLink { LauncherStatusView(eventId: e.id) } label: {
                            EventCard(event: e, kind: .mine)
                        }.buttonStyle(.plain)
                    }

                    // Inviti in arrivo
                    SectionTitle("Inviti in arrivo")
                    if incoming.isEmpty {
                        EmptyRow(text: "Nessun invito al momento. Lancia tu il primo caffè!")
                    } else {
                        ForEach(incoming) { e in
                            Button { openInvite = e } label: {
                                EventCard(event: e, kind: .incoming)
                            }.buttonStyle(.plain)
                        }
                    }
                }
                .padding(16)
            }
            .background(EC.background(scheme).ignoresSafeArea())
            .navigationTitle("Home")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showLaunch) { LaunchCoffeeView() }
            .sheet(item: $openInvite) { e in InvitePopupView(event: e) }
        }
    }
}

// MARK: Componenti riutilizzabili

struct SectionTitle: View {
    let t: String
    init(_ t: String) { self.t = t }
    var body: some View {
        Text(t).font(EC.titleL)
            .foregroundColor(EC.primaryDark)
            .padding(.top, 4)
    }
}

struct EmptyRow: View {
    let text: String
    @Environment(\.colorScheme) var scheme
    var body: some View {
        Text(text).font(EC.bodyM).foregroundColor(EC.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(EC.card(scheme))
            .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
    }
}

struct EventCard: View {
    enum Kind { case mine, incoming, radar }
    let event: CoffeeEvent
    let kind: Kind
    @Environment(\.colorScheme) var scheme

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle().fill(EC.primary)
                Text(String(event.launcherName.prefix(1)).uppercased())
                    .font(EC.sans(20, weight: .bold)).foregroundColor(.white)
            }
            .frame(width: 48, height: 48)

            VStack(alignment: .leading, spacing: 3) {
                Text(kind == .mine ? "Hai lanciato un caffè"
                                   : "\(event.launcherName) ti invita")
                    .font(EC.titleM).foregroundColor(EC.onBackground(scheme))
                Text(event.barName)
                    .font(EC.label).foregroundColor(EC.muted)
                    .lineLimit(1)
            }
            Spacer()
            CountdownBadge(event: event)
        }
        .padding(14)
        .background(EC.card(scheme))
        .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
    }
}

/// Badge countdown mm:ss aggiornato ogni secondo.
struct CountdownBadge: View {
    let event: CoffeeEvent
    @State private var now = Int64(Date().timeIntervalSince1970 * 1000)
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        let rem = max(0, event.remainingMillis(now: now))
        let mm = Int(rem / 60000), ss = Int((rem % 60000) / 1000)
        Text(String(format: "%02d:%02d", mm, ss))
            .font(EC.sans(15, weight: .bold))
            .foregroundColor(EC.primary)
            .onReceive(timer) { _ in now = Int64(Date().timeIntervalSince1970 * 1000) }
    }
}
