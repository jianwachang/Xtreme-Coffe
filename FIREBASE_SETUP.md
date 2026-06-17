# 🔌 Collegare il backend Firebase (per far vedere i telefoni tra loro)

Il codice del backend è già pronto. Manca solo creare IL TUO progetto Firebase e
incollare il file di configurazione. ~10 minuti, gratis (piano Spark).

## Passi
1. Vai su https://console.firebase.google.com → **Aggiungi progetto** → chiamalo
   "ExtremeCoffee" (puoi collegarlo allo stesso progetto Google Cloud della chiave Maps).
2. Dentro il progetto, icona **Android** → "Registra app":
   - **Nome pacchetto Android**: `com.extremecoffee.app`  (deve essere identico)
   - (SHA-1 facoltativo per ora)
3. **Scarica `google-services.json`** e mettilo nella cartella **`app/`** del progetto,
   SOSTITUENDO il segnaposto che trovi già lì.
4. Nel menu a sinistra: **Build → Firestore Database → Crea database** →
   scegli **"Avvia in modalità test"** (regole aperte per 30 giorni: ok per provare).
5. In Android Studio: **File → Sync Project with Gradle Files**, poi ricompila.

Fatto: i telefoni con la stessa app + stesso `google-services.json` ora condividono i dati.

## Cosa funziona cross-device adesso
- **Modalità nuove amicizie (radar)**: il telefono A lancia "un caffè in amicizia" →
  compare nel radar del telefono B → B accetta → A vede B muoversi sulla mappa in tempo reale.
- **Tracking live**: le posizioni dei partecipanti si sincronizzano tra i telefoni.
- I nomi: ognuno imposta il proprio nome nella Home, così vi riconoscete.

## Cosa serve ancora per la CERCHIA RISTRETTA (invito a un amico specifico)
Per far arrivare l'invito a un contatto specifico e svegliargli l'app servono due pezzi
in più (gratuiti), che possiamo fare come prossimo passo:
1. **Registro utenti per numero** (Firebase Phone Auth): salva `users/{id} = { phone, name }`,
   così l'app sa chi è "già su Extreme Coffee".
2. **Cloud Function + Cloud Messaging**: alla creazione dell'invito, manda la notifica push
   che apre il popup del countdown sul telefono dell'amico.
Finché non ci sono, la cerchia ristretta invita via WhatsApp (com'è ora), mentre la
modalità amicizia funziona già cross-device.

## Sicurezza (dopo i test)
La "modalità test" di Firestore scade dopo 30 giorni. Quando vorrai, mettiamo regole
serie (es. lettura/scrittura solo a utenti autenticati) — te le preparo io.
