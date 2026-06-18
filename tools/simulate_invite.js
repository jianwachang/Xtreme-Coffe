const admin = require("firebase-admin");
const fs = require("fs");

function finish(out) {
  fs.writeFileSync("sim-result.json", JSON.stringify(out, null, 2));
  console.log(JSON.stringify(out, null, 2));
}

(async () => {
  const out = { status: "INIT" };
  try {
    const sa = JSON.parse(process.env.SA_JSON);
    admin.initializeApp({ credential: admin.credential.cert(sa) });
    const db = admin.firestore();
    const msg = admin.messaging();

    // 1) Leggo i dispositivi registrati (token FCM)
    const tokensSnap = await db.collection("tokens").get();
    const devices = [];
    tokensSnap.forEach((d) => {
      const t = d.data() || {};
      if (t.token) devices.push({ id: d.id, name: t.name || "(senza nome)", token: t.token });
    });
    out.devicesFound = devices.map((x) => ({ id: x.id, name: x.name, tokenPreview: x.token.slice(0, 10) + "…" }));

    if (devices.length === 0) {
      out.status = "NO_TOKENS";
      out.hint = "Nessun token registrato: apri l'app almeno una volta concedendo le notifiche, poi riprova.";
      return finish(out);
    }

    // 2) Creo un Extreme Coffee reale (demo) in modalità AMICIZIA (niente broadcast automatico)
    const now = Date.now();
    const minutes = 15;
    const eventId = "sim_" + now;
    const invitedIds = devices.map((d) => d.id); // così compare in "Hai ricevuto" nell'app
    const event = {
      id: eventId,
      launcherId: "extreme_demo_bot",
      launcherName: "Extreme Coffee (demo)",
      launcherPhoto: "",
      barName: "Bar Centrale (demo)",
      barLat: 45.4642,
      barLng: 9.19,
      minutes: minutes,
      createdAt: now,
      mode: "AMICIZIA",
      acceptedCount: 0,
      invitedIds: invitedIds,
      cancelled: false,
    };
    await db.collection("events").doc(eventId).set(event);
    out.eventId = eventId;

    // 3) Invio il push MIRATO solo ai token registrati (di norma il/i tuoi dispositivi)
    const expiresAt = now + minutes * 60000;
    const tokens = devices.map((d) => d.token);
    const res = await msg.sendEachForMulticast({
      notification: {
        title: "\u2615 Extreme Coffee (demo) ti invita!",
        body: "Bar Centrale \u2022 hai 15 minuti per arrivare.",
      },
      data: { eventId: String(eventId), expiresAt: String(expiresAt) },
      android: { priority: "high", notification: { channelId: "extreme_coffee_invites" }, ttl: minutes * 60000 },
      tokens: tokens,
    });
    out.sent = res.successCount;
    out.failed = res.failureCount;
    out.perToken = res.responses.map((r, i) =>
      ({ name: devices[i].name, ok: r.success, error: r.success ? null : (r.error && r.error.code) }));
    out.status = "DONE";
  } catch (e) {
    out.status = "ERROR";
    out.error = String((e && e.message) || e);
  }
  finish(out);
})();
