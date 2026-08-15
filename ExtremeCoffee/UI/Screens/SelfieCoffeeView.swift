import SwiftUI

/// Selfie Coffee — replica fedele dello screenshot Android: scatto con cornice arancione,
/// banda brand ("EXTREME COFFEE" + "☕ <bar>"), pulsante grande "Storia Instagram",
/// poi "Condividi"/"Salva" e "Rifai lo scatto". Mirror di SelfieCoffeeScreen.kt.
struct SelfieCoffeeView: View {
    let barName: String
    @EnvironmentObject var app: AppState
    @Environment(\.colorScheme) var scheme

    private func t(_ it: String, _ en: String) -> String { app.lang == "en" ? en : it }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {

                Text(t("Selfie Coffee", "Selfie Coffee"))
                    .font(EC.sans(22, weight: .bold))
                    .foregroundColor(EC.onBackground(scheme))
                    .padding(.top, 4)

                // Scatto incorniciato
                VStack(spacing: 0) {
                    ZStack {
                        Rectangle().fill(EC.primaryContainer)
                        VStack(spacing: 8) {
                            Image(systemName: "camera.fill").font(.system(size: 44)).foregroundColor(EC.primary)
                            Text(t("Anteprima scatto", "Photo preview"))
                                .font(EC.label).foregroundColor(EC.muted)
                        }
                    }
                    .frame(height: 340)

                    // Banda brand in basso: logo + EXTREME COFFEE a sx, ☕ bar a dx
                    HStack(alignment: .center) {
                        HStack(spacing: 8) {
                            Image("CoffeeMarker")
                                .resizable().scaledToFit()
                                .frame(width: 34, height: 34)
                            Text("EXTREME\nCOFFEE")
                                .font(EC.sans(13, weight: .bold)).foregroundColor(.white)
                                .lineSpacing(0)
                        }
                        Spacer()
                        Text("\u{2615} \(barName)")
                            .font(EC.sans(13, weight: .bold)).foregroundColor(EC.primary)
                    }
                    .padding(.horizontal, 14).padding(.vertical, 12)
                    .frame(maxWidth: .infinity)
                    .background(EC.ink)
                }
                .overlay(RoundedRectangle(cornerRadius: EC.radiusMedium).stroke(EC.primary, lineWidth: 4))
                .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))

                // Pulsante grande "Storia Instagram"
                Button { } label: {
                    Text(t("Storia Instagram", "Instagram Story"))
                        .font(EC.sans(17, weight: .bold)).foregroundColor(.white)
                        .frame(maxWidth: .infinity).frame(height: 56)
                        .background(EC.primary)
                        .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge, style: .continuous))
                }

                // Condividi | Salva (outline)
                HStack(spacing: 12) {
                    outlineButton(t("Condividi", "Share"))
                    outlineButton(t("Salva", "Save"))
                }

                // Rifai lo scatto
                Button { } label: {
                    Text(t("Rifai lo scatto", "Retake"))
                        .font(EC.sans(15, weight: .semibold)).foregroundColor(EC.muted)
                        .frame(maxWidth: .infinity).frame(height: 44)
                }
            }
            .padding(20)
        }
        .background(EC.background(scheme).ignoresSafeArea())
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func outlineButton(_ title: String) -> some View {
        Text(title)
            .font(EC.sans(16, weight: .bold)).foregroundColor(EC.primary)
            .frame(maxWidth: .infinity).frame(height: 54)
            .background(EC.card(scheme))
            .overlay(RoundedRectangle(cornerRadius: EC.radiusLarge).stroke(EC.outline, lineWidth: 1.5))
            .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge))
    }
}
