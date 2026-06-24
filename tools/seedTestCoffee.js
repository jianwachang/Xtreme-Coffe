// Crea un Extreme Coffee di prova (modalità CERCHIA) invitando Dario.
// Gira in CI col service account (GOOGLE_APPLICATION_CREDENTIALS=sa.json).
// La Cloud Function onNewEvent invierà la notifica push al token di Dario.
const admin = require("firebase-admin");
const fs = require("fs");

admin.initializeApp({ credential: admin.credential.applicationDefault() });
const db = admin.firestore();

(async () => {
  const out = { ok: false };
  try {
    const TARGET_DIGITS = "3935672548"; // parte significativa di +39 393 567 2548

    const snap = await db.collection("users").get();
    let dario = null;
    snap.forEach((d) => {
      if (dario) return;
      const x = d.data() || {};
      const digits = String(x.phone || "").replace(/\D/g, "");
      const nm = String(x.name || x.nickname || "").trim().toLowerCase();
      if (digits.endsWith(TARGET_DIGITS) || nm === "dario") {
        dario = {
          docId: d.id,
          id: x.id || d.id,
          name: x.name || x.nickname || "",
          phone: x.phone || "",
        };
      }
    });

    out.usersScanned = snap.size;
    if (!dario) {
      out.error = "Utente Dario non trovato nella collection users.";
    } else {
      out.darioId = dario.id;
      out.darioName = dario.name;
      out.darioPhone = dario.phone;

      // C'è un token FCM registrato? (serve per ricevere la push)
      const tok = await db.collection("tokens").doc(dario.id).get();
      out.hasToken = tok.exists && !!(tok.data() || {}).token;

      const ref = db.collection("events").doc();
      const now = Date.now();
      const minutes = 20;
      const ev = {
        id: ref.id,
        launcherId: "claude-tester",
        launcherName: "Claude \u2615",
        barName: "Bar del Duomo (test)",
        barLat: 45.4641,
        barLng: 9.1919,
        minutes: minutes,
        createdAt: now,
        mode: "CERCHIA",
        acceptedCount: 0,
        invitedIds: [dario.id],
        launcherPhoto: "",
        cancelled: false,
      };
      await ref.set(ev);

      out.eventId = ref.id;
      out.expiresAt = now + minutes * 60000;
      out.ok = true;
    }
  } catch (e) {
    out.error = String((e && e.stack) || e);
  }

  fs.writeFileSync("op-result.json", JSON.stringify(out, null, 2));
  console.log(JSON.stringify(out, null, 2));
  process.exit(out.ok ? 0 : 1);
})();
