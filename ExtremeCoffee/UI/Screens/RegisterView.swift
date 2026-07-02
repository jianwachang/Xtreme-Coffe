import SwiftUI

/// Registrazione: nickname + telefono (normalizzato IT) + foto opzionale.
/// Mirror di ui/screens/RegisterScreen.kt.
struct RegisterView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme

    @State private var nickname = ""
    @State private var phone = ""
    @State private var error: String?

    private var normPhone: String? { Phones.normalizeIt(phone) }
    private var canSubmit: Bool { !nickname.trimmingCharacters(in: .whitespaces).isEmpty && normPhone != nil }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Avatar / logo brand
                HStack { Spacer(); BrandLogo(size: 104, framed: false); Spacer() }
                    .padding(.top, 24)

                Text("Crea il tuo profilo")
                    .font(EC.headline).foregroundColor(EC.primary)
                    .padding(.top, 12)
                Text("Bastano un soprannome e il tuo numero: così gli amici ti riconoscono quando lanci o accetti un caffè.")
                    .font(EC.bodyM).foregroundColor(EC.muted)
                    .padding(.top, 2)

                field(title: "Soprannome", text: $nickname,
                      icon: "person.fill", help: "Come ti vedranno gli amici")
                    .padding(.top, 24)

                field(title: "Numero di telefono", text: $phone,
                      icon: "phone.fill",
                      help: normPhone != nil ? "Riconosciuto: \(normPhone!)" : "Es. 333 123 4567",
                      keyboard: .phonePad)
                    .padding(.top, 12)

                if let e = error {
                    Text(e).font(EC.bodyM).foregroundColor(EC.error)
                        .fontWeight(.semibold).padding(.top, 8)
                }

                PrimaryButton(title: "Crea profilo", enabled: canSubmit) {
                    submit()
                }
                .padding(.top, 24)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
        .background(EC.background(scheme).ignoresSafeArea())
    }

    @ViewBuilder
    private func field(title: String, text: Binding<String>, icon: String,
                       help: String, keyboard: UIKeyboardType = .default) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 10) {
                Image(systemName: icon).foregroundColor(EC.primary)
                TextField(title, text: text)
                    .keyboardType(keyboard)
                    .foregroundColor(EC.ink)
                    .onChange(of: text.wrappedValue) { _ in error = nil }
            }
            .padding(14)
            .background(EC.card(scheme))
            .overlay(RoundedRectangle(cornerRadius: EC.radiusMedium)
                .stroke(EC.outline, lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
            Text(help).font(EC.label).foregroundColor(EC.muted)
        }
    }

    private func submit() {
        switch repo.registerOnce(nickname: nickname, phone: phone, myId: app.myId) {
        case .success(let nick, let ph):
            app.completeRegistration(name: nick, phone: ph)
        case .nicknameTaken: error = "Questo soprannome è già in uso."
        case .invalidNickname: error = "Il soprannome è troppo corto."
        case .invalidPhone: error = "Numero non valido. Controlla il formato."
        case .error: error = "Qualcosa è andato storto. Riprova."
        }
    }
}
