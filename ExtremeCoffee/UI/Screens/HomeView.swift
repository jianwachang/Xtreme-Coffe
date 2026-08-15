import SwiftUI

private let HeroStart = Color(hex: 0xF3923F)
private let HeroEnd   = Color(hex: 0xC85F1C)

/// Home — replica fedele di ui/screens/HomeScreen.kt (Android):
/// header arancione con saluto + CTA, card SLANCIO/CAFFÈ, sezione "Esplora".
struct HomeView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme
    @State private var showLaunch = false
    @State private var openInvite: CoffeeEvent?

    private var mine: CoffeeEvent? { repo.myActiveEvent(myId: app.myId) }
    private var incoming: [CoffeeEvent] { repo.incomingInvites(myId: app.myId) }
    private var stats: MyStats { repo.loadMyStats(myId: app.myId) }

    private func t(_ it: String, _ en: String) -> String { app.lang == "en" ? en : it }
    private var displayName: String {
        let n = app.myName.trimmingCharacters(in: .whitespaces).uppercased()
        return n.isEmpty ? t("AMICO", "FRIEND") : n
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {

                    // ---- HERO arancione ----
                    ZStack(alignment: .topTrailing) {
                        VStack(alignment: .leading, spacing: 0) {
                            Text(t("BUONGIORNO, \(displayName)", "GOOD MORNING, \(displayName)"))
                                .font(EC.sans(12, weight: .semibold))
                                .foregroundColor(Color(hex: 0xFFE6D2))
                            Text(mine != nil
                                 ? t("Caffè in corso!", "Coffee in progress!")
                                 : t("Pronto per\nun caffè?", "Ready for\na coffee?"))
                                .font(EC.serif(32)).foregroundColor(.white)
                                .fixedSize(horizontal: false, vertical: true)
                                .padding(.top, 8)
                            // CTA bianca
                            Button {
                                if let e = mine { openInvite = e } else { showLaunch = true }
                            } label: {
                                Text(mine != nil
                                     ? t("Vedi la mappa", "View the map")
                                     : t("Lancia un Extreme Coffee", "Start an Extreme Coffee"))
                                    .font(EC.sans(15, weight: .bold))
                                    .foregroundColor(HeroEnd)
                                    .padding(.horizontal, 22).padding(.vertical, 14)
                                    .background(Color.white)
                                    .clipShape(Capsule())
                            }
                            .padding(.top, 16)
                        }
                        .padding(20)

                        // logo tazza reale (identico all'Android) in alto a destra
                        Image("CoffeeMarker")
                            .resizable().scaledToFit()
                            .frame(width: 66, height: 66)
                            .padding(18)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(LinearGradient(colors: [HeroStart, HeroEnd],
                                              startPoint: .topLeading, endPoint: .bottomTrailing))
                    .clipShape(RoundedRectangle(cornerRadius: EC.radiusXLarge, style: .continuous))
                    .shadow(color: HeroEnd.opacity(0.25), radius: 10, y: 6)

                    // ---- CARD statistiche ----
                    HStack(spacing: 14) {
                        HomeStatCard(label: t("SLANCIO", "SLANCIO"), icon: "flame.fill",
                                     value: "\(stats.streakWeeks)", unit: t("settimane", "weeks"))
                        HomeStatCard(label: t("CAFFÈ", "COFFEES"), icon: "cup.and.saucer.fill",
                                     value: "\(stats.launched + stats.joined)", unit: t("totali", "total"))
                    }
                    .padding(.top, 14)

                    // ---- ESPLORA ----
                    Text(t("Esplora", "Explore"))
                        .font(EC.sans(17, weight: .bold))
                        .foregroundColor(EC.onBackground(scheme))
                        .padding(.top, 20)

                    NavigationLink { RadarView() } label: {
                        ExploreRow(icon: "dot.radiowaves.left.and.right",
                                   title: t("Radar amici", "Friends radar"),
                                   subtitle: t("Chi è in pausa caffè ora", "Who's on a coffee break now"))
                    }.buttonStyle(.plain).padding(.top, 12)

                    ShareLink(item: URL(string: "https://www.extremecoffee.it/invita/")!) {
                        ExploreRow(icon: "person.badge.plus",
                                   title: t("Invita gli amici", "Invite friends"),
                                   subtitle: t("Falli entrare nel giro", "Bring them on board"))
                    }.buttonStyle(.plain).padding(.top, 12)

                    NavigationLink { RecurringView() } label: {
                        ExploreRow(icon: "clock",
                                   title: t("Caffè ricorrenti", "Recurring coffees"),
                                   subtitle: t("Promemoria settimanali", "Weekly reminders"))
                    }.buttonStyle(.plain).padding(.top, 12)

                    // ---- Inviti in arrivo ----
                    if !incoming.isEmpty {
                        Text(t("Inviti per te", "Invites for you"))
                            .font(EC.sans(17, weight: .bold))
                            .foregroundColor(EC.onBackground(scheme))
                            .padding(.top, 22)
                        ForEach(incoming) { e in
                            Button { openInvite = e } label: { EventCard(event: e, kind: .incoming) }
                                .buttonStyle(.plain).padding(.top, 10)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8).padding(.bottom, 20)
            }
            .background(EC.background(scheme).ignoresSafeArea())
            .navigationBarHidden(true)
            .sheet(isPresented: $showLaunch) { LaunchCoffeeView() }
            .sheet(item: $openInvite) { e in InvitePopupView(event: e) }
        }
    }
}

/// Card statistica (SLANCIO / CAFFÈ) — etichetta + icona in alto, numero grande + unità.
struct HomeStatCard: View {
    let label: String, icon: String, value: String, unit: String
    @Environment(\.colorScheme) var scheme
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label).font(EC.sans(12, weight: .semibold)).foregroundColor(EC.muted)
                Spacer()
                Image(systemName: icon).font(.system(size: 18)).foregroundColor(EC.primary)
            }
            HStack(alignment: .lastTextBaseline, spacing: 8) {
                Text(value).font(EC.serif(32)).foregroundColor(EC.primary)
                Text(unit).font(EC.bodyM).foregroundColor(EC.muted)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(EC.card(scheme))
        .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge))
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
}

/// Riga della sezione "Esplora": icona in tondo, titolo + sottotitolo, freccia.
struct ExploreRow: View {
    let icon: String, title: String, subtitle: String
    @Environment(\.colorScheme) var scheme
    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: EC.radiusMedium)
                    .fill(EC.primary.opacity(0.13))
                Image(systemName: icon).font(.system(size: 18)).foregroundColor(EC.primary)
            }
            .frame(width: 44, height: 44)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(EC.sans(16, weight: .semibold)).foregroundColor(EC.onBackground(scheme))
                Text(subtitle).font(EC.bodyM).foregroundColor(EC.muted)
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundColor(EC.muted)
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(EC.card(scheme))
        .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge))
        .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
    }
}

/// Schermata "Caffè ricorrenti" (promemoria settimanali) — placeholder fedele allo stile.
struct RecurringView: View {
    @EnvironmentObject var app: AppState
    @Environment(\.colorScheme) var scheme
    private func t(_ it: String, _ en: String) -> String { app.lang == "en" ? en : it }
    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                Image(systemName: "clock.badge.checkmark").font(.system(size: 48)).foregroundColor(EC.primary)
                    .padding(.top, 40)
                Text(t("Caffè ricorrenti", "Recurring coffees")).font(EC.serif(24)).foregroundColor(EC.primary)
                Text(t("Presto potrai impostare promemoria settimanali per i tuoi caffè di gruppo.",
                       "Soon you'll be able to set weekly reminders for your group coffees."))
                    .font(EC.bodyM).foregroundColor(EC.muted).multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
        }
        .background(EC.background(scheme).ignoresSafeArea())
        .navigationTitle(t("Caffè ricorrenti", "Recurring coffees"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: Componenti condivisi (usati anche da Radar / Inviti)

struct SectionTitle: View {
    let t: String
    init(_ t: String) { self.t = t }
    var body: some View {
        Text(t).font(EC.titleL).foregroundColor(EC.primaryDark).padding(.top, 4)
    }
}

struct EmptyRow: View {
    let text: String
    @Environment(\.colorScheme) var scheme
    var body: some View {
        Text(text).font(EC.bodyM).foregroundColor(EC.muted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16).background(EC.card(scheme))
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
                Text(kind == .mine ? "Hai lanciato un caffè" : "\(event.launcherName) ti invita")
                    .font(EC.titleM).foregroundColor(EC.onBackground(scheme))
                Text(event.barName).font(EC.label).foregroundColor(EC.muted).lineLimit(1)
            }
            Spacer()
            CountdownBadge(event: event)
        }
        .padding(14).background(EC.card(scheme))
        .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
    }
}

struct CountdownBadge: View {
    let event: CoffeeEvent
    @State private var now = Int64(Date().timeIntervalSince1970 * 1000)
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    var body: some View {
        let rem = max(0, event.remainingMillis(now: now))
        let mm = Int(rem / 60000), ss = Int((rem % 60000) / 1000)
        Text(String(format: "%02d:%02d", mm, ss))
            .font(EC.sans(15, weight: .bold)).foregroundColor(EC.primary)
            .onReceive(timer) { _ in now = Int64(Date().timeIntervalSince1970 * 1000) }
    }
}
