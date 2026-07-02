import SwiftUI

// MARK: - Radar (AMICIZIA nazionale) — mirror di RadarScreen.kt
struct RadarView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme
    @State private var open: CoffeeEvent?

    var events: [CoffeeEvent] { repo.radarEvents(myId: app.myId) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Caffè in amicizia, ovunque in Italia. Uniscitene uno che ti passa vicino \u{1F4CD}")
                        .font(EC.bodyM).foregroundColor(EC.muted)
                    if events.isEmpty {
                        EmptyRow(text: "Il radar è tranquillo adesso. Lancia un caffè in modalità Amicizia!")
                    } else {
                        ForEach(events) { e in
                            Button { open = e } label: { EventCard(event: e, kind: .radar) }
                                .buttonStyle(.plain)
                        }
                    }
                }
                .padding(16)
            }
            .background(EC.background(scheme).ignoresSafeArea())
            .navigationTitle("Radar")
            .sheet(item: $open) { e in InvitePopupView(event: e) }
        }
    }
}

// MARK: - Notifiche (risposte agli inviti) — mirror di NotificationsScreen.kt
struct NotificationsView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme
    @State private var open: CoffeeEvent?

    var incoming: [CoffeeEvent] { repo.incomingInvites(myId: app.myId) }
    var responses: [InviteResponse] { repo.responsesForMe(myId: app.myId) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    SectionTitle("Inviti per te")
                    if incoming.isEmpty {
                        EmptyRow(text: "Nessun invito in sospeso.")
                    } else {
                        ForEach(incoming) { e in
                            Button { open = e } label: { EventCard(event: e, kind: .incoming) }
                                .buttonStyle(.plain)
                        }
                    }

                    SectionTitle("Risposte ai tuoi caffè")
                    if responses.isEmpty {
                        EmptyRow(text: "Ancora nessuna risposta.")
                    } else {
                        ForEach(responses) { r in
                            HStack(spacing: 12) {
                                Image(systemName: r.status == "accepted"
                                      ? "checkmark.circle.fill" : "xmark.circle.fill")
                                    .foregroundColor(r.status == "accepted" ? EC.primary : EC.muted)
                                Text("\(r.fromName) " +
                                     (r.status == "accepted" ? "sta arrivando" : "passa stavolta"))
                                    .font(EC.bodyM).foregroundColor(EC.onBackground(scheme))
                                Spacer()
                            }
                            .padding(14).background(EC.card(scheme))
                            .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
                        }
                    }
                }
                .padding(16)
            }
            .background(EC.background(scheme).ignoresSafeArea())
            .navigationTitle("Notifiche")
            .sheet(item: $open) { e in InvitePopupView(event: e) }
        }
    }
}

// MARK: - Classifica — mirror di LeaderboardScreen.kt
struct LeaderboardView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(Array(repo.leaderboard().enumerated()), id: \.element.id) { i, entry in
                        HStack(spacing: 14) {
                            Text("\(i + 1)").font(EC.serif(22)).foregroundColor(EC.primary)
                                .frame(width: 28)
                            Text(entry.name).font(EC.titleM)
                                .foregroundColor(EC.onBackground(scheme))
                            Spacer()
                            Text("\(entry.total) \u{2615}").font(EC.sans(15, weight: .semibold))
                                .foregroundColor(EC.primaryDark)
                        }
                        .padding(14).background(EC.card(scheme))
                        .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
                    }
                }
                .padding(16)
            }
            .background(EC.background(scheme).ignoresSafeArea())
            .navigationTitle("Classifica")
        }
    }
}

// MARK: - Account — mirror di AccountScreen.kt + BadgesScreen.kt
struct AccountView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme

    var stats: MyStats { repo.loadMyStats(myId: app.myId) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    VStack(spacing: 8) {
                        BrandLogo(size: 84, framed: false)
                        Text(app.myName.isEmpty ? "Tu" : app.myName)
                            .font(EC.headline).foregroundColor(EC.onBackground(scheme))
                        Text(Profile.phone).font(EC.label).foregroundColor(EC.muted)
                    }.padding(.top, 8)

                    HStack(spacing: 10) {
                        StatCard(value: "\(stats.launched)", label: "Lanciati")
                        StatCard(value: "\(stats.joined)", label: "Accettati")
                        StatCard(value: "\(stats.streakWeeks)", label: "Streak")
                    }

                    NavigationLink { BadgesView() } label: {
                        HStack {
                            Image(systemName: "rosette").foregroundColor(EC.primary)
                            Text("I tuoi badge").font(EC.titleM)
                                .foregroundColor(EC.onBackground(scheme))
                            Spacer()
                            Image(systemName: "chevron.right").foregroundColor(EC.muted)
                        }
                        .padding(16).background(EC.card(scheme))
                        .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
                    }
                }
                .padding(16)
            }
            .background(EC.background(scheme).ignoresSafeArea())
            .navigationTitle("Account")
        }
    }
}

struct StatCard: View {
    let value: String, label: String
    @Environment(\.colorScheme) var scheme
    var body: some View {
        VStack(spacing: 4) {
            Text(value).font(EC.serif(28)).foregroundColor(EC.primary)
            Text(label).font(EC.label).foregroundColor(EC.muted)
        }
        .frame(maxWidth: .infinity).padding(.vertical, 16)
        .background(EC.card(scheme))
        .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
    }
}

// MARK: - Badge — mirror di BadgesScreen.kt
struct BadgesView: View {
    @Environment(\.colorScheme) var scheme
    private let badges: [(String, String, String)] = [
        ("cup.and.saucer.fill", "Primo caffè", "Hai lanciato il tuo primo Extreme Coffee"),
        ("bolt.fill", "Fulmine", "Accettato un invito in meno di 2 minuti"),
        ("figure.walk", "Amico vero", "Arrivato al bar da oltre 2 km"),
        ("flame.fill", "Streak", "Una settimana di caffè di fila")
    ]
    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                ForEach(badges, id: \.1) { b in
                    HStack(spacing: 14) {
                        Image(systemName: b.0).font(.title2).foregroundColor(EC.primary)
                            .frame(width: 36)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(b.1).font(EC.titleM).foregroundColor(EC.onBackground(scheme))
                            Text(b.2).font(EC.label).foregroundColor(EC.muted)
                        }
                        Spacer()
                    }
                    .padding(14).background(EC.card(scheme))
                    .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
                }
            }
            .padding(16)
        }
        .background(EC.background(scheme).ignoresSafeArea())
        .navigationTitle("Badge")
        .navigationBarTitleDisplayMode(.inline)
    }
}
