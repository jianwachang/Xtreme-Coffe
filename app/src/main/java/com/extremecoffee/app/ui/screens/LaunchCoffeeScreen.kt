@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)

package com.extremecoffee.app.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
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
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.data.PlacesService
import com.extremecoffee.app.data.locationFlow
import com.extremecoffee.app.model.CoffeeEvent
import com.extremecoffee.app.ui.AppMap
import com.extremecoffee.app.ui.CoffeeScaffold
import com.extremecoffee.app.ui.GeoMarker
import com.extremecoffee.app.ui.goFresh
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val TIMES = listOf(5, 10, 15, 20, 25)

@Composable
fun LaunchCoffeeScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myId = remember { Profile.id(context) }
    val acceptedInvite by CoffeeRepository.myAcceptedActiveEvent(myId).collectAsState(initial = null)

    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<PlacesService.Suggestion>>(emptyList()) }
    var picked by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    var minutes by remember { mutableStateOf(15) }
    var mode by remember { mutableStateOf("CERCHIA") }
    var barLat by remember { mutableStateOf<Double?>(null) }
    var barLng by remember { mutableStateOf<Double?>(null) }

    // Posizione attuale del telefono
    var myLat by remember { mutableStateOf<Double?>(null) }
    var myLng by remember { mutableStateOf<Double?>(null) }
    val locPerm = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { if (!locPerm.status.isGranted) locPerm.launchPermissionRequest() }
    LaunchedEffect(locPerm.status.isGranted) {
        if (locPerm.status.isGranted && myLat == null) {
            runCatching {
                val (lat, lng) = locationFlow(context).first()
                myLat = lat; myLng = lng
            }
        }
    }

    // Autocompletamento ufficiale Google Places (con piccolo ritardo)
    LaunchedEffect(query) {
        if (picked) { picked = false; return@LaunchedEffect }
        if (query.trim().length < 3) { suggestions = emptyList(); loading = false; return@LaunchedEffect }
        loading = true
        delay(300)
        suggestions = PlacesService.autocomplete(context, query)
        loading = false
    }

    fun pick(s: PlacesService.Suggestion) {
        picked = true
        query = s.label
        suggestions = emptyList()
        scope.launch {
            PlacesService.fetchLatLng(context, s.placeId)?.let { (lat, lng) ->
                barLat = lat; barLng = lng
            }
        }
    }

    val startLat = myLat ?: 45.4642
    val startLng = myLng ?: 9.1900
    val focusLat = barLat ?: myLat
    val focusLng = barLng ?: myLng

    CoffeeScaffold("Lancia un caffè", nav, "launch") { mod ->
        Column(mod.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            Text("Dove vi vedete?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Inserisci un bar o un indirizzo") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = { if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Surface(tonalElevation = 3.dp, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()) {
                    Column {
                        suggestions.forEachIndexed { i, s ->
                            Row(Modifier.fillMaxWidth().clickable { pick(s) }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(s.label, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (i < suggestions.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(Modifier.fillMaxWidth().height(300.dp).clip(MaterialTheme.shapes.large)) {
                AppMap(
                    modifier = Modifier.fillMaxSize(),
                    centerLat = startLat, centerLng = startLng, zoom = 14.0,
                    markers = if (barLat != null)
                        listOf(GeoMarker(barLat!!, barLng!!, query.ifBlank { "Punto scelto" }, coffee = true))
                    else emptyList(),
                    recenterLat = focusLat, recenterLng = focusLng,
                    showMyLocation = locPerm.status.isGranted,
                    onMapTap = { lat, lng -> barLat = lat; barLng = lng; if (query.isBlank()) query = "Punto sulla mappa" }
                )
            }
            Text(
                if (barLat != null) "☕ Punto di ritrovo impostato"
                else "Cerca l'indirizzo qui sopra, oppure tocca la mappa",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(20.dp))
            Text("Quanto tempo hanno?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TIMES.forEach { t ->
                    FilterChip(selected = minutes == t, onClick = { minutes = t }, label = { Text("$t min", maxLines = 1, softWrap = false) })
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Modalità", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeOption("Cerchia ristretta", "Inviti i tuoi contatti", mode == "CERCHIA") { mode = "CERCHIA" }
                ModeOption("Un caffè in amicizia", "Aperto a chi è nei paraggi", mode == "AMICIZIA") { mode = "AMICIZIA" }
            }

            Spacer(Modifier.height(28.dp))
            if (acceptedInvite != null) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "☕ Sei già in viaggio per un caffè. Potrai lanciare il tuo appena questo si conclude.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(
                enabled = acceptedInvite == null,
                onClick = {
                    val la = barLat; val ln = barLng
                    if (query.isBlank() || la == null || ln == null) {
                        Toast.makeText(context, "Scegli un posto (ricerca o tocco sulla mappa)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        try {
                            val id = CoffeeRepository.launchCoffee(
                                CoffeeEvent(
                                    launcherId = Profile.id(context), launcherName = Profile.name(context),
                                    launcherPhoto = Profile.photo64(context),
                                    barName = query, barLat = la, barLng = ln,
                                    minutes = minutes, mode = mode
                                )
                            )
                            if (mode == "CERCHIA") nav.goFresh("inviteCircle/$id")
                            else nav.goFresh("launched/$id")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Errore nel lancio: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) { Text("Lancia Extreme Coffee · $minutes min", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ModeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
