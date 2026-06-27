// Registra (o riusa) l'app Android com.extremecoffee.myapp nel progetto Firebase
// e produce un google-services.json che contiene SIA com.extremecoffee.app SIA com.extremecoffee.myapp.
const fs = require("fs");
const { GoogleAuth } = require("google-auth-library");

const PROJECT = "extreme-coffe";
const NEW_PKG = "com.extremecoffee.myapp";
const DISPLAY = "Extreme Coffee";

async function main() {
  const sa = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  const auth = new GoogleAuth({ credentials: sa, scopes: ["https://www.googleapis.com/auth/cloud-platform"] });
  const client = await auth.getClient();
  const base = "https://firebase.googleapis.com/v1beta1";
  const call = async (url, method = "GET", body) => {
    const res = await client.request({ url, method, data: body });
    return res.data;
  };

  // 1) Cerca se l'app esiste gia'
  let appId = null;
  let list = await call(`${base}/projects/${PROJECT}/androidApps?pageSize=100`);
  for (const a of (list.apps || [])) {
    if (a.packageName === NEW_PKG) { appId = a.appId; break; }
  }

  // 2) Altrimenti creala (operazione long-running)
  if (!appId) {
    const op = await call(`${base}/projects/${PROJECT}/androidApps`, "POST", { packageName: NEW_PKG, displayName: DISPLAY });
    let opName = op.name;
    for (let i = 0; i < 30 && opName; i++) {
      await new Promise(r => setTimeout(r, 3000));
      const st = await call(`${base}/${opName}`);
      if (st.done) {
        if (st.error) throw new Error("create op error: " + JSON.stringify(st.error));
        appId = st.response && st.response.appId;
        break;
      }
    }
    if (!appId) throw new Error("creazione app non completata");
  }

  // 3) Config (google-services.json del nuovo package, base64)
  const cfg = await call(`${base}/projects/-/androidApps/${appId}/config`);
  const newJson = JSON.parse(Buffer.from(cfg.configFileContents, "base64").toString("utf8"));

  // 4) Unisci nel google-services.json esistente (mantieni entrambi i client)
  const existing = JSON.parse(fs.readFileSync("app/google-services.json", "utf8"));
  const have = new Set(existing.client.map(c => c.client_info.android_client_info.package_name));
  for (const c of newJson.client) {
    const pkg = c.client_info.android_client_info.package_name;
    if (!have.has(pkg)) existing.client.push(c);
  }
  const merged = JSON.stringify(existing, null, 2);
  fs.writeFileSync("/tmp/google-services.merged.json", merged);
  fs.writeFileSync("op-result.json", JSON.stringify({
    ok: true, appId, newPackage: NEW_PKG,
    packages: existing.client.map(c => c.client_info.android_client_info.package_name)
  }, null, 2));
  fs.writeFileSync("google-services.b64", Buffer.from(merged).toString("base64"));
  console.log("OK appId=", appId);
}
main().catch(e => {
  fs.writeFileSync("op-result.json", JSON.stringify({ ok: false, error: String(e && e.message || e) }, null, 2));
  fs.writeFileSync("google-services.b64", "");
  console.error(e);
  process.exit(0); // non fallire il job: pubblichiamo comunque l'errore
});
