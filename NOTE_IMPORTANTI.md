# ⚠️ Due cose da sapere su questa versione

## 1) Il link di download (punto importante)
Il messaggio WhatsApp contiene `https://extremecoffee.app/download`, che è un
**segnaposto e NON scarica nulla**: quel sito non esiste. È normale, perché l'app
non è ancora ospitata da nessuna parte.

Per farlo scaricare davvero servono **un file APK e un posto dove metterlo**. Le opzioni
gratuite, dalla più semplice:

- **GitHub Releases** (gratis, consigliata): nel tuo repo GitHub crei una "Release",
  carichi `app-debug.apk` come allegato e ottieni un link diretto tipo
  `https://github.com/tuonome/extreme-coffee/releases/download/v1/app-debug.apk`.
- **Firebase App Distribution** o un sito/landing page tua.
- **Google Play Store** (quando sarai pronto a pubblicare).

Poi apri il file **`app/src/main/java/com/extremecoffee/app/Config.kt`** e incolli il
link reale alla riga `DOWNLOAD_URL`. Da quel momento il messaggio farà scaricare l'app.
È centralizzato lì apposta: un solo punto da cambiare.

## 2) L'invito "in-app" ai contatti registrati
Nella modalità **cerchia ristretta**, dopo aver lanciato il caffè, la schermata
"Invita la cerchia" divide i contatti in due gruppi:
- **In app** → invito scritto su Firebase (events/{id}/invites/{telefono}).
- **Da invitare** → messaggio WhatsApp con il link di download.

Perché funzioni davvero servono due pezzi lato server (gratuiti) che ti preparo quando vuoi:
1. **Registro utenti**: quando un utente installa l'app e si registra col numero
   (Firebase Phone Auth), salviamo `users/{uid} = { phone, fcmToken }`. È così che l'app
   capisce chi è "in app".
2. **Cloud Function**: quando scrivo un invito, manda la **notifica push** al telefono
   del contatto, che apre il popup del countdown.

Senza questi due pezzi, col Firebase segnaposto tutti i contatti compaiono come
"Da invitare" (quindi via WhatsApp): è il comportamento prudente e corretto.
Quando crei il vero progetto Firebase, dimmelo e implementiamo registro + Cloud Function.
