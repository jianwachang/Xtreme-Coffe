package com.extremecoffee.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.extremecoffee.app.model.CoffeeEvent
import com.extremecoffee.app.model.InviteResponse

object Notifier {
    private const val CHANNEL = "extreme_coffee_invites"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Inviti Extreme Coffee", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun hasPerm(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Rimuove la notifica di un singolo invito (evento annullato, rifiutato o scaduto). */
    fun cancelInvite(context: Context, eventId: String) {
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.cancel(eventId.hashCode())
    }

    /** Rimuove TUTTE le notifiche dell'app. Usato alla riapertura per non lasciare avvisi di caffè scaduti. */
    fun cancelAll(context: Context) {
        ContextCompat.getSystemService(context, NotificationManager::class.java)?.cancelAll()
    }

    /** Notifica di annullamento. Persistente finché l'utente non la scarta. */
    fun showCancelled(context: Context, title: String, body: String, eventId: String?) {
        if (!hasPerm(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, ("cancel_" + (eventId ?: "")).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(("cancel_" + (eventId ?: title)).hashCode(), notif)
    }

    fun showInvite(context: Context, event: CoffeeEvent) {
        if (!hasPerm(context)) return
        val remaining = event.remainingMillis()
        if (remaining <= 0L) return                 // già scaduto: non mostrare nulla
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("eventId", event.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, event.id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\u2615 ${event.launcherName} ti ha invitato a un Extreme Coffee!")
            .setContentText("Caff\u00e8 da ${event.barName} \u00b7 hai ${event.minutes} minuti")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(remaining)             // sparisce da sola quando l'Extreme Coffee scade
            .setContentIntent(pi)
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(event.id.hashCode(), notif)
    }

    /** Notifica generica (usata dal push in primo piano). Se [expiresAt] è noto, scade da sola. */
    fun showRaw(context: Context, title: String, body: String, eventId: String?, expiresAt: Long? = null) {
        if (!hasPerm(context)) return
        val now = System.currentTimeMillis()
        val remaining = expiresAt?.let { it - now }
        if (remaining != null && remaining <= 0L) return   // arrivato dopo la scadenza: ignora
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!eventId.isNullOrBlank()) putExtra("eventId", eventId)
        }
        val pi = PendingIntent.getActivity(
            context, (eventId ?: title).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
        if (remaining != null) b.setTimeoutAfter(remaining)
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify((eventId ?: title).hashCode(), b.build())
    }

    /** Notifica al lanciatore (accetta/rifiuta). Scade insieme all'Extreme Coffee (fallback 30 min). */
    fun showResponse(context: Context, response: InviteResponse, expiresAt: Long? = null) {
        if (!hasPerm(context)) return
        val now = System.currentTimeMillis()
        val remaining = (expiresAt ?: (now + 30L * 60_000L)) - now
        if (remaining <= 0L) return
        ensureChannel(context)
        val declined = response.status == "declined"
        val title = if (declined) "\uD83D\uDE34 ${response.fromName} stavolta passa"
                    else "\u2705 ${response.fromName} sta arrivando!"
        val text = if (declined) "Niente Extreme Coffee con ${response.fromName} stavolta."
                   else "Preparati: ${response.fromName} \u00e8 in viaggio verso il bar."
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(remaining)
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(("${response.eventId}_${response.fromId}").hashCode(), notif)
    }
}
