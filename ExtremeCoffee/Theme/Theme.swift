import SwiftUI

/// Palette e tipografia dell'app, mirror fedele di ui/theme/Theme.kt.
/// I colori esadecimali sono identici allo schema Compose (chiaro/scuro).
enum EC {

    // MARK: Colori (hex identici a Theme.kt)
    static let primary       = Color(hex: 0xE8772E)   // arancio brand
    static let primaryDark   = Color(hex: 0xC85F1C)
    static let onPrimary     = Color.white
    static let primaryContainer = Color(hex: 0xFCE3CE)
    static let tertiary      = Color(hex: 0xD9A24B)   // oro/crema
    static let cream         = Color(hex: 0xFBF4EA)   // background chiaro
    static let ink           = Color(hex: 0x2A1A0F)   // testo scuro
    static let espresso      = Color(hex: 0x241309)   // background scuro
    static let creamOnDark   = Color(hex: 0xF6EADA)
    static let surface       = Color.white
    static let outline       = Color(hex: 0xE7D6BD)
    static let muted         = Color(hex: 0x9C8770)
    static let error         = Color(hex: 0xB23B2E)

    /// Sfondo adattivo (crema in chiaro, espresso in scuro)
    static func background(_ scheme: ColorScheme) -> Color { scheme == .dark ? espresso : cream }
    static func onBackground(_ scheme: ColorScheme) -> Color { scheme == .dark ? creamOnDark : ink }
    static func card(_ scheme: ColorScheme) -> Color { scheme == .dark ? Color(hex: 0x2A1A0F) : .white }

    // MARK: Forme (mirror AppShapes)
    static let radiusSmall: CGFloat = 14
    static let radiusMedium: CGFloat = 18
    static let radiusLarge: CGFloat = 24

    // MARK: Tipografia
    // Android usa DM Serif Display (titoli) + Poppins (UI). Se i font sono
    // aggiunti al bundle vengono usati, altrimenti si ricade su serif/system.
    static func serif(_ size: CGFloat) -> Font {
        if UIFont(name: "DMSerifDisplay-Regular", size: size) != nil {
            return .custom("DMSerifDisplay-Regular", size: size)
        }
        return .system(size: size, weight: .regular, design: .serif)
    }
    static func sans(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        let name: String
        switch weight {
        case .bold, .heavy: name = "Poppins-Bold"
        case .semibold: name = "Poppins-SemiBold"
        case .medium: name = "Poppins-Medium"
        default: name = "Poppins-Regular"
        }
        if UIFont(name: name, size: size) != nil { return .custom(name, size: size) }
        return .system(size: size, weight: weight)
    }

    // Stili nominali (equivalenti alle voci Material3 usate nell'app)
    static var display   = serif(40)
    static var headline  = serif(27)
    static var titleL    = sans(22, weight: .semibold)
    static var titleM    = sans(16, weight: .semibold)
    static var body      = sans(16)
    static var bodyM     = sans(14)
    static var label     = sans(12, weight: .medium)
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: alpha)
    }
}

/// Pulsante primario arancione a tutta larghezza (equivalente al Button Material3).
struct PrimaryButton: View {
    let title: String
    var enabled: Bool = true
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(EC.sans(16, weight: .bold))
                .foregroundColor(EC.onPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
        }
        .background(enabled ? EC.primary : EC.primary.opacity(0.4))
        .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge, style: .continuous))
        .disabled(!enabled)
    }
}

/// Pulsante secondario "fantasma".
struct GhostButton: View {
    let title: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(EC.sans(15, weight: .semibold))
                .foregroundColor(EC.primaryDark)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
        }
    }
}
