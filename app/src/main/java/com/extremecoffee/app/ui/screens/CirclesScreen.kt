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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.data.Circle
import com.extremecoffee.app.data.CircleMember
import com.extremecoffee.app.data.Circles
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.readContacts
import com.extremecoffee.app.ui.CoffeeScaffold
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CirclesScreen(nav: NavController) {
    val context = LocalContext.current
    val perm = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)
    var appContacts by remember { mutableStateOf<List<CircleMember>?>(null) }
    var circles by remember { mutableStateOf(Circles.all(context)) }
    var name by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }

    LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) {
            val contacts = withContext(Dispatchers.IO) { readContacts(context) }
            val phones = contacts.mapNotNull { Phones.normalizeIt(it.phone) ?: Phones.normalizeIt(it.raw) }
            val registered = withContext(Dispatchers.IO) { CoffeeRepository.findRegistered(phones) }
            appContacts = registered.values.filter { it.id.isNotBlank() }
                .map { CircleMember(it.id, it.name) }.distinctBy { it.id }
        } else perm.launchPermissionRequest()
    }

    CoffeeScaffold("Le tue cerchie", nav, "circles") { mod ->
        Column(mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Crea una cerchia", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it.take(24) },
                label = { Text("Nome (es. Colleghi)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text("Membri (tuoi contatti con l'app)",
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            val contacts = appContacts
            when {
                !perm.status.isGranted -> Text("Concedi l'accesso ai contatti per scegliere i membri.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                contacts == null -> Text("Carico i contatti\u2026", style = MaterialTheme.typography.bodySmall)
                contacts.isEmpty() -> Text("Nessuno dei tuoi contatti ha ancora l'app.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> contacts.forEach { m ->
                    val sel = selected.contains(m.id)
                    Surface(onClick = { if (sel) selected.remove(m.id) else selected.add(m.id) },
                        shape = MaterialTheme.shapes.large,
                        color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (sel) "\u2611" else "\u2610",
                                modifier = Modifier.padding(end = 10.dp),
                                style = MaterialTheme.typography.titleMedium)
                            Text(m.name.ifBlank { "Anonimo" }, modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(enabled = name.isNotBlank() && selected.isNotEmpty(),
                onClick = {
                    val members = (appContacts ?: emptyList()).filter { selected.contains(it.id) }
                    Circles.add(context, Circle(System.currentTimeMillis().toString(), name.trim(), members))
                    circles = Circles.all(context); name = ""; selected.clear()
                }, modifier = Modifier.fillMaxWidth()) { Text("Crea cerchia") }

            Spacer(Modifier.height(24.dp))
            Text("Le tue cerchie", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (circles.isEmpty()) {
                Text("Nessuna cerchia. Creane una qui sopra.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                circles.forEach { c ->
                    Surface(shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text("${c.members.size} membri", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                Circles.remove(context, c.id); circles = Circles.all(context)
                            }) { Text("Elimina") }
                        }
                    }
                }
            }
        }
    }
}
