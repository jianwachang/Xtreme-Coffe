import Foundation

/// Mirror di Config.kt.
enum Config {
    /// Link alla scheda store dell'app. Sull'Android punta al Play Store;
    /// per iOS andrà aggiornato con l'URL App Store una volta pubblicata.
    /// Override possibile da Firestore: config/app -> campo "downloadUrl".
    static let downloadURL =
        "https://play.google.com/store/apps/details?id=com.extremecoffee.myapp"

    /// Messaggio precompilato per condividere l'invito (WhatsApp, ecc.).
    static func inviteMessage(launcher: String, url: String = downloadURL) -> String {
        "\(launcher) ti sta invitando a prendere un caffè insieme \u{2615}\u{1F525}\n" +
        "Scarica Extreme Coffee e fagli sapere che stai arrivando \u{1F449} \(url)"
    }
}
