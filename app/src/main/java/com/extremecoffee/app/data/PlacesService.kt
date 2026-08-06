package com.extremecoffee.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "PlacesService"
private const val NETWORK_TIMEOUT_MS = 8_000L

/**
 * Autocompletamento ufficiale Google Places.
 * - autocomplete(): suggerimenti mentre scrivi (solo testo, niente coordinate).
 * - fetchLatLng(): coordinate del luogo scelto (chiamata quando l'utente tocca un suggerimento).
 * La chiave viene letta dalla meta-data del manifest (com.google.android.geo.API_KEY).
 *
 * Se i suggerimenti non compaiono MAI (lista sempre vuota), la causa più probabile non è nel
 * codice ma nella configurazione della chiave su Google Cloud Console: la "Places API" deve
 * essere abilitata per il progetto E la chiave non deve avere restrizioni che la escludono
 * (controllare "API restrictions" sulla chiave). Gli errori vengono scritti nel Logcat con
 * tag "PlacesService" per poterli verificare via `adb logcat`.
 */
object PlacesService {

    data class Suggestion(val placeId: String, val label: String)

    private var client: PlacesClient? = null
    private var token: AutocompleteSessionToken? = null

    /** Inizializza il client Places. Non lancia mai eccezioni: se fallisce, client resta null. */
    private fun ensureReady(context: Context): Boolean {
        return try {
            if (!Places.isInitialized()) {
                val ai = context.packageManager
                    .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                val key = ai.metaData?.getString("com.google.android.geo.API_KEY")
                if (key.isNullOrBlank()) {
                    Log.e(TAG, "Chiave Maps/Places assente o vuota nel manifest: autocomplete disabilitato.")
                    return false
                }
                Places.initialize(context.applicationContext, key)
            }
            if (client == null && Places.isInitialized()) {
                client = Places.createClient(context.applicationContext)
            }
            if (token == null) token = AutocompleteSessionToken.newInstance()
            client != null
        } catch (e: Exception) {
            // Qualsiasi problema di inizializzazione (chiave non valida, SDK non pronto, ecc.)
            // non deve mai propagarsi: meglio "nessun suggerimento" che un crash dell'app.
            Log.e(TAG, "Inizializzazione Places fallita", e)
            false
        }
    }

    suspend fun autocomplete(
        context: Context,
        query: String,
        originLat: Double? = null,
        originLng: Double? = null
    ): List<Suggestion> {
        if (query.trim().length < 2) return emptyList()
        if (!ensureReady(context)) return emptyList()
        val c = client ?: return emptyList()
        return try {
            withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                val builder = FindAutocompletePredictionsRequest.builder()
                    .setSessionToken(token)
                    .setQuery(query)
                // Nessuna restrizione di paese: l'autocomplete funziona ovunque nel mondo.
                // Il bias sulla posizione (sotto) resta comunque il modo principale per dare
                // priorità ai risultati vicini all'utente, in qualsiasi paese si trovi.
                if (originLat != null && originLng != null) {
                    builder.setOrigin(LatLng(originLat, originLng))
                }
                c.findAutocompletePredictions(builder.build()).await()
                    .autocompletePredictions
                    .map { Suggestion(it.placeId, it.getFullText(null).toString()) }
            } ?: run {
                Log.e(TAG, "Timeout richiesta autocomplete (>${NETWORK_TIMEOUT_MS}ms)")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Richiesta autocomplete fallita: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchLatLng(context: Context, placeId: String): Pair<Double, Double>? {
        if (!ensureReady(context)) return null
        val c = client ?: return null
        return try {
            withTimeoutOrNull(NETWORK_TIMEOUT_MS) {
                val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG))
                    .setSessionToken(token)
                    .build()
                val place = c.fetchPlace(request).await().place
                token = AutocompleteSessionToken.newInstance() // nuova sessione dopo il fetch (fatturazione corretta)
                place.latLng?.let { it.latitude to it.longitude }
            } ?: run {
                Log.e(TAG, "Timeout richiesta fetchLatLng (>${NETWORK_TIMEOUT_MS}ms)")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Richiesta fetchLatLng fallita: ${e.message}", e)
            null
        }
    }
}
