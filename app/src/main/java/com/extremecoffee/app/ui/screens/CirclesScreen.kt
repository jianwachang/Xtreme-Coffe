package com.extremecoffee.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.extremecoffee.app.R
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
    val anonLabel = stringResource(R.string.anon)
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

    CoffeeScaffold(stringResource(R.string.cir_title), nav, "circles") { mod ->
        Column(mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.cir_create), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it.take(24) },
                label = { Text(stringResource(R.string.cir_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.cir_members),
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            val contacts = appContacts
            when {
                !perm.status.isGranted -> Text(stringResource(R.string.cir_perm),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                contacts == null -> Text(stringResource(R.string.cir_loading), style = MaterialTheme.typography.bodySmall)
                contacts.isEmpty() -> Text(stringResource(R.string.cir_none_contacts),
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
                            Text(m.name.ifBlank { anonLabel }, modifier = Modifier.weight(1f),
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
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cir_create_btn)) }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.cir_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (circles.isEmpty()) {
                Text(stringResource(R.string.cir_empty),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                circles.forEach { c ->
                    Surface(shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.cir_members_count, c.members.size), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                Circles.remove(context, c.id); circles = Circles.all(context)
                            }) { Text(stringResource(R.string.common_delete)) }
                        }
                    }
                }
            }
        }
    }
}
