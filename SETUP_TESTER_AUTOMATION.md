# Automazione tester (gratis) — Cloud Identity Free + Admin SDK

Obiettivo: quando un utente inserisce l'email sul sito, viene **aggiunto in automatico**
come membro del Google Gruppo dei tester → può scaricare dal link del test interno.
Tutto gratuito: usa **Cloud Identity Free** sul dominio `extremecoffee.it`.

Il codice è già nel repo (`functions/index.js` → `onTesterRequest` chiama `addToTestersGroup`).
Se i parametri qui sotto non sono impostati, il sistema ricade sulla sola notifica push
(niente errori). Una volta completati i passi, l'aggiunta diventa automatica.

---

## 1) Attiva Cloud Identity Free sul dominio (gratis)
1. Vai su https://workspace.google.com/gcpidentity/signup (Cloud Identity **Free**).
2. Inserisci il dominio **extremecoffee.it** e crea l'utente amministratore, es.
   **admin@extremecoffee.it** (sarà il tuo Super Admin).
3. Verifica il dominio aggiungendo il record **TXT** indicato nella tua zona DNS GoDaddy.
4. Entri così nella **Admin Console** (admin.google.com) come super amministratore.

## 2) Crea il gruppo tester
1. Admin Console → **Directory → Gruppi → Crea gruppo**.
2. Email gruppo: **testers@extremecoffee.it** (nome a piacere).
3. Impostazioni di accesso: lascia pure che solo gli admin possano aggiungere membri
   (li aggiunge la funzione via API).

## 3) Imposta il gruppo come lista tester su Play Console
1. Play Console → la tua app → **Test → Test interno → Tester**.
2. Scegli **Google Gruppi** e inserisci **testers@extremecoffee.it** → Salva.
   > D'ora in poi, chi è nel gruppo è automaticamente un tester interno.

## 4) Service account + delega a livello di dominio
1. Google Cloud Console (stesso account) → progetto **extreme-coffe** →
   **API e servizi → Abilita API**: abilita **Admin SDK API**.
2. **IAM e amministrazione → Account di servizio → Crea**: es. `tester-adder`.
   Crea una **chiave JSON** e scaricala (serve al passo 5).
3. Copia il **Client ID numerico** dell'account di servizio (schermata Dettagli).
4. Admin Console → **Sicurezza → Controllo accessi e dati → Controlli API →
   Delega a livello di dominio → Aggiungi**:
   - Client ID: quello copiato;
   - Ambiti OAuth: `https://www.googleapis.com/auth/admin.directory.group.member`
5. Admin Console → **Ruoli amministratore**: assegna all'account di servizio
   (o all'admin che impersona) il ruolo **Amministratore gruppi**.

## 5) Configura Firebase Functions e fai il deploy
Dalla cartella del repo:
```bash
# la chiave JSON del service account come SECRET
firebase functions:secrets:set GWS_SA_KEY
#   ...incolla tutto il contenuto del file JSON quando richiesto

# parametri non segreti in functions/.env
printf 'GWS_ADMIN_EMAIL=admin@extremecoffee.it\nGWS_TESTERS_GROUP=testers@extremecoffee.it\n' >> functions/.env

# installa la nuova dipendenza e fai il deploy
cd functions && npm install && cd ..
firebase deploy --only functions
```

## 6) Fatto
Da ora, quando qualcuno inserisce l'email sul sito:
1. il sito chiama `requestTesterAccess` → salva la richiesta;
2. `onTesterRequest` **aggiunge l'email al gruppo** via Admin SDK;
3. ricevi una push *"✅ aggiunto automaticamente ai tester"*;
4. l'utente apre il link del test interno e scarica (Google può metterci qualche
   minuto a propagare l'abilitazione).

### Note
- Gli indirizzi @gmail dei tester sono semplici **membri del gruppo**: non consumano
  licenze Cloud Identity, quindi restano gratis.
- Se un'email è già membro, l'API risponde 409 e la trattiamo come "già presente".
- Finché non completi i passi 1-5, tutto continua a funzionare in modalità
  semi-automatica (push con l'email da aggiungere a mano).
