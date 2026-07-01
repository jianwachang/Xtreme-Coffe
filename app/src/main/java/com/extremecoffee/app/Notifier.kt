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
                NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun hasPerm(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** PendingIntent che apre l'app nella sezione [route] (deep-link via MainActivity). */
    private fun contentPI(context: Context, requestCode: Int, route: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_route", route)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Rimuove la notifica di un singolo invito (evento annullato, rifiutato o scaduto). */
    fun cancelInvite(context: Context, eventId: String) {
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.cancel(eventId.hashCode())
    }

    /** Rimuove la notifica di risposta (accetta/rifiuta) di uno specifico invitato. */
    fun cancelResponse(context: Context, eventId: String) {
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.cancel(eventId.hashCode())
    }

    /** Rimuove TUTTE le notifiche dell'app. Usato alla riapertura per non lasciare avvisi di caffè scaduti. */
    /** Notifica del promemoria ricorrente. Tap = apre l'app. */
    fun showReminder(context: Context, label: String, notifId: Int) {
        if (!hasPerm(context)) return
        ensureChannel(context)
        val pi = contentPI(context, notifId, "home")
        val text = if (label.isBlank()) context.getString(R.string.push_reminder_body)
        else context.getString(R.string.push_reminder_body_with, label)
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.push_reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)?.notify(notifId, notif)
    }

    fun cancelAll(context: Context) {
        ContextCompat.getSystemService(context, NotificationManager::class.java)?.cancelAll()
    }

    /** Notifica di recap settimanale (statistiche + streak). Tap = apre l'app. */
    fun showRecap(context: Context, title: String, body: String) {
        if (!hasPerm(context)) return
        ensureChannel(context)
        val pi = contentPI(context, "recap".hashCode(), "home")
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify("recap".hashCode(), notif)
    }

    /** Notifica di annullamento. Persistente finché l'utente non la scarta. */
    fun showCancelled(context: Context, title: String, body: String, eventId: String?) {
        if (!hasPerm(context)) return
        ensureChannel(context)
        val pi = contentPI(context, ("cancel_" + (eventId ?: "")).hashCode(), "notifications")
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
        val pi = contentPI(context, event.id.hashCode(), "invite/${event.id}")
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.push_invite_title, event.launcherName))
            .setContentText(context.getString(R.string.push_invite_body, event.barName, event.minutes))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(remaining)             // sparisce da sola quando l'Extreme Coffee scade
            .setContentIntent(pi)
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(event.id.hashCode(), notif)
    }

    /** Notifica generica (usata dal push in primo piano). Se [expiresAt] è noto, scade da sola. */
    fun showRaw(context: Context, title: String, body: String, eventId: String?, expiresAt: Long? = null, route: String? = null) {
        if (!hasPerm(context)) return
        val now = System.currentTimeMillis()
        val remaining = expiresAt?.let { it - now }
        if (remaining != null && remaining <= 0L) return   // arrivato dopo la scadenza: ignora
        ensureChannel(context)
        val finalRoute = route?.takeIf { it.isNotBlank() }
            ?: if (!eventId.isNullOrBlank()) "invite/$eventId" else "notifications"
        val pi = contentPI(context, (eventId ?: title).hashCode(), finalRoute)
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
        val title = if (declined) context.getString(R.string.push_resp_decline_title, response.fromName)
                    else context.getString(R.string.push_resp_accept_title, response.fromName)
        val text = if (declined) context.getString(R.string.push_resp_decline_body, response.fromName)
                   else context.getString(R.string.push_resp_accept_body, response.fromName)
        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(remaining)
            .setContentIntent(contentPI(context, response.eventId.hashCode(), "launched/${response.eventId}"))
            .build()
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.notify(response.eventId.hashCode(), notif)
    }
}
