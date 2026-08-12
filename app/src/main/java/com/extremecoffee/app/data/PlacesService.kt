package com.extremecoffee.app.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val TAG = "PlacesService"
private const val TIMEOUT_MS = 8_000

/**
 * Autocompletamento indirizzi basato su Photon (OpenStreetMap) — https://photon.komoot.io
 *
 * PERCHÉ QUESTA SCELTA: l'autocomplete di Google Places richiede la "Places API" abilitata e
 * fatturabile su Google Cloud, con la chiave configurata senza restrizioni che la escludano.
 * Quando quella configurazione non è a posto, Google non restituisce risultati (senza errori
 * visibili). Photon invece è gratuito, non richiede alcuna chiave, copre tutto il mondo ed è
 * progettato proprio per il "completamento mentre digiti". Così la funzione è affidabile a
 * prescindere dalla configurazione Google.
 *
 * L'interfaccia pubblica (Suggestion, autocomplete, fetchLatLng) è rimasta identica a prima,
 * quindi la schermata che la usa non ha avuto bisogno di modifiche.
 * Nota: le coordinate arrivano già nella risposta di autocomplete, quindi le "impacchettiamo"
 * dentro placeId ("lat,lng") e fetchLatLng le rilegge senza dover fare una seconda chiamata.
 */
object PlacesService {

    data class Suggestion(val placeId: String, val label: String)

    /** Lingue realmente supportate da Photon per i nomi; per le altre usiamo l'inglese. */
    private fun photonLang(): String {
        return when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
            "it" -> "it"
            "de" -> "de"
            "fr" -> "fr"
            else -> "en"
        }
    }

    suspend fun autocomplete(
        context: Context,
        query: String,
        originLat: Double? = null,
        originLng: Double? = null
    ): List<Suggestion> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()

        val url = buildString {
            append("https://photon.komoot.io/api/?q=")
            append(Uri.encode(query.trim()))
            append("&limit=6")
            append("&lang=").append(photonLang())
            // Bias sulla posizione: dà priorità ai risultati vicini all'utente (non è un filtro,
            // funziona in qualsiasi paese).
            if (originLat != null && originLng != null) {
                append("&lat=").append(originLat)
                append("&lon=").append(originLng)
            }
        }

        val result = withTimeoutOrNull(TIMEOUT_MS.toLong()) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    // User-Agent richiesto dalle policy dei servizi OSM.
                    setRequestProperty("User-Agent", "ExtremeCoffee/1.0 (Android; contatto: unlimitedvisionltd@gmail.com)")
                }
                val code = conn.responseCode
                if (code != 200) {
                    Log.e(TAG, "Photon ha risposto HTTP $code")
                    conn.disconnect()
                    return@withTimeoutOrNull emptyList<Suggestion>()
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                parsePhoton(body)
            } catch (e: Exception) {
                Log.e(TAG, "Richiesta Photon fallita: ${e.message}", e)
                emptyList<Suggestion>()
            }
        }
        result ?: run {
            Log.e(TAG, "Timeout richiesta Photon (>${TIMEOUT_MS}ms)")
            emptyList()
        }
    }

    /**
     * Le coordinate sono già dentro placeId nel formato "lat,lng" (le mette autocomplete),
     * quindi qui non serve alcuna chiamata di rete: le rileggiamo e basta.
     */
    suspend fun fetchLatLng(context: Context, placeId: String): Pair<Double, Double>? {
        val parts = placeId.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null
        return lat to lng
    }

    private fun parsePhoton(body: String): List<Suggestion> {
        val out = ArrayList<Suggestion>()
        val features = JSONObject(body).optJSONArray("features") ?: return out
        for (i in 0 until features.length()) {
            val f = features.optJSONObject(i) ?: continue
            val geom = f.optJSONObject("geometry") ?: continue
            val coords = geom.optJSONArray("coordinates") ?: continue
            if (coords.length() < 2) continue
            val lng = coords.optDouble(0, Double.NaN)
            val lat = coords.optDouble(1, Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue
            val props = f.optJSONObject("properties") ?: JSONObject()
            val label = buildLabel(props)
            if (label.isBlank()) continue
            out.add(Suggestion("$lat,$lng", label))
        }
        return out
    }

    /** Costruisce un'etichetta leggibile dalle proprietà OSM (nome, via, città, paese…). */
    private fun buildLabel(p: JSONObject): String {
        fun v(key: String): String? = p.optString(key).takeIf { it.isNotBlank() }

        val name = v("name")
        val street = v("street")
        val house = v("housenumber")
        val postcode = v("postcode")
        val city = v("city") ?: v("district") ?: v("county") ?: v("locality")
        val state = v("state")
        val country = v("country")

        val streetLine = when {
            street != null && house != null -> "$street $house"
            street != null -> street
            else -> null
        }
        val cityLine = listOfNotNull(postcode, city).joinToString(" ").takeIf { it.isNotBlank() }

        val parts = ArrayList<String>()
        if (name != null && name != street) parts.add(name)
        if (streetLine != null) parts.add(streetLine)
        when {
            cityLine != null -> parts.add(cityLine)
            state != null -> parts.add(state)
        }
        if (country != null) parts.add(country)

        return parts.distinct().joinToString(", ")
    }
}
