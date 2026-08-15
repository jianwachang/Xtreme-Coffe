import SwiftUI
import PhotosUI

/// Registrazione (prima apertura) — replica fedele di ui/screens/RegisterScreen.kt (Android):
/// selettore lingua IT/EN, foto opzionale, nickname, prefisso paese + numero, "Crea profilo".
struct RegisterView: View {
    @EnvironmentObject var app: AppState
    @EnvironmentObject var repo: InMemoryCoffeeRepository
    @Environment(\.colorScheme) var scheme

    @State private var nickname = ""
    @State private var dial: DialCode = Phones.defaultDialCode()
    @State private var national = ""
    @State private var error: String?

    // Foto profilo (opzionale)
    @State private var photoItem: PhotosPickerItem?
    @State private var photo: UIImage?

    // Selettore paese
    @State private var showCountry = false

    /// Testo localizzato IT/EN in base alla lingua scelta.
    private func t(_ it: String, _ en: String) -> String { app.lang == "en" ? en : it }

    private var fullPhone: String? { Phones.international(dialCode: dial.code, national: national) }
    private var canSubmit: Bool {
        !nickname.trimmingCharacters(in: .whitespaces).isEmpty && fullPhone != nil
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {

                // 1) Selettore lingua
                Text(t("In che lingua vuoi usare l'app?", "Which language do you want to use?"))
                    .font(EC.sans(15, weight: .semibold))
                    .foregroundColor(EC.onBackground(scheme))
                    .padding(.top, 8)
                HStack(spacing: 12) {
                    langPill("Italiano", selected: app.lang == "it") { app.lang = "it" }
                    langPill("English", selected: app.lang == "en") { app.lang = "en" }
                }
                .padding(.top, 12)

                // 2) Foto profilo (opzionale)
                VStack(spacing: 8) {
                    PhotosPicker(selection: $photoItem, matching: .images) {
                        ZStack {
                            Circle().fill(EC.card(scheme))
                                .overlay(Circle().stroke(EC.outline, lineWidth: 1))
                            if let img = photo {
                                Image(uiImage: img).resizable().scaledToFill()
                                    .clipShape(Circle())
                            } else {
                                Image(systemName: "person.fill")
                                    .font(.system(size: 46)).foregroundColor(EC.primary)
                            }
                        }
                        .frame(width: 128, height: 128)
                    }
                    Text(photo == nil
                         ? t("Tocca per aggiungere una foto (opzionale)", "Tap to add a photo (optional)")
                         : t("Tocca per cambiare la foto", "Tap to change the photo"))
                        .font(EC.label).foregroundColor(EC.muted)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 24)

                // 3) Titolo + sottotitolo
                Text(t("Crea il tuo profilo", "Create your profile"))
                    .font(EC.headline).foregroundColor(EC.primary)
                    .padding(.top, 24)
                Text(t("Ti serve una sola volta. Scegli un nickname e metti il numero: così gli amici ti riconoscono e ricevi gli inviti direttamente nell'app.",
                       "You only need this once. Pick a nickname and add your number: your friends will recognise you and you'll get invites right in the app."))
                    .font(EC.bodyM).foregroundColor(EC.muted)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 6)

                // 4) Nickname
                HStack(spacing: 10) {
                    Image(systemName: "person.fill").foregroundColor(EC.muted)
                    TextField(t("Nickname (unico)", "Nickname (unique)"), text: $nickname)
                        .foregroundColor(EC.ink)
                        .autocorrectionDisabled(true)
                        .textInputAutocapitalization(.never)
                        .onChange(of: nickname) { _ in error = nil }
                }
                .padding(16)
                .background(EC.card(scheme))
                .overlay(RoundedRectangle(cornerRadius: EC.radiusMedium).stroke(EC.outline, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
                .padding(.top, 24)

                Text(t("Da 3 a 20 caratteri: lettere, numeri, . _ -",
                       "3 to 20 characters: letters, numbers, . _ -"))
                    .font(EC.label).foregroundColor(EC.muted)
                    .padding(.top, 6).padding(.leading, 4)

                // 5) Telefono: prefisso (tendina) + numero
                HStack(spacing: 10) {
                    Button { showCountry = true } label: {
                        HStack(spacing: 6) {
                            Text(dial.code)
                                .font(EC.sans(17, weight: .semibold))
                                .foregroundColor(EC.ink)
                            Image(systemName: "chevron.down")
                                .font(.system(size: 13)).foregroundColor(EC.muted)
                        }
                        .frame(height: 24)
                        .padding(.horizontal, 16).padding(.vertical, 16)
                        .background(EC.card(scheme))
                        .overlay(RoundedRectangle(cornerRadius: EC.radiusMedium).stroke(EC.outline, lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
                    }

                    TextField(t("Numero di telefono", "Phone number"), text: $national)
                        .keyboardType(.phonePad)
                        .foregroundColor(EC.ink)
                        .onChange(of: national) { _ in error = nil }
                        .padding(16)
                        .background(EC.card(scheme))
                        .overlay(RoundedRectangle(cornerRadius: EC.radiusMedium).stroke(EC.outline, lineWidth: 1))
                        .clipShape(RoundedRectangle(cornerRadius: EC.radiusMedium))
                }
                .padding(.top, 14)

                Text(fullPhone != nil
                     ? t("Riconosciuto come \(fullPhone!)", "Recognised as \(fullPhone!)")
                     : t("Il numero serve solo a farti trovare dai tuoi contatti che hanno l'app. Non viene mostrato pubblicamente.",
                         "Your number is only used so your contacts who have the app can find you. It is not shown publicly."))
                    .font(EC.label).foregroundColor(EC.muted)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 6).padding(.leading, 4)

                if let e = error {
                    Text(e).font(EC.bodyM).fontWeight(.semibold)
                        .foregroundColor(EC.error).padding(.top, 10)
                }

                // 6) Crea profilo
                PrimaryButton(title: t("Crea profilo", "Create profile"), enabled: canSubmit) {
                    submit()
                }
                .padding(.top, 24)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
        .background(EC.background(scheme).ignoresSafeArea())
        .sheet(isPresented: $showCountry) {
            CountryPickerSheet(selected: $dial, lang: app.lang)
        }
        .onChange(of: photoItem) { item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let img = UIImage(data: data) {
                    photo = img
                }
            }
        }
    }

    @ViewBuilder
    private func langPill(_ title: String, selected: Bool, _ tap: @escaping () -> Void) -> some View {
        Button(action: tap) {
            Text(title)
                .font(EC.sans(16, weight: .semibold))
                .foregroundColor(selected ? EC.ink : EC.muted)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
        }
        .background(selected ? EC.primaryContainer : EC.card(scheme))
        .overlay(RoundedRectangle(cornerRadius: EC.radiusLarge)
            .stroke(selected ? Color.clear : EC.outline, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: EC.radiusLarge))
    }

    private func submit() {
        guard let phone = fullPhone else { error = t("Numero non valido.", "Invalid number."); return }
        switch repo.registerOnce(nickname: nickname, phone: phone, myId: app.myId) {
        case .success(let nick, let ph):
            app.completeRegistration(name: nick, phone: ph)
        case .nicknameTaken:  error = t("Questo nickname è già in uso.", "This nickname is already taken.")
        case .invalidNickname: error = t("Il nickname è troppo corto.", "The nickname is too short.")
        case .invalidPhone:   error = t("Numero non valido. Controlla il formato.", "Invalid number. Check the format.")
        case .error:          error = t("Qualcosa è andato storto. Riprova.", "Something went wrong. Try again.")
        }
    }
}

/// Foglio di selezione del paese, con ricerca — mirror del dialog "Scegli il paese".
private struct CountryPickerSheet: View {
    @Binding var selected: DialCode
    let lang: String
    @Environment(\.dismiss) var dismiss
    @State private var search = ""

    private var filtered: [DialCode] {
        let all = Phones.dialCodes.sorted { $0.name < $1.name }
        guard !search.isEmpty else { return all }
        return all.filter {
            $0.name.localizedCaseInsensitiveContains(search) || $0.code.contains(search)
        }
    }

    var body: some View {
        NavigationStack {
            List(filtered) { dc in
                Button {
                    selected = dc; dismiss()
                } label: {
                    HStack {
                        Text(dc.name).foregroundColor(EC.ink)
                        Spacer()
                        Text(dc.code).font(EC.sans(15, weight: .semibold)).foregroundColor(EC.primary)
                    }
                }
            }
            .listStyle(.plain)
            .searchable(text: $search,
                        prompt: lang == "en" ? "Search for a country…" : "Cerca un paese…")
            .navigationTitle(lang == "en" ? "Choose your country" : "Scegli il paese")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(lang == "en" ? "Cancel" : "Annulla") { dismiss() }
                }
            }
        }
    }
}
