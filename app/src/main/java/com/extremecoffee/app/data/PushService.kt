package com.extremecoffee.app.data

import com.extremecoffee.app.Notifier
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Riceve i messaggi push FCM. Ad app chiusa/in background la notifica la mostra il sistema;
 *  qui gestiamo il caso app in primo piano e l'aggiornamento del token. */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        CoffeeRepository.saveFcmToken(Profile.id(applicationContext), token, Profile.name(applicationContext))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification
        val title = n?.title ?: message.data["title"] ?: "Extreme Coffee"
        val body = n?.body ?: message.data["body"] ?: ""
        val expiresAt = message.data["expiresAt"]?.toLongOrNull()
        Notifier.showRaw(applicationContext, title, body, message.data["eventId"], expiresAt)
    }
}
