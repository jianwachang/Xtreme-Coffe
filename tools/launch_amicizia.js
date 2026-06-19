const admin = require("firebase-admin");
const fs = require("fs");
function finish(o){ fs.writeFileSync("op-result.json", JSON.stringify(o,null,2)); console.log(JSON.stringify(o,null,2)); }

const ADDRESS = "Via Silvio Pellico 25, Montecatini Terme";
const FALLBACK = { lat: 43.8828, lng: 10.7711 };      // centro Montecatini Terme (Wikipedia)
const MINUTES = 60;

(async () => {
  const out = { action: "launch_amicizia", address: ADDRESS };
  try {
    admin.initializeApp({ credential: admin.credential.cert(JSON.parse(process.env.SA_JSON)) });
    const db = admin.firestore();

    // 1) Geocodifica via Google (precisione sull'indirizzo). Fallback: centro citta'.
    let lat = FALLBACK.lat, lng = FALLBACK.lng, geo = "FALLBACK";
    const key = process.env.MAPS_KEY || "";
    if (key) {
      try {
        const url = "https://maps.googleapis.com/maps/api/geocode/json?address=" +
          encodeURIComponent(ADDRESS) + "&region=it&key=" + key;
        const r = await fetch(url);
        const j = await r.json();
        out.geocodeApiStatus = j.status;
        if (j.status === "OK" && j.results && j.results[0]) {
          lat = j.results[0].geometry.location.lat;
          lng = j.results[0].geometry.location.lng;
          geo = "GOOGLE";
          out.formattedAddress = j.results[0].formatted_address;
        } else {
          out.geocodeError = j.error_message || j.status;
        }
      } catch (e) { out.geocodeError = String(e && e.message || e); }
    } else { out.geocodeApiStatus = "NO_KEY"; }
    out.coords = { lat, lng, source: geo };

    // 2) Crea l'evento AMICIZIA (nessuna notifica: la modalita' amicizia non fa broadcast).
    const now = Date.now();
    const eventId = "test_amicizia_" + now;
    const event = {
      id: eventId,
      launcherId: "claude_demo_bot",
      launcherName: "Claude \u00b7 test AMICIZIA",
      barName: ADDRESS,
      barLat: lat, barLng: lng,
      minutes: MINUTES,
      createdAt: now,
      mode: "AMICIZIA",
      acceptedCount: 0,
      invitedIds: [],
      launcherPhoto: "",
      cancelled: false,
      simulated: true
    };
    await db.collection("events").doc(eventId).set(event);

    out.eventId = eventId;
    out.minutes = MINUTES;
    out.expiresAt = new Date(now + MINUTES * 60000).toISOString();
    out.status = "DONE";
  } catch (e) { out.status = "ERROR"; out.error = String(e && e.message || e); }
  finish(out);
})();
