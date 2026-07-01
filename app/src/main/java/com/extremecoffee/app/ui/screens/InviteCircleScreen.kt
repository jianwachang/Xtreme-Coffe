@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)

package com.extremecoffee.app.ui.screens

import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.extremecoffee.app.R
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import com.extremecoffee.app.model.AppUser
import com.extremecoffee.app.ui.CoffeeScaffold
import com.extremecoffee.app.ui.goFresh
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

@Composable
fun InviteCircleScreen(nav: NavController, eventId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val perm = rememberPermissionState(android.Manifest.permission.READ_CONTACTS)

    var contacts by remember { mutableStateOf(Profile.cachedContacts(context)) }
    var registered by remember { mutableStateOf(Profile.cachedRegisteredUsers(context)) }
    var invited by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var checkingFirst by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val downloadUrl by CoffeeRepository.downloadUrl().collectAsState()

    LaunchedEffect(perm.status.isGranted) {
        if (perm.status.isGranted) {
            if (contacts.isEmpty()) loading = true   // spinner SOLO se non ho nulla in cache
            val fresh = withContext(Dispatchers.IO) { readContacts(context) }
            contacts = fresh
            Profile.setCachedContacts(context, fresh)
            loading = false
        } else perm.launchPermissionRequest()
    }

    // Aggiornamento "chi ha l'app" OTTIMIZZATO (come Invita amici): completo max ogni 3h,
    // altrimenti solo i contatti nuovi, altrimenti nessuna rete. Conteggio sempre aggiornato.
    LaunchedEffect(contacts) {
        if (contacts.isEmpty()) return@LaunchedEffect
        val allPhones = contacts.mapNotNull { Phones.normalizeIt(it.phone) ?: Phones.normalizeIt(it.raw) }.toSet()
        val firstTime = !Profile.regChecked(context, "users")
        val now = System.currentTimeMillis()
        val stale = now - Profile.regRefreshedAt(context, "users") >= 3L * 60 * 60 * 1000
        val newPhones = allPhones - Profile.checkedPhones(context, "users")

        val fullCheck = firstTime || stale
        val toQuery = when {
            fullCheck -> allPhones
            newPhones.isNotEmpty() -> newPhones
            else -> emptySet()
        }
        if (toQuery.isEmpty()) return@LaunchedEffect

        if (firstTime) checkingFirst = true
        val found = withContext(Dispatchers.IO) { CoffeeRepository.findRegistered(toQuery.toList()) }
        val updated = if (fullCheck) {
            Profile.setRegRefreshedAt(context, "users", now)
            found
        } else {
            registered.filterKeys { it in allPhones } + found
        }
        registered = updated
        Profile.setCachedRegisteredUsers(context, updated)
        Profile.setCheckedPhones(context, "users", allPhones)
        Profile.setRegChecked(context, "users")
        checkingFirst = false
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

    fun inviteInApp(c: Contact, user: AppUser) {
        scope.launch {
            CoffeeRepository.inviteUserToEvent(eventId, user.id)
            invited = invited + (normOf(c) ?: c.phone)
            Toast.makeText(context, context.getString(R.string.ic_sent, c.name), Toast.LENGTH_SHORT).show()
        }
    }

    fun hasAppOf(c: Contact): Boolean = normOf(c)?.let { it in registered } == true

    var filter by remember { mutableStateOf("all") } // "all" | "app" | "invite"

    val appCount = remember(contacts, registered) { contacts.count { hasAppOf(it) } }
    val inviteCount = remember(contacts, registered) { contacts.size - appCount }

    val filtered = remember(contacts, registered, search, filter) {
        val digits = search.filter { it.isDigit() }
        contacts
            .filter { c ->
                search.isBlank() ||
                    c.name.contains(search, ignoreCase = true) ||
                    (digits.isNotEmpty() && c.phone.contains(digits))
            }
            .filter { c ->
                when (filter) {
                    "app" -> hasAppOf(c)
                    "invite" -> !hasAppOf(c)
                    else -> true
                }
            }
            // chi ha già l'app va in cima; poi ordine alfabetico
            .sortedWith(
                compareByDescending<Contact> { hasAppOf(it) }.thenBy { it.name.lowercase() }
            )
    }

    CoffeeScaffold(stringResource(R.string.ic_title), nav, "inviteCircle/$eventId") { mod ->
        Column(mod.fillMaxSize().padding(horizontal = 16.dp)) {

            Text(
                stringResource(R.string.ic_intro1) +
                    stringResource(R.string.ic_intro2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            if (perm.status.isGranted) {
                if (contacts.isNotEmpty()) {
                    if (checkingFirst) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.ic_checking), style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                    Text(
                        stringResource(R.string.ic_count, appCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { filter = "app" }
                            .padding(bottom = 6.dp)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filter == "all",
                            onClick = { filter = "all" },
                            label = { Text(stringResource(R.string.if_filter_all)) }
                        )
                        FilterChip(
                            selected = filter == "app",
                            onClick = { filter = "app" },
                            label = { Text(stringResource(R.string.if_filter_app, appCount)) }
                        )
                        FilterChip(
                            selected = filter == "invite",
                            onClick = { filter = "invite" },
                            label = { Text(stringResource(R.string.if_filter_invite, inviteCount)) }
                        )
                    }
                    }
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
                loading && contacts.isEmpty() -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() }
                filtered.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f).padding(24.dp), Alignment.Center) {
                    Text(stringResource(R.string.if_empty_filter),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(Modifier.weight(1f)) {
                    items(filtered) { c ->
                        val user = normOf(c)?.let { registered[it] }
                        ContactRow(
                            contact = c,
                            hasApp = user != null,
                            invited = (normOf(c) ?: c.phone) in invited,
                            onInvitaApp = { user?.let { inviteInApp(c, it) } },
                            onWhatsapp = { openWhatsapp(c) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Button(
                onClick = { nav.goFresh("launched/$eventId") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(52.dp),
                shape = MaterialTheme.shapes.large
            ) { Text(stringResource(R.string.ic_goto_status), fontWeight = FontWeight.Bold) }
        }
    }
}

/** Riga contatto. Se ha l'app -> invito in-app; altrimenti -> link WhatsApp. */
@Composable
private fun ContactRow(
    contact: Contact,
    hasApp: Boolean,
    invited: Boolean,
    onInvitaApp: () -> Unit,
    onWhatsapp: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(contact.raw, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hasApp) {
                Text(stringResource(R.string.ic_has_app), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        if (invited) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ic_invited), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        } else if (hasApp) {
            FilledTonalButton(
                onClick = onInvitaApp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) { Text(stringResource(R.string.ic_invite)) }
        } else {
            OutlinedButton(
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
