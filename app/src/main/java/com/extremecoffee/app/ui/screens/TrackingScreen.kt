package com.extremecoffee.app.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.locationFlow
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.model.ParticipantLocation
import com.extremecoffee.app.ui.CoffeeScaffold
import com.extremecoffee.app.ui.GeoLine
import com.extremecoffee.app.ui.GeoMarker
import com.extremecoffee.app.ui.AppMap
import com.extremecoffee.app.ui.goFresh
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackingScreen(nav: NavController, eventId: String, isLauncher: Boolean) {
    val context = LocalContext.current
    val event by CoffeeRepository.eventFlow(eventId).collectAsState(initial = null)
    val locations by CoffeeRepository.participantLocations(eventId).collectAsState(initial = emptyList())

    // Se il lanciatore annulla mentre sei in viaggio, torni alla home (ricevi anche la notifica).
    LaunchedEffect(event?.cancelled) {
        if (event?.cancelled == true) nav.goFresh("home")
    }
    var remaining by remember { mutableStateOf(0L) }
    var myLat by remember { mutableStateOf<Double?>(null) }
    var myLng by remember { mutableStateOf<Double?>(null) }
    val role = if (isLauncher) "launcher" else "guest"

    val locPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { if (!locPermission.status.isGranted) locPermission.launchPermissionRequest() }

    LaunchedEffect(locPermission.status.isGranted, isLauncher) {
        if (!isLauncher && locPermission.status.isGranted) {
            locationFlow(context).collect { (lat, lng) ->
                myLat = lat; myLng = lng
                val ev = event
                val arrived = if (ev != null) {
                    val r = FloatArray(1)
                    android.location.Location.distanceBetween(lat, lng, ev.barLat, ev.barLng, r)
                    r[0] <= 120f
                } else false
                CoffeeRepository.updateMyLocation(
                    eventId,
                    ParticipantLocation(Profile.id(context), Profile.name(context), lat, lng, arrived, System.currentTimeMillis())
                )
            }
        }
    }

    LaunchedEffect(event?.id) {
        while (true) { remaining = event?.remainingMillis() ?: 0L; delay(1_000) }
    }

    CoffeeScaffold(event?.barName ?: "Tracking", nav, "tracking/$eventId/$role") { mod ->
        val e = event
        if (e == null) {
            Box(mod.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@CoffeeScaffold
        }

        Column(mod.fillMaxSize().padding(16.dp)) {

            // --- SEZIONE TIMER DEDICATA (separata dalla mappa) ---
            val min = (remaining / 60_000).coerceAtLeast(0)
            val sec = (remaining % 60_000 / 1_000).coerceAtLeast(0)
            val urgent = remaining in 1 until 60_000
            val timerColor = when {
                remaining <= 0 -> MaterialTheme.colorScheme.error
                urgent -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("\u2615 ${e.barName}", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text(
                            if (isLauncher) "${locations.size} amici in arrivo" else "Stai arrivando!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("TEMPO", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format("%02d:%02d", min, sec),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black, color = timerColor
                        )
                    }
                }
            }

            if (remaining <= 0) {
                // --- TIMER SCADUTO: 2 opzioni per chi ha ricevuto l'invito ---
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { nav.goFresh("selfie/$eventId") },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) { Text("\uD83D\uDCF8 Scatta il Selfie con i tuoi amici!", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { nav.goFresh("home") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ritorna alla home") }
            } else {
                val arrivedNow = run {
                    val la = myLat; val ln = myLng
                    if (!isLauncher && la != null && ln != null) {
                        val r = FloatArray(1)
                        android.location.Location.distanceBetween(la, ln, e.barLat, e.barLng, r)
                        r[0] <= 120f
                    } else false
                }
                if (arrivedNow) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { nav.navigate("selfie/$eventId") },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("\uD83D\uDCF8 Sei arrivato! Scatta il Selfie Coffee", fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- AREA MAPPA DEDICATA (riempie lo spazio rimanente, separata) ---
            val markers = buildList {
                add(GeoMarker(e.barLat, e.barLng, "${e.barName} \u2615", coffee = true))
                locations.forEach { add(GeoMarker(it.lat, it.lng, it.name)) }
            }
            val lines = locations.map { GeoLine(it.lat, it.lng, e.barLat, e.barLng) }

            Box(Modifier.fillMaxSize().clip(MaterialTheme.shapes.large)) {
                AppMap(
                    modifier = Modifier.fillMaxSize(),
                    centerLat = e.barLat, centerLng = e.barLng, zoom = 15.0,
                    markers = markers, lines = lines
                )
            }
        }
    }
}
