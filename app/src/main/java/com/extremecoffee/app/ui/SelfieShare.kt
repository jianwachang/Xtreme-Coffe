package com.extremecoffee.app.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.extremecoffee.app.R
import com.extremecoffee.app.model.CoffeeEvent
import java.io.File
import java.io.FileOutputStream

/** Composizione cornice brandizzata + condivisione del Selfie Coffee. */
object SelfieShare {

    /** Decodifica lo scatto, corregge l'orientamento EXIF e applica la cornice. */
    fun frameFromFile(context: Context, file: File, event: CoffeeEvent?): Bitmap {
        var bmp = BitmapFactory.decodeFile(file.absolutePath)
        runCatching {
            val exif = ExifInterface(file.absolutePath)
            val m = Matrix()
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            }
            if (!m.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return drawFrame(context, bmp, event)
    }

    private fun drawFrame(context: Context, src: Bitmap, event: CoffeeEvent?): Bitmap {
        val innerW = src.width.toFloat()
        val innerH = src.height.toFloat()
        val mx = innerW * 0.07f          // margine laterale crema
        val titleH = innerW * 0.24f      // fascia titolo in alto
        val bottomH = innerW * 0.20f     // fascia logo/tagline in basso
        val W = innerW + mx * 2f
        val H = titleH + innerH + bottomH

        val out = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val cream = 0xFFF8E7D3.toInt()
        val orange = 0xFFE8772E.toInt()
        val dark = 0xFF3A2418.toInt()
        val rad = W * 0.06f

        // sfondo crema (predominante)
        c.drawRoundRect(0f, 0f, W, H, rad, rad, Paint().apply { isAntiAlias = true; color = cream })
        // bordo arancione esterno (accento)
        val ins = W * 0.012f
        c.drawRoundRect(ins, ins, W - ins, H - ins, rad * 0.9f, rad * 0.9f,
            Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = W * 0.008f; color = orange })

        // titolo "Selfie Coffee" centrato, due colori
        val tp = Paint().apply {
            isAntiAlias = true; textSize = titleH * 0.42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val s1 = "Selfie "; val s2 = "Coffee"
        val w1 = tp.measureText(s1); val w2 = tp.measureText(s2)
        val tot = w1 + w2
        val tx = (W - tot) / 2f
        val ty = titleH * 0.60f
        tp.color = dark; c.drawText(s1, tx, ty, tp)
        tp.color = orange; c.drawText(s2, tx + w1, ty, tp)
        // sottolineatura arancio
        val uw = tot * 0.5f
        val uy = ty + titleH * 0.14f
        c.drawRoundRect((W - uw) / 2f, uy, (W + uw) / 2f, uy + W * 0.012f, W * 0.01f, W * 0.01f,
            Paint().apply { isAntiAlias = true; color = orange })

        // foto con angoli arrotondati + keyline arancio
        val px = mx; val py = titleH
        val photoRad = W * 0.045f
        c.save()
        val clip = android.graphics.Path().apply {
            addRoundRect(px, py, px + innerW, py + innerH, photoRad, photoRad, android.graphics.Path.Direction.CW)
        }
        c.clipPath(clip)
        c.drawBitmap(src, px, py, null)
        c.restore()
        c.drawRoundRect(px, py, px + innerW, py + innerH, photoRad, photoRad,
            Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = W * 0.008f; color = orange })

        // logo reale in basso a destra
        val ls = bottomH * 0.9f
        val ly = py + innerH + (bottomH - ls) / 2f
        val lx = W - mx - ls
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_coffee_marker)?.let { logo ->
            c.drawBitmap(Bitmap.createScaledBitmap(logo, ls.toInt(), ls.toInt(), true), lx, ly, null)
        }
        c.drawOval(lx, ly, lx + ls, ly + ls,
            Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = W * 0.006f; color = 0xFF281814.toInt() })

        // tagline in basso a sinistra (tono di voce dell'app)
        val tag = Paint().apply { isAntiAlias = true; textSize = bottomH * 0.22f; typeface = Typeface.DEFAULT_BOLD }
        tag.color = dark; c.drawText("Un caff\u00e8. Un timer.", mx, py + innerH + bottomH * 0.42f, tag)
        tag.color = orange; c.drawText("Zero scuse.", mx, py + innerH + bottomH * 0.72f, tag)

        return out
    }

    private fun cacheUri(context: Context, bmp: Bitmap): Uri {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val f = File(dir, "selfie_share.jpg")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    }

    fun shareSystem(context: Context, bmp: Bitmap) {
        val uri = cacheUri(context, bmp)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Extreme Coffee \u2615")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Condividi il tuo Selfie Coffee")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** true se Instagram era installato e ha gestito l'intent della Storia. */
    fun shareInstagramStory(context: Context, bmp: Bitmap): Boolean {
        val uri = cacheUri(context, bmp)
        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(uri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.grantUriPermission("com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent); true
        } else false
    }

    fun saveToGallery(context: Context, bmp: Bitmap): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ExtremeCoffee_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ExtremeCoffee")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@runCatching false
        context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        true
    }.getOrDefault(false)
}
