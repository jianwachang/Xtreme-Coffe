package com.extremecoffee.app.ui.screens

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
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.LeaderEntry
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.data.readContacts
import com.extremecoffee.app.ui.CoffeeScaffold
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
    var entries by remember { mutableStateOf<List<LeaderEntry>?>(null) }
    var metric by remember { mutableStateOf(0) } // 0=totale 1=lanciati 2=partecipati

    LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) {
            val contacts = withContext(Dispatchers.IO) { readContacts(context) }
            val phones = contacts.mapNotNull { Phones.normalizeIt(it.phone) ?: Phones.normalizeIt(it.raw) }
            val registered = withContext(Dispatchers.IO) { CoffeeRepository.findRegistered(phones) }
            val users = LinkedHashMap<String, String>()
            users[Profile.id(context)] = Profile.name(context).ifBlank { "Tu" }
            registered.values.take(25).forEach { u -> if (u.id.isNotBlank()) users[u.id] = u.name }
            entries = withContext(Dispatchers.IO) {
                CoffeeRepository.loadLeaderboard(users.map { it.key to it.value })
            }
        } else perm.launchPermissionRequest()
    }

    CoffeeScaffold("Classifica amici", nav, "leaderboard") { mod ->
        Column(mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            val labels = listOf("Totale", "Lanciati", "Partecipati")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEachIndexed { i, l ->
                    val sel = metric == i
                    Surface(onClick = { metric = i }, shape = MaterialTheme.shapes.small,
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)) {
                        Text(l, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            val e = entries
            when {
                !perm.status.isGranted -> Text(
                    "Per la classifica servono i contatti: confronto solo chi ha già l'app. Concedi il permesso per continuare.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                e == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Calcolo la classifica\u2026")
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
                                Text(medal, modifier = Modifier.width(40.dp),
                                    style = MaterialTheme.typography.titleMedium)
                                Text(le.name.ifBlank { "Anonimo" }, modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(value.toString(), fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    if (ranked.size <= 1) {
                        Spacer(Modifier.height(10.dp))
                        Text("Quando i tuoi contatti useranno l'app, li vedrai qui in classifica.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
