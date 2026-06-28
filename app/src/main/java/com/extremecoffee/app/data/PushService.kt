package com.extremecoffee.app.data

import com.extremecoffee.app.Notifier
import com.extremecoffee.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Riceve i messaggi push FCM. Ad app chiusa/in background la notifica la mostra il sistema;
 *  qui gestiamo il caso app in primo piano e l'aggiornamento del token. */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        CoffeeRepository.saveFcmToken(Profile.id(applicationContext), token, Profile.name(applicationContext), LocaleManager.getLang(applicationContext))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        // Evento annullato: rimuovo l'eventuale invito ancora a schermo e avviso dell'annullamento.
        if (data["type"] == "cancelled") {
            val eventId = data["eventId"]
            if (!eventId.isNullOrBlank()) Notifier.cancelInvite(applicationContext, eventId)
            val title = data["title"] ?: applicationContext.getString(R.string.push_cancelled_title)
            val body = data["body"] ?: applicationContext.getString(R.string.push_cancelled_body)
            Notifier.showCancelled(applicationContext, title, body, eventId)
            return
        }
        val n = message.notification
        val title = n?.title ?: data["title"] ?: applicationContext.getString(R.string.push_generic_title)
        val body = n?.body ?: data["body"] ?: ""
        val expiresAt = data["expiresAt"]?.toLongOrNull()
        Notifier.showRaw(applicationContext, title, body, data["eventId"], expiresAt, data["nav_route"])
    }
}
