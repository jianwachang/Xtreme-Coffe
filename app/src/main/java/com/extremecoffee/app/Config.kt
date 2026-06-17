package com.extremecoffee.app

object Config {
    // Link di RISERVA (usato se non ne hai impostato uno dalla console Firebase).
    // Meglio impostarlo da Firestore: config/app -> campo "downloadUrl" (vedi GUIDA_APK.md),
    // così non devi ricompilare ogni volta che cambia.
    const val DOWNLOAD_URL =
        "https://github.com/jianwachang/Xtreme-Coffe/releases/latest/download/app-debug.apk"

    fun inviteMessage(launcher: String, url: String = DOWNLOAD_URL): String =
        "$launcher ti sta invitando a prendere un caffè insieme \u2615\uD83D\uDD25\n" +
        "Scarica Extreme Coffee e fagli sapere che stai arrivando \uD83D\uDC49 $url"
}
