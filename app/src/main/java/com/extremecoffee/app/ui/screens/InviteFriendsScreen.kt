@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)

package com.extremecoffee.app.ui.screens

import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.extremecoffee.app.R
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.Config
import com.extremecoffee.app.data.Contact
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.data.readContacts
import com.extremecoffee.app.ui.CoffeeScaffold
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

@Composable
fun InviteFriendsScreen(nav: NavController) {
    val context = LocalContext.current
    val perm = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)

    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var registered by remember { mutableStateOf<Set<String>>(emptySet()) }
    var invited by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val downloadUrl by CoffeeRepository.downloadUrl().collectAsState()

    LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) {
            loading = true
            contacts = withContext(Dispatchers.IO) { readContacts(context) }
            loading = false
        } else perm.launchPermissionRequest()
    }

    // Quali contatti hanno già l'app (registro "users")
    LaunchedEffect(contacts) {
        if (contacts.isNotEmpty()) {
            checking = true
            val phones = contacts.mapNotNull { Phones.normalizeIt(it.phone) ?: Phones.normalizeIt(it.raw) }
            registered = withContext(Dispatchers.IO) { CoffeeRepository.findRegisteredPhones(phones) }
            checking = false
        }
    }

    fun normOf(c: Contact): String? = Phones.normalizeIt(c.phone) ?: Phones.normalizeIt(c.raw)

    fun openWhatsapp(c: Contact) {
        val num = normOf(c)?.drop(1) ?: c.phone.filter { it.isDigit() }
        val msg = URLEncoder.encode(Config.inviteMessage(Profile.name(context), downloadUrl), "UTF-8")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num?text=$msg")))
            invited = invited + (normOf(c) ?: c.phone)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.wa_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    val filtered = remember(contacts, search) {
        if (search.isBlank()) contacts
        else {
            val digits = search.filter { it.isDigit() }
            contacts.filter {
                it.name.contains(search, ignoreCase = true) ||
                    (digits.isNotEmpty() && it.phone.contains(digits))
            }
        }
    }
    val appCount = remember(contacts, registered) {
        contacts.count { normOf(it)?.let { n -> n in registered } == true }
    }

    CoffeeScaffold(stringResource(R.string.if_title), nav, "inviteFriends") { mod ->
        Column(mod.fillMaxSize().padding(horizontal = 16.dp)) {

            Text(
                stringResource(R.string.if_intro1) +
                    stringResource(R.string.if_intro2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            if (perm.status.isGranted) {
                if (checking) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.if_checking), style = MaterialTheme.typography.labelMedium)
                    }
                } else if (contacts.isNotEmpty()) {
                    Text(
                        stringResource(R.string.if_count, appCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    label = { Text(stringResource(R.string.ic_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }

            when {
                !perm.status.isGranted -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.ic_perm))
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { perm.launchPermissionRequest() }) { Text(stringResource(R.string.ic_perm_btn)) }
                    }
                }
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() }
                else -> LazyColumn(Modifier.weight(1f)) {
                    items(filtered) { c ->
                        val hasApp = normOf(c)?.let { it in registered } == true
                        FriendRow(
                            contact = c,
                            hasApp = hasApp,
                            invited = (normOf(c) ?: c.phone) in invited,
                            onWhatsapp = { openWhatsapp(c) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * Riga contatto per stringResource(R.string.if_title).
 * - Ha già l'app  -> etichetta informativa, NESSUN invito.
 * - Non ha l'app  -> pulsante WhatsApp per mandare il link.
 */
@Composable
private fun FriendRow(
    contact: Contact,
    hasApp: Boolean,
    invited: Boolean,
    onWhatsapp: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(contact.raw, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hasApp) {
                Text(stringResource(R.string.if_has_app), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        when {
            hasApp -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.if_subscribed), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            invited -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ic_invited), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            else -> OutlinedButton(
                onClick = onWhatsapp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, tint = Color(0xFF25D366))
                Spacer(Modifier.width(6.dp))
                Text("WhatsApp")
            }
        }
    }
}
