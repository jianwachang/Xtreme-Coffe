const admin = require("firebase-admin");
const fs = require("fs");
const DEAD = [
  "4e84eeb8-3b22-4441-a917-f50c54925ed5",
  "9c4b5df9-845c-4cd3-97c3-5bb3f95b2e6e",
  "cd1dc8bc-2fa7-454a-8905-94c70d4d3fc3"
];
function finish(o){ fs.writeFileSync("op-result.json", JSON.stringify(o,null,2)); console.log(JSON.stringify(o,null,2)); }
(async () => {
  const out = { action: "cleanup" };
  try {
    admin.initializeApp({ credential: admin.credential.cert(JSON.parse(process.env.SA_JSON)) });
    const db = admin.firestore();
    // 1) elimina eventi demo
    const evs = await db.collection("events").get();
    const deleted = [];
    for (const d of evs.docs) { if (d.id.startsWith("sim_")) { await d.ref.delete(); deleted.push(d.id); } }
    out.deletedEvents = deleted;
    // 2) elimina i 3 token morti (per ID certo)
    const pruned = [];
    for (const id of DEAD) { try { await db.collection("tokens").doc(id).delete(); pruned.push(id); } catch (e) {} }
    out.prunedTokens = pruned;
    // 3) riepilogo token rimasti
    const toks = await db.collection("tokens").get();
    out.remaining = toks.docs.map(d => ({ id: d.id, name: (d.data() || {}).name || "" }));
    out.status = "DONE";
  } catch (e) { out.status = "ERROR"; out.error = String(e && e.message || e); }
  finish(out);
})();
