package com.extremecoffee.app.data

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Percorso su strada via Directions API di Google.
 * La chiamata è volutamente rara (una all'accettazione + una ogni 180s) per contenere i costi.
 * Usa la stessa chiave Maps presente nel manifest: deve avere la "Directions API" abilitata
 * e NON essere ristretta alle sole app Android (le web-service richiedono chiave senza quel vincolo).
 */
object DirectionsService {

    private fun apiKey(context: Context): String? = runCatching {
        val ai = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        ai.metaData?.getString("com.google.android.geo.API_KEY")
    }.getOrNull()

    suspend fun route(
        context: Context,
        oLat: Double, oLng: Double,
        dLat: Double, dLng: Double,
        mode: String = "driving"
    ): List<LatLng>? = withContext(Dispatchers.IO) {
        val key = apiKey(context)
        if (key.isNullOrBlank()) return@withContext null
        val urlStr = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=$oLat,$oLng&destination=$dLat,$dLng&mode=$mode&key=$key"
        try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val routes = JSONObject(body).optJSONArray("routes") ?: return@withContext null
            if (routes.length() == 0) return@withContext null
            val enc = routes.getJSONObject(0)
                .optJSONObject("overview_polyline")?.optString("points")
                ?: return@withContext null
            decode(enc)
        } catch (e: Exception) {
            null
        }
    }

    /** Decodifica il formato "encoded polyline" di Google in una lista di punti. */
    private fun decode(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1
            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return poly
    }
}
