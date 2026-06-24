package com.extremecoffee.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.model.CoffeeEvent
import com.extremecoffee.app.ui.TabScaffold
import com.extremecoffee.app.ui.decodeAvatar
import com.extremecoffee.app.ui.goFresh

@Composable
fun NotificationsScreen(nav: NavController) {
    val context = LocalContext.current
    val myId = remember { Profile.id(context) }
    val invites by CoffeeRepository.allMyInvites(myId).collectAsState(initial = emptyList())

    TabScaffold("Notifiche", nav, "notifications") { mod ->
        if (invites.isEmpty()) {
            Box(mod.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.NotificationsNone, contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Nessuna notifica", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Qui trovi gli Extreme Coffee a cui ti hanno invitato i tuoi amici.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = mod.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(invites) { e ->
                    NotificationCard(e) { nav.goFresh("invite/${e.id}") }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(e: CoffeeEvent, onOpen: () -> Unit) {
    val now = System.currentTimeMillis()
    val remaining = e.remainingMillis(now)
    val status = when {
        e.cancelled -> "Annullato"
        remaining <= 0 -> "Scaduto"
        else -> "Attivo"
    }
    val active = status == "Attivo"
    val statusColor = when (status) {
        "Attivo" -> MaterialTheme.colorScheme.primary
        "Annullato" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onOpen,
        enabled = active,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val av = remember(e.launcherPhoto) { decodeAvatar(e.launcherPhoto) }
            if (av != null) {
                Image(
                    av.asImageBitmap(), contentDescription = e.launcherName,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) { Text("\u2615", fontSize = 20.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${e.launcherName.ifBlank { "Qualcuno" }} ti ha invitato",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        if (e.barName.isNotBlank()) append(e.barName + " \u2022 ")
                        append(relativeTime(now - e.createdAt))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                status, style = MaterialTheme.typography.labelMedium,
                color = statusColor, fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun relativeTime(diffMillis: Long): String {
    val m = diffMillis / 60000
    return when {
        m < 1 -> "ora"
        m < 60 -> "$m min fa"
        m < 1440 -> "${m / 60} h fa"
        else -> "${m / 1440} g fa"
    }
}
