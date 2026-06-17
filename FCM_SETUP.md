# Notifiche push ad APP CHIUSA — Cloud Function (FCM)

Lato app è già tutto pronto: ogni dispositivo salva il proprio token FCM nella
collection `tokens` di Firestore, e il `PushService` mostra la notifica.
Manca solo far PARTIRE il push: lo fa la Cloud Function qui nella cartella `functions/`.

## Cosa fa
- Quando viene creato un documento in `events/` (qualcuno lancia un caffè)
  → invia un push a TUTTI i dispositivi con l'app (tranne chi ha lanciato).
- Quando viene creato un documento in `responses/` (qualcuno accetta o rifiuta)
  → invia un push a chi ha lanciato quel caffè.

## ⚠️ Requisito importante: piano Blaze
Le Cloud Functions richiedono il piano **Blaze** (pay-as-you-go) di Firebase.
Ha un tier gratuito molto ampio (per un uso normale non paghi nulla), ma devi
collegare una carta. Senza Blaze NON si possono mandare push ad app chiusa.

## Passi (una volta sola)
1. Installa Node.js 20 dal sito nodejs.org (se non ce l'hai).
2. Installa gli strumenti Firebase:  `npm install -g firebase-tools`
3. Accedi:  `firebase login`
4. Apri il terminale nella cartella del progetto (questa, dove c'è `firebase.json`).
5. Installa le dipendenze della function:
   `cd functions`  →  `npm install`  →  `cd ..`
6. Passa il progetto al piano Blaze:
   Firebase Console → in basso a sinistra "Upgrade" → scegli Blaze
   (puoi mettere un budget di allerta a 1€ per stare tranquillo).
7. Pubblica:  `firebase deploy --only functions`

Se al deploy ti chiede di abilitare delle API (Cloud Functions, Cloud Build,
Eventarc, Artifact Registry), rispondi sì.
Se segnala un problema di "region" per i trigger Firestore: il database è in
`eur3`; la CLI di solito lo gestisce da sola, altrimenti riprova il deploy.

## Come provarlo
- Tieni l'app installata su un telefono e CHIUDILA del tutto (swipe via).
- Da un secondo telefono (o iniettando a mano un documento in `events/`
  come abbiamo già fatto col test `test1`, ricordando tipi `number`) lancia un caffè.
- Sul primo telefono deve arrivare la notifica anche con l'app chiusa.
- Per provare il push di RISPOSTA al lanciatore con un solo telefono: crea a mano
  in `tokens/` un documento con ID = `amico-test` e campo `token` = il token del tuo
  telefono (lo trovi nei log di Logcat all'avvio), poi rifiuta/accetta l'invito di test.

## ⚠️ Telefoni realme/Oppo (ColorOS/realme UI)
Questi telefoni hanno una gestione batteria molto aggressiva che può ritardare o
bloccare le notifiche in background. Sul telefono che RICEVE:
- Impostazioni → App → Extreme Coffee → Batteria → "Consenti attività in background"
  / disattiva l'ottimizzazione batteria per l'app.
- Abilita l'**avvio automatico** (Autostart) per l'app.
- Tienila tra le app "protette" se la tua versione di realme UI lo prevede.
Le notifiche di sistema (quelle qui usate) sono le più affidabili ad app chiusa,
ma su questi marchi un minimo di ritardo può capitare.
