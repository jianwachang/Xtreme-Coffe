/**
 * Extreme Coffee - Cloud Functions (FCM) con notifiche localizzate (it/en).
 * La lingua di ogni destinatario e' salvata sul documento del token (campo "lang").
 * I token vengono raggruppati per lingua e ogni gruppo riceve il testo tradotto.
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();
setGlobalOptions({ maxInstances: 10 });

const CHANNEL = "extreme_coffee_invites";

function androidConfig(expiresAt) {
  const cfg = { priority: "high", notification: { channelId: CHANNEL } };
  if (expiresAt) cfg.ttl = Math.max(expiresAt - Date.now(), 0);
  return cfg;
}

// "en" solo se esplicitamente inglese, altrimenti italiano (default e token vecchi).
function langOf(t) { return t && t.lang === "en" ? "en" : "it"; }

// ---- Testi localizzati ----
const T = {
  inviteTitle: {
    it: (n) => "\u2615 " + n + " ti invita a un Extreme Coffee!",
    en: (n) => "\u2615 " + n + " invited you to an Extreme Coffee!",
  },
  inviteBody: {
    it: (bar, mins) => (bar ? bar + " \u2022 " : "") + "Hai " + mins + " minuti per arrivare.",
    en: (bar, mins) => (bar ? bar + " \u2022 " : "") + "You have " + mins + " minutes to arrive.",
  },
  respTitle: {
    it: (declined, n) => declined ? "\uD83D\uDE34 " + n + " stavolta passa" : "\u2705 " + n + " sta arrivando!",
    en: (declined, n) => declined ? "\uD83D\uDE34 " + n + " passes this time" : "\u2705 " + n + " is on the way!",
  },
  respBody: {
    it: (declined, n) => declined ? "Niente Extreme Coffee con " + n + " stavolta." : "Preparati: " + n + " \u00e8 in viaggio verso il bar.",
    en: (declined, n) => declined ? "No Extreme Coffee with " + n + " this time." : "Get ready: " + n + " is heading to the caf\u00e9.",
  },
  cancelTitle: {
    it: "\u274C Extreme Coffee annullato",
    en: "\u274C Extreme Coffee cancelled",
  },
  cancelBody: {
    it: (n, bar) => n + " ha annullato l'Extreme Coffee" + (bar ? " da " + bar : "") + ".",
    en: (n, bar) => n + " cancelled the Extreme Coffee" + (bar ? " at " + bar : "") + ".",
  },
};

// Invia una notifica "notification" localizzata, raggruppando i destinatari per lingua.
// recipients: [{ token, lang }]; titles/bodies: { it, en }.
async function sendNotifByLang(recipients, titles, bodies, dataObj, expiresAt, tokensSnap) {
  const groups = { it: [], en: [] };
  recipients.forEach((r) => { (groups[r.lang] || groups.it).push(r.token); });
  for (const lang of ["it", "en"]) {
    const toks = groups[lang];
    if (!toks.length) continue;
    const message = {
      notification: { title: titles[lang], body: bodies[lang] },
      data: { ...dataObj, nav_route: dataObj && dataObj.eventId ? "invite/" + dataObj.eventId : "" },
      android: androidConfig(expiresAt),
      tokens: toks,
    };
    const res = await getMessaging().sendEachForMulticast(message);
    await pruneInvalid(res, toks, tokensSnap);
  }
}

// --- Nuovo evento -> push SOLO agli invitati diretti gia' presenti alla creazione ---
exports.onNewEvent = onDocumentCreated("events/{eventId}", async (event) => {
  const snap = event.data;
  if (!snap) return;
  const data = snap.data() || {};
  const launcherId = data.launcherId || "";
  if (data.simulated === true) return;
  if ((data.mode || "") === "AMICIZIA") return;

  const mins = Number(data.minutes) || 15;
  const createdAt = Number(data.createdAt) || Date.now();
  const expiresAt = createdAt + mins * 60000;
  if (expiresAt <= Date.now()) return;

  const invited = Array.isArray(data.invitedIds) ? data.invitedIds.map(String) : [];
  if (invited.length === 0) return;
  const invitedSet = new Set(invited);

  const tokensSnap = await db.collection("tokens").get();
  const recipients = [];
  tokensSnap.forEach((d) => {
    const t = d.data();
    if (d.id !== launcherId && invitedSet.has(String(d.id)) && t && t.token)
      recipients.push({ token: t.token, lang: langOf(t) });
  });
  if (recipients.length === 0) return;

  const name = data.launcherName || "Qualcuno";
  const bar = data.barName || "";
  await sendNotifByLang(
    recipients,
    { it: T.inviteTitle.it(name), en: T.inviteTitle.en(name) },
    { it: T.inviteBody.it(bar, mins), en: T.inviteBody.en(bar, mins) },
    { eventId: String(event.params.eventId), expiresAt: String(expiresAt) },
    expiresAt, tokensSnap
  );
});

// --- Nuova risposta (accetta/rifiuta) -> push al lanciatore (nella SUA lingua) ---
exports.onNewResponse = onDocumentCreated("responses/{responseId}", async (event) => {
  const snap = event.data;
  if (!snap) return;
  const r = snap.data() || {};
  const launcherId = r.launcherId || "";
  if (!launcherId) return;

  const tokenDoc = await db.collection("tokens").doc(launcherId).get();
  const tdata = tokenDoc.exists ? (tokenDoc.data() || {}) : null;
  const token = tdata ? tdata.token : null;
  if (!token) return;
  const lang = langOf(tdata);

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
  const n = r.fromName || "Qualcuno";
  const message = {
    notification: {
      title: T.respTitle[lang](declined, n),
      body: T.respBody[lang](declined, n),
    },
    data: { eventId: String(r.eventId || ""), expiresAt: String(expiresAt || ""), nav_route: r.eventId ? "launched/" + String(r.eventId) : "" },
    android: androidConfig(expiresAt || null),
    token,
  };
  await getMessaging().send(message).catch(() => {});
});

// --- Evento annullato -> avvisa SOLO gli invitati diretti (data-only, localizzato) ---
exports.onEventCancelled = onDocumentUpdated("events/{eventId}", async (event) => {
  const before = event.data && event.data.before ? event.data.before.data() : null;
  const after = event.data && event.data.after ? event.data.after.data() : null;
  if (!after) return;
  const wasCancelled = !!(before && before.cancelled === true);
  if (wasCancelled || after.cancelled !== true) return;

  const invited = Array.isArray(after.invitedIds) ? after.invitedIds.map(String) : [];
  if (invited.length === 0) return;

  const mins = Number(after.minutes) || 15;
  const createdAt = Number(after.createdAt) || Date.now();
  const expiresAt = createdAt + mins * 60000;

  const tokensSnap = await db.collection("tokens").get();
  const invitedSet = new Set(invited);
  const groups = { it: [], en: [] };
  tokensSnap.forEach((d) => {
    const t = d.data();
    if (invitedSet.has(String(d.id)) && t && t.token)
      (groups[langOf(t)] || groups.it).push(t.token);
  });

  const name = after.launcherName || "Qualcuno";
  const bar = after.barName || "";
  for (const lang of ["it", "en"]) {
    const toks = groups[lang];
    if (!toks.length) continue;
    const message = {
      data: {
        type: "cancelled",
        eventId: String(event.params.eventId),
        title: T.cancelTitle[lang],
        body: T.cancelBody[lang](name, bar),
        expiresAt: String(expiresAt),
        nav_route: "notifications",
      },
      android: { priority: "high", ttl: Math.max(expiresAt - Date.now(), 60000) },
      tokens: toks,
    };
    const res = await getMessaging().sendEachForMulticast(message);
    await pruneInvalid(res, toks, tokensSnap);
  }
});

// --- Invito diretto aggiunto ("Invita") -> push SOLO ai nuovi invitati (localizzato) ---
exports.onInviteAdded = onDocumentUpdated("events/{eventId}", async (event) => {
  const before = event.data && event.data.before ? event.data.before.data() : null;
  const after = event.data && event.data.after ? event.data.after.data() : null;
  if (!after) return;
  if (after.cancelled === true) return;
  if ((after.mode || "") === "AMICIZIA") return;

  const beforeIds = new Set(
    Array.isArray(before && before.invitedIds) ? before.invitedIds.map(String) : []
  );
  const afterIds = Array.isArray(after.invitedIds) ? after.invitedIds.map(String) : [];
  const newIds = afterIds.filter((id) => !beforeIds.has(id));
  if (newIds.length === 0) return;

  const launcherId = after.launcherId || "";
  const mins = Number(after.minutes) || 15;
  const createdAt = Number(after.createdAt) || Date.now();
  const expiresAt = createdAt + mins * 60000;
  if (expiresAt <= Date.now()) return;

  const newSet = new Set(newIds);
  const tokensSnap = await db.collection("tokens").get();
  const recipients = [];
  tokensSnap.forEach((d) => {
    const t = d.data();
    if (d.id !== launcherId && newSet.has(String(d.id)) && t && t.token)
      recipients.push({ token: t.token, lang: langOf(t) });
  });
  if (recipients.length === 0) return;

  const name = after.launcherName || "Qualcuno";
  const bar = after.barName || "";
  await sendNotifByLang(
    recipients,
    { it: T.inviteTitle.it(name), en: T.inviteTitle.en(name) },
    { it: T.inviteBody.it(bar, mins), en: T.inviteBody.en(bar, mins) },
    { eventId: String(event.params.eventId), expiresAt: String(expiresAt) },
    expiresAt, tokensSnap
  );
});

// Rimuove dal registro i token non piu' validi
async function pruneInvalid(res, tokens, tokensSnap) {
  if (!res || !res.responses) return;
  const dead = new Set();
  res.responses.forEach((r, i) => {
    if (!r.success) {
      const code = r.error && r.error.code;
      if (code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-argument") dead.add(tokens[i]);
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
