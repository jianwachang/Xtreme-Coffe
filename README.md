# ☕ EXTREME COFFEE — Progetto Android (Kotlin + Jetpack Compose)

Gioco social: lanci un caffè con timer (5–30 min), i tuoi amici ricevono il popup,
li vedi arrivare sulla mappa in tempo reale stile Deliveroo. Modalità "nuove amicizie"
con radar futuristico.

## Cosa c'è già nel codice

| Funzione | File | Stato |
|---|---|---|
| Home con i 3 pulsanti principali | `HomeScreen.kt` | ✅ |
| Lancio Extreme Coffee (bar su mappa + timer 5/10/15/20/25/30) | `LaunchCoffeeScreen.kt` | ✅ |
| Popup invito con countdown gigante e "caffè GRATIS" | `InvitePopupScreen.kt` | ✅ |
| Mappa tracking live (marker bar + partecipanti + polyline) | `TrackingScreen.kt` | ✅ |
| Radar animato modalità nuove amicizie | `RadarScreen.kt` | ✅ |
| Invito amici via WhatsApp con messaggio precompilato | `InviteFriendsScreen.kt` | ✅ |
| Backend realtime (eventi + posizioni GPS) | `CoffeeRepository.kt` (Firestore) | ✅ |
| Notifica push "X ha lanciato un extreme coffee!!" | `CoffeeMessagingService.kt` | ✅ lato ricezione |

## Cosa devi configurare TU (gratis, ~30 minuti)

### 1. Android Studio
Scarica Android Studio, poi `File > Open` su questa cartella. Gradle scarica tutto da solo.

### 2. Firebase (backend gratuito, piano Spark)
1. Vai su https://console.firebase.google.com e crea un progetto "ExtremeCoffee"
2. Aggiungi un'app Android con package `com.extremecoffee.app`
3. Scarica `google-services.json` e mettilo nella cartella `app/`
4. Nella console attiva: **Cloud Firestore** (modalità test per iniziare) e **Cloud Messaging**

### 3. Google Maps (chiave gratuita)
1. https://console.cloud.google.com → stesso progetto → abilita "Maps SDK for Android"
2. Crea una API key e incollala in `gradle.properties` alla riga `MAPS_API_KEY=`

### 4. Build dell'APK
`Build > Build APK(s)` → trovi l'APK in `app/build/outputs/apk/debug/`.
Lo mandi agli amici via WhatsApp o lo metti su un link (il link va poi aggiornato
in `InviteFriendsScreen.kt`).

## Cose da sapere (onestà tecnica)

- **WhatsApp**: nessuna app può inviare messaggi automatici a contatti selezionati
  (è una restrizione di WhatsApp, non del codice). Il flusso implementato è quello
  standard: tap su "Invita" → si apre WhatsApp col messaggio già scritto → l'utente
  seleziona lì uno o più contatti e invia. Identico a quello che fanno Revolut, Vinted ecc.
- **Notifiche push automatiche**: per far arrivare il popup agli amici quando lanci
  un caffè serve una piccola **Cloud Function** Firebase che, alla creazione di un
  evento, manda la notifica FCM ai token dei contatti. È un file di ~30 righe lato
  server: chiedimi e te lo scrivo. In alternativa, per i primi test, chi apre l'app
  vede comunque gli eventi attivi dal radar.
- **TODO nel codice**: i punti segnati `// TODO` riguardano l'autenticazione utente
  (Firebase Auth anonima va benissimo: 5 righe) e il nome profilo, ora fissi per il prototipo.
- **Percorso sulla mappa**: ora la linea ospite→bar è diretta; per il percorso stradale
  vero serve la Directions API di Google (a pagamento oltre la soglia gratuita).

## Struttura

```
app/src/main/java/com/extremecoffee/app/
├── MainActivity.kt              # Navigazione + tema scuro caffè/arancio
├── model/CoffeeEvent.kt         # Evento + posizione partecipante
├── data/CoffeeRepository.kt     # Firestore: lancio, accettazione, posizioni live
├── data/LocationTracker.kt      # GPS ogni 5 secondi
├── notifications/CoffeeMessagingService.kt  # Popup push FCM
└── ui/screens/                  # Le 6 schermate
```

---
## Mappe e ricerca luoghi
- Mappa: **Google Maps** (maps-compose).
- Autocompletamento: **Google Places SDK** (ufficiale). La chiave è la stessa di Maps,
  in `gradle.properties` (`MAPS_API_KEY`), letta a runtime dalla meta-data del manifest.

### Da abilitare nella console Google Cloud (sullo stesso progetto della chiave)
1. **Maps SDK for Android** (per la mappa)
2. **Places API** (per l'autocompletamento)  ← nuovo
3. **Account di fatturazione attivo** sul progetto (richiesto da Maps/Places anche con credito gratuito)
Se limiti la chiave per app Android, aggiungi package `com.extremecoffee.app` + SHA-1 di debug.

## Versioni
- AGP 8.6.1 · Gradle 8.7 · Kotlin 2.0.20 · compileSdk 35 · minSdk 26 · Java 17
- Compose BOM 2024.09.03 · maps-compose 6.1.2 · Places 3.5.0 · Firebase BOM 33.4.0
