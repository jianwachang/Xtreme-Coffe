package com.extremecoffee.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/** Decodifica un avatar salvato come base64 (o null se vuoto/non valido). */
fun decodeAvatar(b64: String?): Bitmap? {
    if (b64.isNullOrBlank()) return null
    return runCatching {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

/**
 * Copia la foto scelta dall'utente in filesDir/profile/avatar.jpg e ne restituisce il percorso.
 * Usata sia in registrazione sia per modificare la foto dalla Home.
 */
fun saveProfilePhoto(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, "profile").apply { mkdirs() }
    val out = File(dir, "avatar.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        out.outputStream().use { output -> input.copyTo(output) }
    }
    out.absolutePath
}.getOrNull()

/** Ritaglia al centro, ridimensiona a 256x256 e codifica in base64 l'avatar (per eventi e mappe). */
fun makeAvatarBase64(path: String): String? = runCatching {
    val src = BitmapFactory.decodeFile(path) ?: return null
    val w = src.width; val h = src.height; val side = minOf(w, h)
    val cropped = Bitmap.createBitmap(src, (w - side) / 2, (h - side) / 2, side, side)
    val scaled = Bitmap.createScaledBitmap(cropped, 256, 256, true)
    val baos = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
    Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}.getOrNull()
