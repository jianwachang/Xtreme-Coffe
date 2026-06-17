# Stato attuale dell'app

## Cosa funziona, sul telefono, senza configurare niente
- Tutte le schermate e la navigazione (indietro / avanti / home).
- **Google Maps** con la tua chiave (in `gradle.properties`).
- **Autocompletamento Google Places** sulla ricerca del bar.
- Posizione attuale (pallino), marker a caffè sul punto scelto, percorsi nel tracking.
- Lancio di un Extreme Coffee → schermata countdown → rilancio a timer scaduto.
- Timer 5/10/15/20/25, modalità cerchia/amicizia, radar, inviti WhatsApp.

## Importante
- **Firebase è stato rimosso** per ora: era la causa del crash al lancio (scriveva su un
  backend non configurato). L'app gira tutta **in locale sul singolo telefono**: perfetta
  per provare l'intero flusso, ma i telefoni non si sincronizzano ancora tra loro.
- Per far comunicare più dispositivi in tempo reale (invito che arriva all'amico,
  posizioni live tra telefoni diversi, notifiche push) servirà reintrodurre un backend
  con un progetto Firebase reale: è il prossimo passo, quando vuoi lo facciamo insieme.

## Perché la mappa/ricerca funzionino davvero (console Google Cloud, stessa chiave)
1. Abilita **Maps SDK for Android**
2. Abilita **Places API**
3. **Account di fatturazione attivo** sul progetto
4. Se limiti la chiave: package `com.extremecoffee.app` + SHA-1 di debug

## Logo
Tazzina che "impenna" inclinata in obliquo, manico in basso a sinistra, con stanghette
orizzontali a dare il senso della velocità.
