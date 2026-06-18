package com.extremecoffee.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

/** Decodifica un avatar salvato come base64 (o null se vuoto/non valido). */
fun decodeAvatar(b64: String?): Bitmap? {
    if (b64.isNullOrBlank()) return null
    return runCatching {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
