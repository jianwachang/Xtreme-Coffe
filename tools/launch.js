const admin = require("firebase-admin");
const fs = require("fs");
function finish(o){ fs.writeFileSync("op-result.json", JSON.stringify(o,null,2)); console.log(JSON.stringify(o,null,2)); }
(async () => {
  const out = { action: "launch_cerchia" };
  try {
    admin.initializeApp({ credential: admin.credential.cert(JSON.parse(process.env.SA_JSON)) });
    const db = admin.firestore();
    const msg = admin.messaging();
    // SOLO i token con nome "Dario"
    const toks = await db.collection("tokens").get();
    const dario = [];
    toks.forEach(d => { const t = d.data() || {}; if ((t.name || "") === "Dario" && t.token) dario.push({ id: d.id, token: t.token }); });
    out.targets = dario.map(x => ({ id: x.id, tokenPreview: x.token.slice(0, 10) + "\u2026" }));
    if (dario.length === 0) { out.status = "NO_DARIO_TOKEN"; return finish(out); }

    const now = Date.now(); const minutes = 15; const eventId = "sim_" + now;
    const event = {
      id: eventId, launcherId: "claude_demo_bot", launcherName: "Claude (demo)", launcherPhoto: "",
      barName: "Via Bure Vecchia Nord, Pistoia", barLat: 43.935859, barLng: 10.9400289,
      minutes: minutes, createdAt: now, mode: "CERCHIA", acceptedCount: 0,
      invitedIds: dario.map(d => d.id), cancelled: false, simulated: true
    };
    await db.collection("events").doc(eventId).set(event);
    out.eventId = eventId;

    const expiresAt = now + minutes * 60000;
    const res = await msg.sendEachForMulticast({
      notification: {
        title: "\u2615 Claude ti invita a un Extreme Coffee!",
        body: "Via Bure Vecchia Nord, Pistoia \u2022 hai 15 minuti per arrivare."
      },
      data: { eventId: String(eventId), expiresAt: String(expiresAt) },
      android: { priority: "high", notification: { channelId: "extreme_coffee_invites" }, ttl: minutes * 60000 },
      tokens: dario.map(d => d.token)
    });
    out.sent = res.successCount; out.failed = res.failureCount;
    out.perToken = res.responses.map((r, i) => ({ id: dario[i].id, ok: r.success, error: r.success ? null : (r.error && r.error.code) }));
    out.status = "DONE";
  } catch (e) { out.status = "ERROR"; out.error = String(e && e.message || e); }
  finish(out);
})();
