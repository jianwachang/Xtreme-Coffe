package com.extremecoffee.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val TAG = "PlacesService"
private const val TIMEOUT_MS = 10_000

/**
 * Ricerca indirizzi "mentre digiti", con due motori in cascata:
 *   1) Google Places (come su Google Maps) usando la chiave dell'app — se la "Places API" è
 *      abilitata sul progetto Google Cloud.
 *   2) Se Google non risponde (API non abilitata, chiave ristretta, ecc.), si passa a Photon
 *      (OpenStreetMap), gratuito e senza chiave, adatto alle app.
 *
 * IMPORTANTE: quando entrambi falliscono, autocomplete() restituisce il MOTIVO tecnico esatto,
 * così la schermata può mostrarlo e possiamo capire cosa succede invece di restare al buio.
 */
object PlacesService {

    data class Suggestion(val placeId: String, val label: String)

    sealed class Result {
        data class Ok(val items: List<Suggestion>) : Result()
        data class Failed(val reason: String) : Result()
    }

    private var client: PlacesClient? = null
    private var token: AutocompleteSessionToken? = null

    private fun lang(): String = when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
        "it" -> "it"; "de" -> "de"; "fr" -> "fr"; else -> "en"
    }

    suspend fun autocomplete(
        context: Context,
        query: String,
        originLat: Double? = null,
        originLng: Double? = null
    ): Result {
        val q = query.trim()
        if (q.length < 2) return Result.Ok(emptyList())

        // 1) Google Places
        val (googleItems, googleErr) = googleAutocomplete(context, q, originLat, originLng)
        if (googleItems != null) return Result.Ok(googleItems)

        // 2) Fallback Photon (OpenStreetMap)
        val (photonItems, photonErr) = photonAutocomplete(q, originLat, originLng)
        if (photonItems != null) return Result.Ok(photonItems)

        // Entrambi ko: riporto i due motivi, così è chiaro cosa correggere.
        return Result.Failed("Google: ${googleErr ?: "?"} · OpenStreetMap: ${photonErr ?: "?"}")
    }

    suspend fun fetchLatLng(context: Context, placeId: String): Pair<Double, Double>? {
        // Se placeId è nel formato "lat,lng" (viene da Photon), niente rete.
        val coords = placeId.split(",")
        if (coords.size == 2) {
            val la = coords[0].trim().toDoubleOrNull()
            val lo = coords[1].trim().toDoubleOrNull()
            if (la != null && lo != null) return la to lo
        }
        // Altrimenti è un placeId di Google: recupero le coordinate dal SDK.
        return try {
            val c = client ?: return null
            val req = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG))
                .setSessionToken(token).build()
            val place = c.fetchPlace(req).await().place
            token = AutocompleteSessionToken.newInstance()
            place.latLng?.let { it.latitude to it.longitude }
        } catch (e: Exception) {
            Log.e(TAG, "fetchPlace Google fallita: ${e.message}", e)
            null
        }
    }

    // ---------------- Google Places ----------------
    /** Ritorna (lista, null) se ok, oppure (null, motivo) se fallisce. */
    private suspend fun googleAutocomplete(
        context: Context, q: String, lat: Double?, lng: Double?
    ): Pair<List<Suggestion>?, String?> {
        if (!ensureGoogleReady(context)) return null to "chiave assente/non inizializzata"
        val c = client ?: return null to "client nullo"
        return try {
            val b = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(q)
            if (lat != null && lng != null) b.setOrigin(LatLng(lat, lng))
            val preds = c.findAutocompletePredictions(b.build()).await().autocompletePredictions
            preds.map { Suggestion(it.placeId, it.getFullText(null).toString()) } to null
        } catch (e: Exception) {
            // Il messaggio spesso dice ESATTAMENTE il problema (es. "This API project is not
            // authorized to use this API" = Places API non abilitata).
            val msg = e.message?.take(90) ?: e.javaClass.simpleName
            Log.e(TAG, "Google autocomplete: $msg", e)
            null to msg
        }
    }

    private fun ensureGoogleReady(context: Context): Boolean {
        return try {
            if (!Places.isInitialized()) {
                val ai = context.packageManager
                    .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                val key = ai.metaData?.getString("com.google.android.geo.API_KEY")
                if (key.isNullOrBlank()) return false
                Places.initialize(context.applicationContext, key)
            }
            if (client == null) client = Places.createClient(context.applicationContext)
            if (token == null) token = AutocompleteSessionToken.newInstance()
            client != null
        } catch (e: Exception) {
            Log.e(TAG, "Init Google Places fallita: ${e.message}", e); false
        }
    }

    // ---------------- Photon (OpenStreetMap) ----------------
    private suspend fun photonAutocomplete(
        q: String, lat: Double?, lng: Double?
    ): Pair<List<Suggestion>?, String?> = withContext(Dispatchers.IO) {
        val url = buildString {
            append("https://photon.komoot.io/api/?q=").append(Uri.encode(q))
            append("&limit=6&lang=").append(lang())
            if (lat != null && lng != null) { append("&lat=").append(lat); append("&lon=").append(lng) }
        }
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS; readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ExtremeCoffee/1.0 (Android; unlimitedvisionltd@gmail.com)")
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            if (code != 200) { conn.disconnect(); return@withContext null to "HTTP $code" }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val out = ArrayList<Suggestion>()
            val feats = JSONObject(body).optJSONArray("features")
            if (feats != null) {
                for (i in 0 until feats.length()) {
                    val f = feats.optJSONObject(i) ?: continue
                    val cc = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                    if (cc.length() < 2) continue
                    val lo = cc.optDouble(0, Double.NaN); val la = cc.optDouble(1, Double.NaN)
                    if (la.isNaN() || lo.isNaN()) continue
                    val label = photonLabel(f.optJSONObject("properties") ?: JSONObject())
                    if (label.isNotBlank()) out.add(Suggestion("$la,$lo", label))
                }
            }
            out to null
        } catch (e: Exception) {
            val msg = e.message?.take(90) ?: e.javaClass.simpleName
            Log.e(TAG, "Photon: $msg", e)
            null to msg
        }
    }

    private fun photonLabel(p: JSONObject): String {
        fun v(k: String) = p.optString(k).takeIf { it.isNotBlank() }
        val name = v("name"); val street = v("street"); val house = v("housenumber")
        val city = v("city") ?: v("district") ?: v("county") ?: v("locality")
        val line = if (street != null && house != null) "$street $house" else street
        val cityLine = listOfNotNull(v("postcode"), city).joinToString(" ").ifBlank { null }
        return listOfNotNull(name?.takeIf { it != street }, line, cityLine ?: v("state"), v("country"))
            .distinct().joinToString(", ")
    }
}
