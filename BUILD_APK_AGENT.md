# Agente "Build APK debug" — come funziona

Hai due modi per creare l'APK debug. Il **primo è l'agente automatico** (consigliato): compila e pubblica l'APK da solo, non fai più niente a mano.

---

## 1) Agente automatico (GitHub Actions) — consigliato

Una volta configurato, ogni volta che **carichi il codice su GitHub** (push), parte da solo un "robot" che:
1. scarica il codice,
2. installa Java 17 + Android SDK,
3. compila l'APK debug,
4. lo **pubblica nella release del repo** come `app-debug.apk`.

Così il link già usato dall'app
`https://github.com/jianwachang/Xtreme-Coffe/releases/latest/download/app-debug.apk`
punta **sempre all'ultima build**, senza che tu carichi niente.

### Configurazione (si fa UNA volta sola)

1. **Metti il progetto su GitHub.**
   Il repository deve contenere **tutto il progetto** (questa cartella), non solo l'APK.
   Da Android Studio: menu **Git → GitHub → Share Project on GitHub** (oppure usa GitHub Desktop).
   Assicurati che vengano caricati anche `gradle.properties` e `app/google-services.json` (ci sono già: servono alla build).

2. **Dai i permessi al robot di pubblicare la release.**
   Sul sito del repo: **Settings → Actions → General → Workflow permissions →** seleziona **"Read and write permissions"** → **Save**.

3. (Facoltativo) **Chiave Maps come "secret"** invece che dentro `gradle.properties`:
   **Settings → Secrets and variables → Actions → New repository secret**
   - Name: `MAPS_API_KEY`
   - Secret: la tua chiave
   Se non lo fai, l'agente usa la chiave già presente in `gradle.properties` (funziona comunque).

### Uso quotidiano
- **Automatico:** fai push del codice → tab **Actions** del repo → vedi la build "Build debug APK" che gira → a fine corsa l'APK è:
  1. nella **release** (link `.../releases/latest/download/app-debug.apk`, usato dall'app per l'auto-aggiornamento);
  2. nel branch **`apk`**, scaricabile via:
     `https://raw.githubusercontent.com/<owner>/<repo>/apk/app-debug.apk`
- **A mano:** tab **Actions → Build debug APK → Run workflow**.
- Puoi anche scaricare l'APK direttamente dalla pagina della run (sezione **Artifacts → app-debug**).

### Come l'APK arriva QUI in chat (automatico lato mio)
Questo ambiente non può compilare l'APK, ma **può scaricare i file dal branch `apk`** (raw.githubusercontent è raggiungibile).
Quindi, dopo che il robot ha fatto una build, basta che tu mi dica "dammi l'apk" (oppure lo faccio dopo ogni modifica) e io eseguo:
`curl -sSL https://raw.githubusercontent.com/<owner>/<repo>/apk/app-debug.apk -o app-debug.apk` e te lo consegno in chat.
Nessuna compilazione né upload manuale da parte tua: il robot compila, io recupero e ti porto il file.

> Nota sicurezza: l'APK debug è pubblico e contiene la chiave Maps. Conviene **limitare la chiave** nella Google Cloud Console (per package + firma) così non è usabile altrove.

---

## 2) Build locale "1 click" (alternativa)

Se preferisci compilare sul tuo PC (hai già Android Studio):
- doppio click su **`build_apk.bat`** dentro la cartella del progetto.
- A fine compilazione si apre la cartella con l'APK:
  `app\build\outputs\apk\debug\app-debug.apk`

Poi, se vuoi distribuirlo, carichi tu quell'APK nella release di GitHub (questo passaggio lo automatizza l'agente del punto 1).

---

## Quando passerai al Play Store
Non serve toccare l'agente: cambierà solo il **link di download**.
Imposta su Firestore il documento `config/app` campo `downloadUrl` con il link dello Store:
l'app userà quello al posto dell'APK, senza ricompilare.
