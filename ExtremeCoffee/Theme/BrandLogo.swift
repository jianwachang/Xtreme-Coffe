import SwiftUI

/// Logo Extreme Coffee — regola fissa del brand:
/// tazza di caffè che fa l'OCCHIOLINO con cuore nel latte, swoosh arancio e linee di
/// velocità, SEMPRE su tile CHIARA/CREMA con cornice scura (mai sfondo scuro).
///
/// Vettoriale nativo: nessun asset esterno richiesto. Usato per header e come base
/// dell'icona app (vedi README: esportare a 1024px su sfondo crema).
struct BrandLogo: View {
    var size: CGFloat = 120
    /// se true disegna la tile crema con cornice scura (per l'icona);
    /// se false solo la tazza (per header su crema)
    var framed: Bool = true

    var body: some View {
        ZStack {
            if framed {
                RoundedRectangle(cornerRadius: size * 0.22, style: .continuous)
                    .fill(EC.cream)
                RoundedRectangle(cornerRadius: size * 0.22, style: .continuous)
                    .strokeBorder(EC.ink, lineWidth: max(2, size * 0.05))
            }
            CupMark()
                .frame(width: size * 0.62, height: size * 0.62)
        }
        .frame(width: size, height: size)
    }
}

/// La tazza vera e propria (swoosh + tazza + occhiolino + cuore + vapore).
private struct CupMark: View {
    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width, h = geo.size.height
            ZStack {
                // Swoosh arancione dietro
                Path { p in
                    p.move(to: CGPoint(x: w * 0.05, y: h * 0.82))
                    p.addQuadCurve(to: CGPoint(x: w * 0.95, y: h * 0.82),
                                   control: CGPoint(x: w * 0.5, y: h * 1.10))
                }
                .stroke(EC.primary, style: StrokeStyle(lineWidth: h * 0.07, lineCap: .round))

                // Linee di velocità
                ForEach(0..<3) { i in
                    let y = h * (0.30 + Double(i) * 0.12)
                    Path { p in
                        p.move(to: CGPoint(x: w * 0.86, y: y))
                        p.addLine(to: CGPoint(x: w * 1.02, y: y))
                    }
                    .stroke(EC.primaryDark.opacity(0.8),
                            style: StrokeStyle(lineWidth: h * 0.045, lineCap: .round))
                }

                // Vapore
                ForEach(0..<2) { i in
                    let x = w * (0.42 + Double(i) * 0.16)
                    Path { p in
                        p.move(to: CGPoint(x: x, y: h * 0.14))
                        p.addQuadCurve(to: CGPoint(x: x, y: h * 0.02),
                                       control: CGPoint(x: x + w * 0.06, y: h * 0.08))
                    }
                    .stroke(EC.muted, style: StrokeStyle(lineWidth: h * 0.035, lineCap: .round))
                }

                // Piattino
                Ellipse()
                    .fill(Color.white)
                    .overlay(Ellipse().stroke(EC.ink, lineWidth: h * 0.03))
                    .frame(width: w * 0.86, height: h * 0.16)
                    .position(x: w * 0.5, y: h * 0.80)

                // Corpo tazza
                RoundedRectangle(cornerRadius: h * 0.14, style: .continuous)
                    .fill(Color.white)
                    .overlay(
                        RoundedRectangle(cornerRadius: h * 0.14, style: .continuous)
                            .stroke(EC.ink, lineWidth: h * 0.045)
                    )
                    .frame(width: w * 0.56, height: h * 0.46)
                    .position(x: w * 0.46, y: h * 0.50)

                // Manico
                Circle()
                    .stroke(EC.ink, lineWidth: h * 0.045)
                    .frame(width: w * 0.20, height: h * 0.20)
                    .position(x: w * 0.74, y: h * 0.50)

                // Caffè + cuore nel latte
                Ellipse()
                    .fill(EC.primaryDark)
                    .frame(width: w * 0.40, height: h * 0.10)
                    .position(x: w * 0.46, y: h * 0.34)
                Image(systemName: "heart.fill")
                    .resizable().scaledToFit()
                    .foregroundColor(EC.cream)
                    .frame(width: w * 0.10)
                    .position(x: w * 0.46, y: h * 0.34)

                // Faccina: occhio aperto + occhiolino + sorriso
                Circle().fill(EC.ink)
                    .frame(width: w * 0.045)
                    .position(x: w * 0.37, y: h * 0.50)
                Path { p in // occhiolino
                    p.move(to: CGPoint(x: w * 0.50, y: h * 0.50))
                    p.addQuadCurve(to: CGPoint(x: w * 0.57, y: h * 0.50),
                                   control: CGPoint(x: w * 0.535, y: h * 0.47))
                }
                .stroke(EC.ink, style: StrokeStyle(lineWidth: h * 0.028, lineCap: .round))
                Path { p in // sorriso
                    p.move(to: CGPoint(x: w * 0.37, y: h * 0.58))
                    p.addQuadCurve(to: CGPoint(x: w * 0.55, y: h * 0.58),
                                   control: CGPoint(x: w * 0.46, y: h * 0.64))
                }
                .stroke(EC.ink, style: StrokeStyle(lineWidth: h * 0.028, lineCap: .round))
            }
        }
    }
}

#Preview {
    VStack(spacing: 24) {
        BrandLogo(size: 140, framed: true)
        BrandLogo(size: 90, framed: false)
    }
    .padding()
    .background(EC.cream)
}
