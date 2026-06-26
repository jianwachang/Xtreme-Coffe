// Crea un Extreme Coffee di prova (CERCHIA) invitando Dario, a un indirizzo geocodificato.
// Gira in CI col service account (GOOGLE_APPLICATION_CREDENTIALS=sa.json).
// La Cloud Function onNewEvent invia la notifica push al token di Dario.
const admin = require("firebase-admin");
const fs = require("fs");

admin.initializeApp({ credential: admin.credential.applicationDefault() });
const db = admin.firestore();

const ADDRESS = process.env.SEED_ADDRESS || "Piazza del Popolo, Montecatini Terme";
const FALLBACK = { lat: 43.8821, lng: 10.7716 }; // centro Montecatini Terme

async function geocode(addr) {
  const key = process.env.GEOCODE_KEY || "";
  if (!key) return null;
  const url = "https://maps.googleapis.com/maps/api/geocode/json?address=" +
    encodeURIComponent(addr) + "&key=" + key;
  try {
    const r = await fetch(url);
    const j = await r.json();
    if (j.status === "OK" && j.results && j.results[0]) {
      const loc = j.results[0].geometry.location;
      return { lat: loc.lat, lng: loc.lng, formatted: j.results[0].formatted_address };
    }
    return { error: "geocode status " + j.status };
  } catch (e) {
    return { error: String(e) };
  }
}

(async () => {
  const out = { ok: false, address: ADDRESS };
  try {
    const TARGET_DIGITS = "3935672548";
    const snap = await db.collection("users").get();
    let dario = null;
    snap.forEach((d) => {
      if (dario) return;
      const x = d.data() || {};
      const digits = String(x.phone || "").replace(/\D/g, "");
      const nm = String(x.name || x.nickname || "").trim().toLowerCase();
      if (digits.endsWith(TARGET_DIGITS) || nm === "dario") {
        dario = { id: x.id || d.id, name: x.name || x.nickname || "", phone: x.phone || "" };
      }
    });
    out.usersScanned = snap.size;
    if (!dario) { out.error = "Utente Dario non trovato."; }
    else {
      out.darioId = dario.id; out.darioName = dario.name; out.darioPhone = dario.phone;
      const tok = await db.collection("tokens").doc(dario.id).get();
      out.hasToken = tok.exists && !!(tok.data() || {}).token;

      const g = await geocode(ADDRESS);
      let lat = FALLBACK.lat, lng = FALLBACK.lng;
      if (g && typeof g.lat === "number") { lat = g.lat; lng = g.lng; out.formatted = g.formatted; out.geocoded = true; }
      else { out.geocoded = false; out.geocodeNote = (g && g.error) || "nessuna chiave/risultato, uso fallback"; }
      out.lat = lat; out.lng = lng;

      const ref = db.collection("events").doc();
      const now = Date.now();
      const minutes = 20;
      await ref.set({
        id: ref.id,
        launcherId: "claude-tester",
        launcherName: "Claude \u2615",
        barName: ADDRESS,
        barLat: lat, barLng: lng,
        minutes: minutes, createdAt: now,
        mode: "CERCHIA", acceptedCount: 0,
        invitedIds: [dario.id], launcherPhoto: "", cancelled: false,
      });
      out.eventId = ref.id;
      out.expiresAt = now + minutes * 60000;
      out.ok = true;
    }
  } catch (e) { out.error = String((e && e.stack) || e); }

  fs.writeFileSync("op-result.json", JSON.stringify(out, null, 2));
  console.log(JSON.stringify(out, null, 2));
  process.exit(out.ok ? 0 : 1);
})();
