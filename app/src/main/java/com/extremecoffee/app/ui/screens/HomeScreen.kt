@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.extremecoffee.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.ui.goFresh
import com.extremecoffee.app.ui.decodeAvatar

@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val name = remember { Profile.name(context) }
    val myId = remember { Profile.id(context) }
    val phone = remember { Profile.phone(context) }
    val normPhone = Phones.normalizeIt(phone)
    val photoPath = remember { Profile.photoPath(context) }
    val incoming by CoffeeRepository.incomingInvites(myId).collectAsState(initial = emptyList())
    val myActive by CoffeeRepository.myActiveEvent(myId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { scope.launch { refreshing = true; delay(500); refreshing = false } },
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text("\u2615", fontSize = 56.sp)
        Spacer(Modifier.height(4.dp))
        Text("Extreme Coffee",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary)
        Text("Un caffè. Un timer. Zero scuse.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                val bmp = remember(photoPath) { if (photoPath != null) BitmapFactory.decodeFile(photoPath) else null }
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), contentDescription = "Foto profilo",
                        modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(normPhone ?: phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (incoming.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Hai ricevuto un Extreme Coffee!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            incoming.forEach { e ->
                Surface(
                    onClick = { nav.goFresh("invite/${e.id}") },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(Modifier.padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        val inv = remember(e.launcherPhoto) { decodeAvatar(e.launcherPhoto) }
                        if (inv != null) {
                            Image(inv.asImageBitmap(), contentDescription = e.launcherName,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop)
                        } else {
                            Text("\u2615", fontSize = 28.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${e.launcherName} ti invita",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold)
                            Text("${e.barName} · ${e.remainingMillis() / 60_000} min rimasti",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                        Icon(Icons.Filled.Person, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        val active = myActive
        ActionCard(
            if (active != null) "Extreme Coffee in corso" else "Lancia un Extreme Coffee",
            if (active != null) "Ne hai gi\u00e0 uno attivo \u00b7 vedi la mappa" else "Dai appuntamento, parte il timer",
            MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary
        ) { nav.goFresh(if (active != null) "launched/${active.id}" else "launch") }
        Spacer(Modifier.height(14.dp))
        ActionCard("Modalità nuove amicizie", "Trova chi prende un caffè vicino a te",
            MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) { nav.goFresh("radar") }
        Spacer(Modifier.height(14.dp))
        ActionCard("Invita i tuoi amici", "Falli scaricare l'app su WhatsApp",
            MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface) { nav.goFresh("inviteFriends") }
    }
    }
}

@Composable
private fun ActionCard(
    title: String, subtitle: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = content, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.8f))
        }
    }
}
