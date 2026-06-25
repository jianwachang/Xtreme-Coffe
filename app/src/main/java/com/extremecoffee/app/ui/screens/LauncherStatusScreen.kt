package com.extremecoffee.app.ui.screens

import android.Manifest
import androidx.compose.ui.res.stringResource
import com.extremecoffee.app.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.DirectionsService
import com.extremecoffee.app.model.CoffeeEvent
import com.google.android.gms.maps.model.LatLng
import com.extremecoffee.app.ui.AppMap
import com.extremecoffee.app.ui.CoffeeScaffold
import com.extremecoffee.app.ui.GeoLine
import com.extremecoffee.app.ui.GeoMarker
import com.extremecoffee.app.ui.goFresh
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LauncherStatusScreen(nav: NavController, eventId: String) {
    val context = LocalContext.current
    val event by CoffeeRepository.eventFlow(eventId).collectAsState(initial = null)
    val locations by CoffeeRepository.participantLocations(eventId).collectAsState(initial = emptyList())
    var remaining by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()
    var showCancel by remember { mutableStateOf(false) }
    val routes = remember { mutableStateMapOf<String, List<LatLng>>() }
    val lastRouteAt = remember { mutableStateMapOf<String, Long>() }

    val locPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { if (!locPermission.status.isGranted) locPermission.launchPermissionRequest() }

    LaunchedEffect(event?.id) {
        while (true) { remaining = event?.remainingMillis() ?: 0L; delay(1_000) }
    }

    // Percorso su strada (Directions API): 1 chiamata quando un amico accetta,
    // poi 1 aggiornamento ogni 180 secondi per ciascuno. Il tentativo viene segnato
    // anche se fallisce, così non si moltiplicano le chiamate (e i costi).
    LaunchedEffect(eventId) {
        while (true) {
            val ev = event
            if (ev != null) {
                val current = locations
                val now = System.currentTimeMillis()
                current.forEach { p ->
                    val last = lastRouteAt[p.userId]
                    if (last == null || now - last >= 180_000L) {
                        lastRouteAt[p.userId] = now
                        val r = DirectionsService.route(context, p.lat, p.lng, ev.barLat, ev.barLng)
                        if (r != null) routes[p.userId] = r
                    }
                }
                val ids = current.map { it.userId }.toSet()
                (routes.keys - ids).forEach { routes.remove(it); lastRouteAt.remove(it) }
            }
            delay(12_000)
        }
    }

    CoffeeScaffold(stringResource(R.string.ls_title), nav, "launched/$eventId") { mod ->
        val e: CoffeeEvent? = event
        if (e == null) {
            Box(mod.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@CoffeeScaffold
        }

        val min = (remaining / 60_000).coerceAtLeast(0)
        val sec = (remaining % 60_000 / 1_000).coerceAtLeast(0)
        val finished = remaining <= 0
        val urgent = remaining in 1 until 60_000
        val timerColor = when {
            finished -> MaterialTheme.colorScheme.error
            urgent -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }

        Column(mod.fillMaxSize().padding(16.dp)) {

            // --- INTESTAZIONE: hai lanciato + amici in arrivo + countdown ---
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ls_launched), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(e.barName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (finished) stringResource(R.string.ls_timeout)
                            else stringResource(R.string.ls_incoming, locations.size, min),
                            style = MaterialTheme.typography.bodySmall,
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

            Spacer(Modifier.height(12.dp))

            // --- MAPPA LIVE: bar + chi ha accettato (marker + linea verso il bar) + la tua posizione ---
            val markers = buildList {
                add(GeoMarker(e.barLat, e.barLng, "${e.barName} \u2615", coffee = true))
                locations.forEach { add(GeoMarker(it.lat, it.lng, it.name, photo = it.photo)) }
            }
            val lines = locations.filter { it.userId !in routes }
                .map { GeoLine(it.lat, it.lng, e.barLat, e.barLng) }
            val routePolylines = routes.values.toList()

            Box(
                Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large)
            ) {
                AppMap(
                    modifier = Modifier.fillMaxSize(),
                    centerLat = e.barLat, centerLng = e.barLng, zoom = 15.0,
                    markers = markers, lines = lines, polylines = routePolylines,
                    showMyLocation = locPermission.status.isGranted
                )
                if (locations.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                    ) {
                        Text(
                            stringResource(R.string.ls_waiting),
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (finished) {
                // --- TIMER SCADUTO: 3 opzioni ---
                Button(
                    onClick = { nav.goFresh("selfie/$eventId") },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.large
                ) { Text(stringResource(R.string.ls_selfie), fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = { nav.goFresh("launch") },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.large
                ) { Text(stringResource(R.string.ls_relaunch), fontWeight = FontWeight.Bold) }

                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { nav.goFresh("home") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.ls_home)) }
            } else {
                if (e.mode == "CERCHIA") {
                    OutlinedButton(
                        onClick = { nav.goFresh("inviteCircle/$eventId") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.large
                    ) { Text(stringResource(R.string.ic_title)) }
                    Spacer(Modifier.height(10.dp))
                }

                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        stringResource(R.string.ls_new_in) + String.format("%02d:%02d", min, sec),
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { showCancel = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.ls_cancel), color = MaterialTheme.colorScheme.error) }
            }
        }

        if (showCancel) {
            AlertDialog(
                onDismissRequest = { showCancel = false },
                title = { Text(stringResource(R.string.ls_cancel_title)) },
                text = { Text(stringResource(R.string.ls_cancel_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        showCancel = false
                        scope.launch { CoffeeRepository.cancelCoffee(eventId) }
                        nav.goFresh("home")
                    }) { Text(stringResource(R.string.ls_cancel_yes), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showCancel = false }) { Text(stringResource(R.string.ls_cancel_no)) }
                }
            )
        }
    }
}
