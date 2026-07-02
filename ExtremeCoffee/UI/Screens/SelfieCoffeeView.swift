import SwiftUI

/// Selfie Coffee: scatto incorniciato con il brand (bordo arancione + banda logo
/// "EXTREME COFFEE · ☕ <bar>") e azioni Storia/Condividi/Salva.
/// Mirror di ui/screens/SelfieCoffeeScreen.kt (qui con placeholder foto:
/// l'integrazione fotocamera reale va collegata su device, vedi README).
struct SelfieCoffeeView: View {
    let barName: String
    @Environment(\.colorScheme) var scheme
    @State private var shareText = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Cornice brandizzata
                VStack(spacing: 0) {
                    ZStack {
                        Rectangle().fill(EC.primaryContainer)
                        VStack(spacing: 8) {
                            Image(systemName: "camera.fill").font(.system(size: 40))
                                .foregroundColor(EC.primary)
                            Text("Anteprima scatto")
                                .font(EC.label).foregroundColor(EC.muted)
                        }
                    }
                    .frame(height: 380)

                    // Banda logo in basso
                    HStack(spacing: 8) {
                        BrandLogo(size: 28, framed: false)
                        Text("EXTREME COFFEE · \u{2615} \(barName)")
                            .font(EC.sans(12, weight: .semibold))
                            .foregroundColor(.white).lineLimit(1)
                        Spacer()
                    }
                    .padding(.horizontal, 12).padding(.vertical, 10)
                    .background(EC.ink)
                }
                .overlay(RoundedRectangle(cornerRadius: EC.radiusMedium)
                    .stroke(EC.primary, lineWidth: 4))
                .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))

                // Azioni
                HStack(spacing: 10) {
                    SelfieAction(title: "Storia", icon: "camera.badge.ellipsis")
                    SelfieAction(title: "Condividi", icon: "square.and.arrow.up")
                    SelfieAction(title: "Salva", icon: "square.and.arrow.down")
                }

                Text("Sul dispositivo, \"Storia\" apre la fotocamera e prepara lo scatto per la storia Instagram con questa cornice.")
                    .font(EC.label).foregroundColor(EC.muted)
                    .multilineTextAlignment(.center)
            }
            .padding(16)
        }
        .background(EC.background(scheme).ignoresSafeArea())
        .navigationTitle("Selfie Coffee")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SelfieAction: View {
    let title: String, icon: String
    @Environment(\.colorScheme) var scheme
    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: icon).font(.title3).foregroundColor(EC.primary)
            Text(title).font(EC.label).foregroundColor(EC.onBackground(scheme))
        }
        .frame(maxWidth: .infinity).padding(.vertical, 14)
        .background(EC.card(scheme))
        .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
    }
}
