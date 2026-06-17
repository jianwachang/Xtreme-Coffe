/**
 * Extreme Coffee - Cloud Functions (FCM)
 * Inviano le notifiche push anche ad app CHIUSA:
 *  - nuovo evento   -> avvisa tutti gli altri dispositivi con l'app
 *  - nuova risposta -> avvisa chi ha lanciato il caffè
 *
 * Le notifiche portano la scadenza dell'Extreme Coffee (expiresAt) e un ttl:
 *  - ttl: se il dispositivo è offline e torna online DOPO la scadenza, FCM non recapita più l'invito.
 *  - expiresAt: l'app, quando costruisce la notifica, la fa sparire da sola alla scadenza.
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();
setGlobalOptions({ maxInstances: 10 });

const CHANNEL = "extreme_coffee_invites";

// Costruisce la config Android con priorità alta + ttl fino alla scadenza (in ms).
function androidConfig(expiresAt) {
  const cfg = { priority: "high", notification: { channelId: CHANNEL } };
  if (expiresAt) {
    const ttl = Math.max(expiresAt - Date.now(), 0);
    cfg.ttl = ttl; // millisecondi
  }
  return cfg;
}

// --- Nuovo evento -> push a tutti tranne il lanciatore ---
exports.onNewEvent = onDocumentCreated("events/{eventId}", async (event) => {
  const snap = event.data;
  if (!snap) return;
  const data = snap.data() || {};
  const launcherId = data.launcherId || "";

  // AMICIZIA: nessuna notifica broadcast. L'evento si scopre dal radar;
  // il lanciatore verra' avvisato solo quando qualcuno tocca il caffe' e conferma di andare (onNewResponse).
  if ((data.mode || "") === "AMICIZIA") return;

  const mins = Number(data.minutes) || 15;
  const createdAt = Number(data.createdAt) || Date.now();
  const expiresAt = createdAt + mins * 60000;
  if (expiresAt <= Date.now()) return; // evento già scaduto: non avvisare nessuno

  const tokensSnap = await db.collection("tokens").get();
  const tokens = [];
  tokensSnap.forEach((d) => {
    const t = d.data();
    if (d.id !== launcherId && t && t.token) tokens.push(t.token);
  });
  if (tokens.length === 0) return;

  const message = {
    notification: {
      title: "\u2615 " + (data.launcherName || "Qualcuno") + " ti invita a un Extreme Coffee!",
      body: (data.barName ? data.barName + " \u2022 " : "") +
            "Hai " + mins + " minuti per arrivare.",
    },
    data: {
      eventId: String(event.params.eventId),
      expiresAt: String(expiresAt),
    },
    android: androidConfig(expiresAt),
    tokens,
  };

  const res = await getMessaging().sendEachForMulticast(message);
  await pruneInvalid(res, tokens, tokensSnap);
});

// --- Nuova risposta (accetta/rifiuta) -> push al lanciatore ---
exports.onNewResponse = onDocumentCreated("responses/{responseId}", async (event) => {
  const snap = event.data;
  if (!snap) return;
  const r = snap.data() || {};
  const launcherId = r.launcherId || "";
  if (!launcherId) return;

  const tokenDoc = await db.collection("tokens").doc(launcherId).get();
  const token = tokenDoc.exists ? (tokenDoc.data() || {}).token : null;
  if (!token) return;

  // Recupera l'evento per legare la notifica alla scadenza del caffè
  let expiresAt = 0;
  if (r.eventId) {
    const evSnap = await db.collection("events").doc(String(r.eventId)).get();
    if (evSnap.exists) {
      const ev = evSnap.data() || {};
      const mins = Number(ev.minutes) || 15;
      const createdAt = Number(ev.createdAt) || Date.now();
      expiresAt = createdAt + mins * 60000;
    }
  }

  const declined = r.status === "declined";
  const message = {
    notification: {
      title: declined
        ? "\uD83D\uDE34 " + (r.fromName || "Qualcuno") + " stavolta passa"
        : "\u2705 " + (r.fromName || "Qualcuno") + " sta arrivando!",
      body: declined
        ? "Niente Extreme Coffee con " + (r.fromName || "lui") + " stavolta."
        : "Preparati: " + (r.fromName || "qualcuno") + " \u00e8 in viaggio verso il bar.",
    },
    data: {
      eventId: String(r.eventId || ""),
      expiresAt: String(expiresAt || ""),
    },
    android: androidConfig(expiresAt || null),
    token,
  };

  await getMessaging().send(message).catch(() => {});
});

// Rimuove dal registro i token non più validi (app disinstallata, ecc.)
async function pruneInvalid(res, tokens, tokensSnap) {
  if (!res || !res.responses) return;
  const dead = new Set();
  res.responses.forEach((r, i) => {
    if (!r.success) {
      const code = r.error && r.error.code;
      if (code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-argument") {
        dead.add(tokens[i]);
      }
    }
  });
  if (dead.size === 0) return;
  const batch = db.batch();
  tokensSnap.forEach((d) => {
    const t = d.data();
    if (t && dead.has(t.token)) batch.delete(d.ref);
  });
  await batch.commit().catch(() => {});
}
