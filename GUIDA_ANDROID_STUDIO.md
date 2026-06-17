# 🛠️ Usare il progetto in Android Studio

Il progetto si apre, sincronizza e compila **senza alcuna chiave**.
Le mappe ora usano **OpenStreetMap** (libreria osmdroid): gratuite, open, senza account.

## Aggiornare il progetto che hai già aperto
Hai già una versione aperta in Android Studio. Per usare questa nuova:
1. Chiudi Android Studio.
2. Estrai questo nuovo zip e **sostituisci** la cartella del progetto (oppure copia dentro
   i file aggiornati sovrascrivendo i vecchi).
3. Riapri la cartella in Android Studio.
4. Menu **File → Sync Project with Gradle Files** (sono cambiate le librerie: è stata
   tolta Google Maps e aggiunta osmdroid).

## Compilare l'APK
- **Build → Build App Bundle(s) / APK(s) → Build APK(s)**
- A fine build, notifica in basso a destra → **locate** → trovi
  `app/build/outputs/apk/debug/app-debug.apk`
- Mandalo al telefono e installalo (consenti "installa da questa fonte").

## Cosa funziona subito, senza configurare niente
- Tutte le schermate e la navigazione (tasti **indietro ←**, **avanti →**, **home ⌂**).
- Le **mappe vere** (OpenStreetMap): scegli il bar toccando la mappa, vedi i marker e i percorsi.
- Timer 5 / 10 / 15 / 20 / 25 minuti.

## Cosa resta opzionale (solo per il backend condiviso tra telefoni diversi)
**Firebase** serve solo per far parlare tra loro i telefoni in tempo reale
(eventi, posizioni live, notifiche push). Senza, l'app gira lo stesso sul singolo telefono.
Per attivarlo: console.firebase.google.com → progetto → app Android con package
`com.extremecoffee.app` → scarica il vero `google-services.json` e sostituiscilo in `app/`
→ attiva Firestore e Cloud Messaging → ri-sincronizza.

## Problemi comuni
- *Sync lento la prima volta*: normale, scarica librerie e SDK.
- *Barra gialla "Install missing SDK"*: clicca e accetta.
- *Mappa un po' lenta a caricare le tile*: è OpenStreetMap, le tile arrivano da internet
  e vengono messe in cache; al secondo avvio è più veloce.
