package com.extremecoffee.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val TAG = "PlacesService"
private const val TIMEOUT_MS = 10_000

/**
 * Ricerca indirizzi con DUE fornitori OpenStreetMap indipendenti, entrambi gratuiti e senza
 * chiave API: prima Photon (komoot), pensato per il completamento mentre digiti; se Photon non
 * risponde, si passa automaticamente a Nominatim. Così, se un servizio è irraggiungibile dalla
 * rete dell'utente, l'altro fa comunque il lavoro.
 *
 * IMPORTANTE (diagnostica): autocomplete() ora RESTITUISCE ANCHE l'esito, così la schermata può
 * mostrare all'utente cosa sta succedendo ("sto cercando", "nessun risultato", oppure il motivo
 * dell'errore) invece di restare muta. Prima ogni problema di rete spariva in silenzio e sembrava
 * che l'autocomplete "non funzionasse".
 *
 * Le coordinate arrivano già nella risposta: le impacchettiamo in placeId ("lat,lng") e
 * fetchLatLng le rilegge senza una seconda chiamata di rete.
 */
object PlacesService {

    data class Suggestion(val placeId: String, val label: String)

    /** Esito della ricerca, così la UI può distinguere risultati / nessun risultato / errore. */
    sealed class Result {
        data class Ok(val items: List<Suggestion>) : Result()
        data class Failed(val reason: String) : Result()
    }

    private fun lang(): String = when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
        "it" -> "it"; "de" -> "de"; "fr" -> "fr"; else -> "en"
    }

    suspend fun autocomplete(
        context: Context,
        query: String,
        originLat: Double? = null,
        originLng: Double? = null
    ): Result = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext Result.Ok(emptyList())

        // 1) Photon. null = errore/irraggiungibile; lista (anche vuota) = risposta valida.
        val photon = fromPhoton(q, originLat, originLng)
        if (photon != null) return@withContext Result.Ok(photon)

        // 2) Fallback Nominatim (solo se Photon ha dato errore, non se ha risposto "0 risultati").
        val nominatim = fromNominatim(q, originLat, originLng)
        if (nominatim != null) return@withContext Result.Ok(nominatim)

        // Entrambi irraggiungibili: quasi sempre è la connessione internet del telefono.
        Result.Failed("Nessuna risposta dai servizi di ricerca (controlla la connessione).")
    }

    /** placeId è "lat,lng": nessuna chiamata di rete necessaria. */
    suspend fun fetchLatLng(context: Context, placeId: String): Pair<Double, Double>? {
        val p = placeId.split(",")
        if (p.size != 2) return null
        val lat = p[0].trim().toDoubleOrNull() ?: return null
        val lng = p[1].trim().toDoubleOrNull() ?: return null
        return lat to lng
    }

    // ---------------- Photon ----------------
    private fun fromPhoton(q: String, lat: Double?, lng: Double?): List<Suggestion>? {
        val url = buildString {
            append("https://photon.komoot.io/api/?q=").append(Uri.encode(q))
            append("&limit=6&lang=").append(lang())
            if (lat != null && lng != null) { append("&lat=").append(lat); append("&lon=").append(lng) }
        }
        val body = httpGet(url) ?: return null
        return try {
            val out = ArrayList<Suggestion>()
            val feats = JSONObject(body).optJSONArray("features") ?: JSONArray()
            for (i in 0 until feats.length()) {
                val f = feats.optJSONObject(i) ?: continue
                val c = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                if (c.length() < 2) continue
                val lo = c.optDouble(0, Double.NaN); val la = c.optDouble(1, Double.NaN)
                if (la.isNaN() || lo.isNaN()) continue
                val label = photonLabel(f.optJSONObject("properties") ?: JSONObject())
                if (label.isNotBlank()) out.add(Suggestion("$la,$lo", label))
            }
            out
        } catch (e: Exception) {
            Log.e(TAG, "Parsing Photon fallito: ${e.message}", e); null
        }
    }

    private fun photonLabel(p: JSONObject): String {
        fun v(k: String) = p.optString(k).takeIf { it.isNotBlank() }
        val name = v("name"); val street = v("street"); val house = v("housenumber")
        val city = v("city") ?: v("district") ?: v("county") ?: v("locality")
        val line = when { street != null && house != null -> "$street $house"; else -> street }
        val cityLine = listOfNotNull(v("postcode"), city).joinToString(" ").ifBlank { null }
        return listOfNotNull(
            name?.takeIf { it != street }, line, cityLine ?: v("state"), v("country")
        ).distinct().joinToString(", ")
    }

    // ---------------- Nominatim (riserva) ----------------
    private fun fromNominatim(q: String, lat: Double?, lng: Double?): List<Suggestion>? {
        val url = buildString {
            append("https://nominatim.openstreetmap.org/search?q=").append(Uri.encode(q))
            append("&format=jsonv2&addressdetails=0&limit=6")
            append("&accept-language=").append(lang())
        }
        val body = httpGet(url) ?: return null
        return try {
            val arr = JSONArray(body)
            val out = ArrayList<Suggestion>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val la = o.optString("lat").toDoubleOrNull() ?: continue
                val lo = o.optString("lon").toDoubleOrNull() ?: continue
                val label = o.optString("display_name").takeIf { it.isNotBlank() } ?: continue
                out.add(Suggestion("$la,$lo", label))
            }
            out
        } catch (e: Exception) {
            Log.e(TAG, "Parsing Nominatim fallito: ${e.message}", e); null
        }
    }

    // ---------------- HTTP comune ----------------
    /** GET con timeout. Ritorna il corpo, oppure null in caso di errore/irraggiungibilità. */
    private fun httpGet(url: String): String? {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS; readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", "ExtremeCoffee/1.0 (Android; unlimitedvisionltd@gmail.com)")
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            if (code != 200) {
                Log.e(TAG, "HTTP $code su $url")
                conn.disconnect(); return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            body
        } catch (e: Exception) {
            Log.e(TAG, "GET fallita ($url): ${e.message}", e); null
        }
    }
}
