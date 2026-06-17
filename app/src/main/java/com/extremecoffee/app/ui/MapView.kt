package com.extremecoffee.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.extremecoffee.app.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

data class GeoMarker(val lat: Double, val lng: Double, val title: String = "", val coffee: Boolean = false)
data class GeoLine(val fromLat: Double, val fromLng: Double, val toLat: Double, val toLng: Double)

/**
 * Mappa basata su Google Maps (maps-compose).
 * - showMyLocation: pallino blu della posizione attuale (richiede permesso concesso).
 * - recenterLat/recenterLng: quando cambiano, la camera si sposta su quel punto.
 * - marker con coffee=true: icona a forma di caffè.
 */
@Composable
fun AppMap(
    modifier: Modifier = Modifier,
    centerLat: Double,
    centerLng: Double,
    zoom: Double = 15.0,
    markers: List<GeoMarker> = emptyList(),
    lines: List<GeoLine> = emptyList(),
    polylines: List<List<LatLng>> = emptyList(),
    recenterLat: Double? = null,
    recenterLng: Double? = null,
    showMyLocation: Boolean = false,
    onMapTap: ((Double, Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), zoom.toFloat())
    }

    LaunchedEffect(recenterLat, recenterLng) {
        if (recenterLat != null && recenterLng != null) {
            val target = LatLng(recenterLat, recenterLng)
            try {
                camera.animate(CameraUpdateFactory.newLatLngZoom(target, 16.5f))
            } catch (e: Exception) {
                camera.position = CameraPosition.fromLatLngZoom(target, 16.5f)
            }
        }
    }

    // IMPORTANTE: BitmapDescriptorFactory funziona solo DOPO l'inizializzazione delle Maps.
    // Inizializziamo qui e proteggiamo tutto: se fallisse, il marker usa lo spillo di default (niente crash).
    val coffeeIcon: BitmapDescriptor? = remember {
        runCatching {
            MapsInitializer.initialize(context)
            bitmapFromVector(context, R.drawable.ic_coffee_marker, 0.25f)
        }.getOrNull()
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(isMyLocationEnabled = showMyLocation),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = showMyLocation
        ),
        onMapClick = { latLng -> onMapTap?.invoke(latLng.latitude, latLng.longitude) }
    ) {
        lines.forEach { l ->
            Polyline(
                points = listOf(LatLng(l.fromLat, l.fromLng), LatLng(l.toLat, l.toLng)),
                color = Color(0xFF54B7C2),
                width = 8f
            )
        }
        polylines.forEach { pts ->
            if (pts.size >= 2) Polyline(points = pts, color = Color(0xFFE8772E), width = 12f)
        }
        markers.forEach { m ->
            Marker(
                state = MarkerState(LatLng(m.lat, m.lng)),
                title = m.title,
                icon = if (m.coffee) coffeeIcon else null,
                anchor = if (m.coffee) Offset(0.5f, 0.5f) else Offset(0.5f, 1f)
            )
        }
    }
}

private fun bitmapFromVector(context: Context, resId: Int, scale: Float = 1f): BitmapDescriptor? = runCatching {
    val drawable = ContextCompat.getDrawable(context, resId) ?: return null
    val w0 = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val h0 = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 120
    val w = (w0 * scale).toInt().coerceAtLeast(1)
    val h = (h0 * scale).toInt().coerceAtLeast(1)
    drawable.setBounds(0, 0, w, h)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    drawable.draw(Canvas(bitmap))
    BitmapDescriptorFactory.fromBitmap(bitmap)
}.getOrNull()
