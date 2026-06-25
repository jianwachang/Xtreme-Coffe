package com.extremecoffee.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.res.stringResource
import com.extremecoffee.app.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.data.Circles
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.LeaderEntry
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.data.readContacts
import com.extremecoffee.app.ui.TabScaffold
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LeaderboardScreen(nav: NavController) {
    val context = LocalContext.current
    val perm = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)
    val circles = remember { Circles.all(context) }
    var selectedCircleId by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<LeaderEntry>?>(null) }
    var metric by remember { mutableStateOf(0) } // 0=totale 1=lanciati 2=partecipati

    LaunchedEffect(perm.status.isGranted, selectedCircleId) {
        val circle = circles.firstOrNull { it.id == selectedCircleId }
        if (circle == null && !perm.status.isGranted) { perm.launchPermissionRequest(); return@LaunchedEffect }
        entries = null
        val users = LinkedHashMap<String, String>()
        users[Profile.id(context)] = Profile.name(context).ifBlank { stringResource(R.string.lb_you) }
        if (circle != null) {
            circle.members.forEach { if (it.id.isNotBlank()) users[it.id] = it.name }
        } else {
            val contacts = withContext(Dispatchers.IO) { readContacts(context) }
            val phones = contacts.mapNotNull { Phones.normalizeIt(it.phone) ?: Phones.normalizeIt(it.raw) }
            val registered = withContext(Dispatchers.IO) { CoffeeRepository.findRegistered(phones) }
            registered.values.take(25).forEach { if (it.id.isNotBlank()) users[it.id] = it.name }
        }
        entries = withContext(Dispatchers.IO) { CoffeeRepository.loadLeaderboard(users.map { it.key to it.value }) }
    }

    TabScaffold(stringResource(R.string.nav_leaderboard), nav, "leaderboard") { mod ->
        Column(mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            // selettore cerchia
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectChip(stringResource(R.string.lb_all), selectedCircleId == null) { selectedCircleId = null }
                circles.forEach { c -> SelectChip(c.name, selectedCircleId == c.id) { selectedCircleId = c.id } }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { nav.navigate("circles") }) { Text(stringResource(R.string.lb_manage)) }
            Spacer(Modifier.height(8.dp))

            // selettore metrica
            val labels = listOf(stringResource(R.string.lb_total), stringResource(R.string.lb_launched), stringResource(R.string.lb_joined))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEachIndexed { i, l -> SelectChip(l, metric == i, Modifier.weight(1f)) { metric = i } }
            }
            Spacer(Modifier.height(16.dp))

            val e = entries
            when {
                selectedCircleId == null && !perm.status.isGranted -> Text(
                    stringResource(R.string.lb_need_contacts),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                e == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.lb_calc))
                }
                else -> {
                    val me = Profile.id(context)
                    val ranked = e.sortedByDescending {
                        when (metric) { 1 -> it.launched; 2 -> it.joined; else -> it.total }
                    }
                    ranked.forEachIndexed { idx, le ->
                        val value = when (metric) { 1 -> le.launched; 2 -> le.joined; else -> le.total }
                        val medal = when (idx) {
                            0 -> "\uD83E\uDD47"; 1 -> "\uD83E\uDD48"; 2 -> "\uD83E\uDD49"; else -> "${idx + 1}."
                        }
                        Surface(shape = MaterialTheme.shapes.large,
                            color = if (le.id == me) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(medal, modifier = Modifier.width(40.dp), style = MaterialTheme.typography.titleMedium)
                                Text(le.name.ifBlank { stringResource(R.string.anon) }, modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(value.toString(), fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (ranked.size <= 1) {
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.lb_empty),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier) {
        Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}
