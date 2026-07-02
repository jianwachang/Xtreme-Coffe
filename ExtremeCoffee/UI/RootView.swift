import SwiftUI

/// Se non registrato → RegisterView. Altrimenti → le 5 tab principali.
/// Mirror della logica in MainActivity/Navigation (home, radar, leaderboard,
/// notifications, account) con badge sugli inviti in arrivo.
struct RootView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme

    var body: some View {
        Group {
            if app.isRegistered {
                MainTabs()
            } else {
                RegisterView()
            }
        }
        .background(EC.background(scheme).ignoresSafeArea())
    }
}

struct MainTabs: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository

    var pending: Int { repo.incomingInvites(myId: app.myId).count }

    var body: some View {
        TabView {
            HomeView()
                .tabItem { Label("Home", systemImage: "house.fill") }

            RadarView()
                .tabItem { Label("Radar", systemImage: "scope") }

            LeaderboardView()
                .tabItem { Label("Classifica", systemImage: "trophy.fill") }

            NotificationsView()
                .tabItem { Label("Notifiche", systemImage: "bell.fill") }
                .badge(pending)

            AccountView()
                .tabItem { Label("Account", systemImage: "person.fill") }
        }
    }
}
