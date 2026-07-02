import SwiftUI
import MapKit

/// Lancia un Extreme Coffee: scegli il bar, i minuti (5/10/15/20/25) e la modalità
/// CERCHIA (amici) o AMICIZIA (radar nazionale). Mirror di LaunchCoffeeScreen.kt.
struct LaunchCoffeeView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var scheme

    private let times = [5, 10, 15, 20, 25]

    @State private var bar = ""
    @State private var minutes = 15
    @State private var mode = "CERCHIA"
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 45.4642, longitude: 9.1900),
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05))

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {

                    Text("Dove vi vedete?").font(EC.titleL)
                    HStack(spacing: 10) {
                        Image(systemName: "magnifyingglass").foregroundColor(EC.primary)
                        TextField("Bar, indirizzo o locale…", text: $bar)
                            .foregroundColor(EC.ink)
                    }
                    .padding(14)
                    .background(EC.card(scheme))
                    .overlay(RoundedRectangle(cornerRadius: EC.radiusMedium).stroke(EC.outline, lineWidth: 1))
                    .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))

                    Map(coordinateRegion: $region)
                        .frame(height: 220)
                        .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge))

                    Text("Tra quanto?").font(EC.titleL)
                    HStack(spacing: 8) {
                        ForEach(times, id: \.self) { t in
                            Chip(label: "\(t)′", selected: minutes == t) { minutes = t }
                        }
                    }

                    Text("A chi?").font(EC.titleL)
                    HStack(spacing: 10) {
                        ModeCard(title: "Cerchia", subtitle: "I tuoi amici",
                                 icon: "person.2.fill",
                                 selected: mode == "CERCHIA") { mode = "CERCHIA" }
                        ModeCard(title: "Amicizia", subtitle: "Radar nazionale",
                                 icon: "scope",
                                 selected: mode == "AMICIZIA") { mode = "AMICIZIA" }
                    }

                    PrimaryButton(title: "Lancia \u{2615}", enabled: !bar.trimmingCharacters(in: .whitespaces).isEmpty) {
                        launch()
                    }
                    .padding(.top, 6)
                }
                .padding(16)
            }
            .background(EC.background(scheme).ignoresSafeArea())
            .navigationTitle("Lancia un caffè")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) {
                Button("Annulla") { dismiss() }
            } }
        }
    }

    private func launch() {
        let e = CoffeeEvent(
            launcherId: app.myId, launcherName: app.myName,
            barName: bar, barLat: region.center.latitude, barLng: region.center.longitude,
            minutes: minutes, mode: mode,
            invitedIds: mode == "CERCHIA" ? repo.registeredUsers.map { $0.id }.filter { $0 != app.myId } : []
        )
        repo.launchCoffee(e)
        dismiss()
    }
}

struct Chip: View {
    let label: String
    let selected: Bool
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(EC.sans(15, weight: .semibold))
                .foregroundColor(selected ? .white : EC.primaryDark)
                .frame(maxWidth: .infinity).frame(height: 46)
                .background(selected ? EC.primary : EC.primaryContainer)
                .clipShape(RoundedRectangle(cornerRadius: EC.radiusSmall))
        }
    }
}

struct ModeCard: View {
    let title: String, subtitle: String, icon: String
    let selected: Bool
    let action: () -> Void
    @Environment(\.colorScheme) var scheme
    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon).font(.title2)
                    .foregroundColor(selected ? .white : EC.primary)
                Text(title).font(EC.titleM)
                    .foregroundColor(selected ? .white : EC.onBackground(scheme))
                Text(subtitle).font(EC.label)
                    .foregroundColor(selected ? .white.opacity(0.9) : EC.muted)
            }
            .frame(maxWidth: .infinity).padding(.vertical, 16)
            .background(selected ? EC.primary : EC.card(scheme))
            .overlay(RoundedRectangle(cornerRadius: EC.radiusMedium)
                .stroke(selected ? Color.clear : EC.outline, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
        }
    }
}
