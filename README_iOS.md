# Extreme Coffee — iOS (SwiftUI)

Versione iOS nativa dell'app **Extreme Coffee** (brand Unlimited Vision), mirror fedele
del progetto Android (Kotlin/Compose). Stessa palette crema/arancio, stesso modello dati
(`CoffeeEvent`), stessa struttura a 5 tab e lo stesso flusso: Registrazione → Home →
Lancia caffè → Invito/Stato → Selfie Coffee.

Oggi gira con **repository in-memory** (dati demo già dentro), esattamente come è nato
l'Android: l'app è navigabile subito nel Simulatore, e le interfacce sono pronte per
essere collegate a Firebase senza toccare la UI.

## Cosa serve (prerequisiti)

- Un **Mac con Xcode 15+** (la compilazione iOS richiede macOS: non è possibile su Linux/Windows).
- **XcodeGen** per generare il progetto: `brew install xcodegen`.
- Per pubblicare su TestFlight/App Store: iscrizione all'**Apple Developer Program** (99 €/anno).

## Aprire e provare (Simulatore)

```bash
cd ExtremeCoffee-iOS
xcodegen generate          # crea ExtremeCoffee.xcodeproj dal project.yml
open ExtremeCoffee.xcodeproj
# in Xcode: seleziona un simulatore iPhone e premi Run (⌘R)
```

L'app parte sulla **Registrazione**; dopo vedrai Home con un invito demo da "Marco"
(Bar Luce), il Radar con un caffè in "Amicizia", classifica, notifiche e account.

## Struttura

```
ExtremeCoffee/
  App/            ExtremeCoffeeApp.swift (entry, @main), AppState (stato globale)
  Model/          CoffeeEvent.swift  (mirror di model/CoffeeEvent.kt)
  Data/           CoffeeRepository.swift (protocollo + in-memory, Firebase-ready)
                  Profile.swift (UserDefaults), Config.swift
  Theme/          Theme.swift (palette/tipografia esatte), BrandLogo.swift (logo vettoriale)
  UI/             RootView.swift (5 tab) + Screens/ (Register, Home, LaunchCoffee,
                  InvitePopup, LauncherStatus, SelfieCoffee, Radar/Notifiche/Classifica/Account/Badge)
```

## Regola brand (rispettata)

Il logo è la **tazza che fa l'occhiolino** con cuore nel latte, swoosh arancio e linee
di velocità (`BrandLogo.swift`, vettoriale). L'**icona app** va esportata da questo logo
**su sfondo crema con cornice scura** — mai sfondo scuro. In Xcode: `Assets.xcassets` →
`AppIcon` → inserisci il PNG 1024×1024 generato con `BrandLogo(framed: true)` su crema.

## Collegare Firebase (quando vuoi i dati reali + push)

1. Firebase Console → progetto **extreme-coffe** → *Aggiungi app* → **iOS**,
   bundle id `com.extremecoffee.myapp`. Scarica **GoogleService-Info.plist** e trascinalo
   in `ExtremeCoffee/Resources/` (aggiungilo al target).
2. In `project.yml` togli i commenti al blocco `packages:` (Firebase) e alle `dependencies`
   del target, poi rigenera con `xcodegen generate`.
3. In `ExtremeCoffeeApp.swift`, in `init()`, abilita `FirebaseApp.configure()`.
4. Sostituisci `InMemoryCoffeeRepository` con una `FirebaseCoffeeRepository` che implementa
   lo stesso `CoffeeRepositoryProtocol` (Firestore per `events`/`responses`/`users`).
5. **Push (APNs)**: crea una chiave APNs sull'Apple Developer, caricala su Firebase Cloud
   Messaging, aggiungi la capability *Push Notifications* + *Background Modes → Remote
   notifications*. La Cloud Function `onNewEvent` già esistente invierà le notifiche anche a iOS.

## Pubblicazione

- In Xcode imposta **Signing & Capabilities** con il tuo team (o `DEVELOPMENT_TEAM` nel `project.yml`).
- `Product → Archive` → `Distribute App → App Store Connect` per TestFlight.
- La CI in `.github/workflows/ios.yml` compila già per Simulatore a ogni push (validazione);
  per l'`.ipa` firmato servono i secret di firma (vedi note nel workflow).

## Nota tecnica

Fotocamera reale, Places autocomplete e mappa/percorso sono presenti come UI (MapKit per la
mappa); i pezzi che richiedono un device fisico o chiavi (fotocamera, Places) sono segnalati
nei rispettivi file e vanno collegati come sull'Android. Ogni modifica futura va fatta
verificando che le funzioni esistenti non si rompano (stessa regola del progetto Android).
