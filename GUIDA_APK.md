# 📲 Far scaricare l'app agli amici (risolvere il 404)

Il link nel messaggio WhatsApp dà errore 404 perché punta ancora a un segnaposto:
l'APK non è ancora caricato da nessuna parte. Servono 2 cose: (1) mettere il file APK
online, (2) dire all'app qual è il link. Il file dell'app deve caricarlo tu, perché è
legato al tuo account — ma ho reso il passaggio (2) modificabile SENZA ricompilare.

## Passo 1 — Ottieni il file APK
In Android Studio: **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
A fine build clicca "locate": trovi `app-debug.apk` (in `app/build/outputs/apk/debug/`).
Questo è il file da condividere.

## Passo 2 — Mettilo online (scegli UNA via)

### Via A — GitHub Release (consigliata, gratis, link diretto)
1. Crea un account su https://github.com e un repository, es. `extreme-coffee`.
2. Nel repo: scheda **Releases** → **Draft a new release** (o "Create a new release").
3. Metti un tag (es. `v1`), poi **trascina `app-debug.apk`** nell'area "Attach binaries".
4. **Publish release**. Ora il file ha un link pubblico, tipo:
   `https://github.com/TUO-UTENTE/extreme-coffee/releases/download/v1/app-debug.apk`
   (copialo dal tasto destro → "Copia indirizzo link" sul file allegato).

### Via B — Firebase Storage (usi il progetto che hai già)
1. Console Firebase → **Build → Storage** → **Inizia** (modalità test).
2. **Carica file** → seleziona `app-debug.apk`.
3. Clicca il file caricato → copia l'**URL di download** (il link lungo con `token=`).

## Passo 3 — Dì all'app qual è il link (SENZA ricompilare)
1. Console Firebase → **Firestore Database**.
2. **Avvia raccolta** → ID raccolta: `config`.
3. Crea un documento con ID: `app`.
4. Aggiungi un campo: nome `downloadUrl` (tipo *string*), valore = il link copiato al Passo 2.
5. Salva. L'app legge questo link in tempo reale: da ora i messaggi WhatsApp conterranno
   il link giusto, su tutti i telefoni, senza ricompilare nulla.

## Passo 4 — Lato di chi riceve (una volta sola sul suo telefono)
Android blocca le app fuori dal Play Store al primo download:
- Tocca il link → scarica l'APK → aprilo.
- Se appare "Installazione bloccata", tocca **Impostazioni** e attiva
  **"Consenti da questa sorgente"** per il browser/WhatsApp, poi torna indietro e installa.

È normale per le app distribuite fuori dal Play Store.
