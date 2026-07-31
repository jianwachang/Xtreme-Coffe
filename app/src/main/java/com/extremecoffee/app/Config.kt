package com.extremecoffee.app

object Config {
    // Link di RISERVA (usato se non ne hai impostato uno dalla console Firebase).
    // Meglio impostarlo da Firestore: config/app -> campo "downloadUrl" (vedi GUIDA_APK.md),
    // così non devi ricompilare ogni volta che cambia.
    // Punta alla pagina di redirect cross-platform: riconosce Android/iOS e manda allo store giusto
    // (Play Store per Android, App Store per iOS). Conforme alle policy: niente APK esterni.
    // Override possibile da Firestore: config/app -> campo "downloadUrl".
    const val DOWNLOAD_URL =
        "https://www.extremecoffee.it/invita/"

    fun inviteMessage(launcher: String, url: String = DOWNLOAD_URL): String =
        "$launcher ti sta invitando a prendere un caffè insieme \u2615\uD83D\uDD25\n" +
        "Scarica Extreme Coffee e fagli sapere che stai arrivando \uD83D\uDC49 $url"
}
