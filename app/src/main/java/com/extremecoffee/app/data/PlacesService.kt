package com.extremecoffee.app.data

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await

/**
 * Autocompletamento ufficiale Google Places.
 * - autocomplete(): suggerimenti mentre scrivi (solo testo, niente coordinate).
 * - fetchLatLng(): coordinate del luogo scelto (chiamata quando l'utente tocca un suggerimento).
 * La chiave viene letta dalla meta-data del manifest (com.google.android.geo.API_KEY).
 */
object PlacesService {

    data class Suggestion(val placeId: String, val label: String)

    private var client: PlacesClient? = null
    private var token: AutocompleteSessionToken? = null

    private fun ensureReady(context: Context) {
        if (!Places.isInitialized()) {
            val ai = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val key = ai.metaData?.getString("com.google.android.geo.API_KEY")
            if (!key.isNullOrBlank()) Places.initialize(context.applicationContext, key)
        }
        if (client == null && Places.isInitialized()) {
            client = Places.createClient(context.applicationContext)
        }
        if (token == null) token = AutocompleteSessionToken.newInstance()
    }

    suspend fun autocomplete(
        context: Context,
        query: String,
        originLat: Double? = null,
        originLng: Double? = null
    ): List<Suggestion> {
        if (query.trim().length < 2) return emptyList()
        ensureReady(context)
        val c = client ?: return emptyList()
        return try {
            val builder = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setCountries("IT")
                .setQuery(query)
            // Bias sulla posizione dell'utente: i risultati vicini vengono ordinati prima.
            if (originLat != null && originLng != null) {
                builder.setOrigin(LatLng(originLat, originLng))
            }
            c.findAutocompletePredictions(builder.build()).await()
                .autocompletePredictions
                .map { Suggestion(it.placeId, it.getFullText(null).toString()) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchLatLng(context: Context, placeId: String): Pair<Double, Double>? {
        ensureReady(context)
        val c = client ?: return null
        return try {
            val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG))
                .setSessionToken(token)
                .build()
            val place = c.fetchPlace(request).await().place
            token = AutocompleteSessionToken.newInstance() // nuova sessione dopo il fetch (fatturazione corretta)
            place.latLng?.let { it.latitude to it.longitude }
        } catch (e: Exception) {
            null
        }
    }
}
