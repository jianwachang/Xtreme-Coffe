const admin = require("firebase-admin");
const fs = require("fs");
function finish(o){ fs.writeFileSync("op-result.json", JSON.stringify(o,null,2)); console.log(JSON.stringify(o,null,2)); }
(async () => {
  const out = { action: "cleanup_amicizia" };
  try {
    admin.initializeApp({ credential: admin.credential.cert(JSON.parse(process.env.SA_JSON)) });
    const db = admin.firestore();
    const evs = await db.collection("events").get();
    const deleted = [];
    for (const d of evs.docs) {
      if (d.id.startsWith("test_amicizia_")) {
        // elimina anche eventuali sottoraccolte locations
        const locs = await d.ref.collection("locations").get();
        for (const l of locs.docs) await l.ref.delete();
        await d.ref.delete();
        deleted.push(d.id);
      }
    }
    out.deletedEvents = deleted;
    out.status = "DONE";
  } catch (e) { out.status = "ERROR"; out.error = String(e && e.message || e); }
  finish(out);
})();
