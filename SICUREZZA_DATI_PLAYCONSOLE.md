# Sicurezza dei dati (Play Console) — Extreme Coffee

Guida operativa per compilare il modulo **Sicurezza dei dati** in Play Console
(Contenuti app → Sicurezza dei dati). Le scelte qui sotto rispecchiano il
comportamento reale dell'app (verificato su manifest, SDK e scritture Firebase),
così da evitare discrepanze — che Google sanziona.

> Nota: le app **solo su test interno sono esenti** dal modulo. Diventa obbligatorio
> quando pubblichi su test chiuso/aperto o in produzione. Conviene averlo già pronto.

---

## A. Cosa NON usa l'app (importante, semplifica tutto)
- **Nessun** SDK di analytics, Crashlytics, pubblicità (AdMob) o tracciamento di terze parti.
- **Nessuna condivisione** di dati con terze parti: Firebase/Google (Firestore, FCM,
  Auth, Maps/Places/Location) agiscono come **fornitori di servizi**, non come terze parti.
- Le **foto** delle Selfie Coffee restano sul dispositivo; la condivisione è un'azione
  avviata dall'utente → **non** si dichiara.

## B. Risposte alle domande iniziali
| Domanda | Risposta |
|---|---|
| La tua app raccoglie o condivide dati utente richiesti? | **Sì** (raccoglie) |
| Tutti i dati raccolti sono **criptati in transito**? | **Sì** (tutto via HTTPS/TLS di Firebase/Google) |
| Consenti agli utenti di **richiedere l'eliminazione** dei dati? | **Sì** (in-app: Account → Elimina account; e pagina web `www.extremecoffee.it/elimina-account`) |
| Condividi dati con terze parti? | **No** |

## C. Tipi di dati da selezionare (Raccolti = Sì, Condivisi = No per tutti)

| Tipo di dati | Perché (nel codice) | Storage | Obblig./Facolt. | Finalità | Effimero? |
|---|---|---|---|---|---|
| **Posizione → Posizione esatta** | posizione live del partecipante verso il bar + coordinate del bar salvate negli `events`/`events/{id}/locations` (ACCESS_FINE_LOCATION) | Trasmessa e salvata | **Facoltativo** (permesso) | Funzionalità dell'app | No |
| **Info personali → Nome** | soprannome salvato in `users`/`nicknames` | Salvato | **Obbligatorio** | Funzionalità dell'app; Gestione dell'account | No |
| **Info personali → Numero di telefono** | Firebase Auth (telefono) + `users/{phone}` | Salvato | **Obbligatorio** | Funzionalità dell'app; Gestione dell'account | No |
| **Info personali → ID utente** | id account/dispositivo (`launcherId`/`fromId`) | Salvato | **Obbligatorio** | Funzionalità dell'app; Gestione dell'account | No |
| **Contatti → Contatti** | i numeri della rubrica vengono inviati a Firestore solo per trovare gli amici già registrati (query `whereIn`), non vengono archiviati | Solo in memoria | **Facoltativo** (permesso) | Funzionalità dell'app | **Sì (effimero)** |
| **ID dispositivo o altri ID** | token FCM salvato in `tokens/{id}` + Firebase Installation ID | Salvato | **Obbligatorio** | Funzionalità dell'app; Comunicazioni dello sviluppatore (notifiche push) | No |

### Note sulle scelte
- **Posizione esatta** marcata *facoltativa* perché l'utente può negare il permesso e
  usare comunque l'app (con funzioni ridotte). Se preferisci essere ancora più prudente,
  puoi aggiungere anche **Posizione approssimativa** (COARSE) con le stesse risposte.
- **Contatti = effimero**: i numeri servono solo a trovare gli amici in tempo reale e non
  vengono salvati sui nostri server. In quanto effimero, va indicato nel modulo ma **non**
  comparirà nella scheda pubblica dello Store.
- **ID dispositivo/altri ID**: include la finalità *Comunicazioni dello sviluppatore*
  perché il token serve a recapitare gli inviti tramite notifica push.
- **Email**: l'app **non** raccoglie email (l'email del form tester è raccolta dal **sito**,
  non dall'app: non va nel modulo Sicurezza dei dati, è coperta dalla Privacy Policy).

## D. Da NON selezionare (non applicabile)
Email, Indirizzo, Foto/Video, Audio, File/Documenti, Calendario, Messaggi (SMS/email/in-app),
Cronologia navigazione web, Info finanziarie, Salute/Fitness, Log arresti anomali/Diagnostica
(nessun SDK di analytics/crash), Cronologia ricerche in-app*, App installate.

\* *Cronologia ricerche in-app*: la ricerca del bar passa dal **Places SDK** di Google
(fornitore di servizi) ed è effimera/di funzionalità. Non è necessario dichiararla; se vuoi
essere massimamente prudente puoi aggiungerla come *raccolta + effimera*, finalità Funzionalità.

## E. Misure di sicurezza (badge)
- **Crittografia in transito**: Sì.
- **Meccanismo di richiesta eliminazione**: Sì (in-app + pagina web).

## F. Altri requisiti Google collegati (già sistemati)
1. **Privacy policy** presente e collegata: `www.extremecoffee.it/privacy`.
2. **Eliminazione account**: in-app (Account → Elimina account) + **URL web**
   `www.extremecoffee.it/elimina-account` → da inserire in Play Console:
   *Contenuti app → Eliminazione dei dati → fornisci l'URL*.
   L'eliminazione ora rimuove profilo, nickname, token, **eventi e risposte** dell'utente.
3. **Autorizzazioni**: INTERNET, ACCESS_NETWORK_STATE, ACCESS_FINE/COARSE_LOCATION,
   READ_CONTACTS, POST_NOTIFICATIONS, CAMERA, WRITE_EXTERNAL_STORAGE. Sono coerenti con le
   funzioni dichiarate. `WRITE_EXTERNAL_STORAGE` è già correttamente limitato con
   `android:maxSdkVersion="28"` (non richiesto su Android 10+): nessuna azione necessaria.

## G. Dopo l'invio
Google esamina in fase di revisione dell'app; le info compaiono nella scheda dello Store.
Aggiorna il modulo se cambi le pratiche sui dati.
