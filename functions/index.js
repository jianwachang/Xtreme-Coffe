/**
 * Extreme Coffee - Cloud Functions (FCM) con notifiche localizzate (it/en).
 * La lingua di ogni destinatario e' salvata sul documento del token (campo "lang").
 * I token vengono raggruppati per lingua e ogni gruppo riceve il testo tradotto.
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { defineSecret } = require("firebase-functions/params");
const { google } = require("googleapis");

// Config automazione tester (Cloud Identity Free + Admin SDK Directory).
// Se non impostati, la funzione ricade sulla sola notifica push (nessun errore).
const GWS_SA_KEY = defineSecret("GWS_SA_KEY");   // JSON del service account con delega
const GWS_ADMIN_EMAIL = process.env.GWS_ADMIN_EMAIL || "";   // es. admin@extremecoffee.it
const GWS_TESTERS_GROUP = process.env.GWS_TESTERS_GROUP || ""; // es. testers@extremecoffee.it

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

// =====================================================================
//  Richiesta accesso ai test interni dal sito web
//  Il sito invia l'email del tester -> la salviamo in "testerRequests"
//  e mandiamo una push a Dario con l'email da aggiungere alla Play Console.
// =====================================================================

const TESTER_EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Endpoint chiamato dal sito (fetch POST { email }). CORS abilitato.
exports.requestTesterAccess = onRequest({ cors: true }, async (req, res) => {
  try {
    if (req.method !== "POST") {
      res.status(405).json({ ok: false, error: "method_not_allowed" });
      return;
    }
    const body = req.body || {};
    const email = String(body.email || "").trim().toLowerCase();
    if (!TESTER_EMAIL_RE.test(email) || email.length > 254) {
      res.status(400).json({ ok: false, error: "invalid_email" });
      return;
    }
    // Deduplica: se l'email ha gia' richiesto, non ricreare
    const existing = await db.collection("testerRequests")
      .where("email", "==", email).limit(1).get();
    if (existing.empty) {
      await db.collection("testerRequests").add({
        email,
        status: "pending",           // pending -> added (lo aggiorni tu quando l'hai inserita)
        source: "website",
        userAgent: String(req.get("user-agent") || "").slice(0, 300),
        createdAt: Date.now()
      });
    }
    res.status(200).json({ ok: true });
  } catch (e) {
    console.error("requestTesterAccess error", e);
    res.status(500).json({ ok: false, error: "server_error" });
  }
});

// Trova i token push di Dario (utente con numero che termina 3935672548).
async function findAdminTokens() {
  const tokens = [];
  const users = await db.collection("users").get();
  const ids = [];
  users.forEach((d) => {
    const x = d.data() || {};
    const digits = String(x.phone || "").replace(/\D/g, "");
    const nm = String(x.name || x.nickname || "").trim().toLowerCase();
    if (digits.endsWith("3935672548") || nm === "dario") ids.push(x.id || d.id);
  });
  for (const id of ids) {
    const t = await db.collection("tokens").doc(id).get();
    if (t.exists && t.data() && t.data().token) tokens.push(t.data().token);
  }
  return tokens;
}

// Aggiunge un'email come MEMBRO del Google Gruppo dei tester, via Admin SDK
// Directory API (delega a livello di dominio su Cloud Identity Free).
// Ritorna: {added} | {already} | {skipped} | {error}
async function addToTestersGroup(email) {
  if (!GWS_TESTERS_GROUP || !GWS_ADMIN_EMAIL) return { skipped: "config_mancante" };
  let key;
  try { key = JSON.parse(GWS_SA_KEY.value() || "{}"); }
  catch (e) { return { skipped: "chiave_assente" }; }
  if (!key.client_email || !key.private_key) return { skipped: "chiave_invalida" };

  const auth = new google.auth.JWT({
    email: key.client_email,
    key: key.private_key,
    scopes: ["https://www.googleapis.com/auth/admin.directory.group.member"],
    subject: GWS_ADMIN_EMAIL   // impersona un super-admin del dominio
  });
  const directory = google.admin({ version: "directory_v1", auth });
  try {
    await directory.members.insert({
      groupKey: GWS_TESTERS_GROUP,
      requestBody: { email, role: "MEMBER" }
    });
    return { added: true };
  } catch (e) {
    if (e && e.code === 409) return { already: true }; // gia' membro
    console.error("addToTestersGroup error", e && e.errors ? e.errors : e);
    return { error: (e && e.message) || "unknown" };
  }
}

// Alla nuova richiesta -> prova ad aggiungere al gruppo tester, poi notifica push a Dario.
exports.onTesterRequest = onDocumentCreated(
  { document: "testerRequests/{id}", secrets: [GWS_SA_KEY] },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const data = snap.data() || {};
    const email = data.email || "";

    // 1) Automazione: aggiunta al Google Gruppo (se configurata)
    const result = await addToTestersGroup(email);
    const auto = result.added || result.already;
    await snap.ref.set({
      status: auto ? "added" : (result.error ? "error" : "pending"),
      autoResult: result
    }, { merge: true });

    // 2) Notifica push a Dario (conferma automatica o richiesta manuale)
    const tokens = await findAdminTokens();
    if (tokens.length === 0) {
      console.log("Tester", email, "->", JSON.stringify(result));
      return;
    }
    const body = auto
      ? email + " \u2014 aggiunto automaticamente ai tester \u2705"
      : email + " \u2014 aggiungilo ai tester interni nella Play Console";
    const res = await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title: "\uD83D\uDCE5 Nuovo tester Extreme Coffee", body },
      data: { type: "tester_request", email: String(email), auto: String(!!auto) },
      android: androidConfig()
    });
    await pruneInvalid(res, tokens, await db.collection("tokens").get());
  }
);
