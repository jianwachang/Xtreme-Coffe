package com.extremecoffee.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.model.CoffeeEvent
import com.extremecoffee.app.model.ParticipantLocation
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.ui.CoffeeScaffold
import com.extremecoffee.app.ui.goFresh
import com.extremecoffee.app.ui.decodeAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InvitePopupScreen(nav: NavController, eventId: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val event by CoffeeRepository.eventFlow(eventId).collectAsState(initial = null)
    var remaining by remember { mutableStateOf(0L) }

    LaunchedEffect(event?.id) {
        while (true) { remaining = event?.remainingMillis() ?: 0L; delay(1_000) }
    }

    CoffeeScaffold("Invito", nav, "invite/$eventId") { mod ->
        Column(
            mod.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val e: CoffeeEvent? = event
            if (e == null) { CircularProgressIndicator(); return@Column }

            if (e.cancelled) {
                Text("\u274C", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Questo Extreme Coffee \u00e8 stato annullato",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { nav.goFresh("home") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) { Text("Torna alla home", fontWeight = FontWeight.Bold) }
                return@Column
            }

            val avatar = remember(e.launcherPhoto) { decodeAvatar(e.launcherPhoto) }
            if (avatar != null) {
                Image(
                    avatar.asImageBitmap(),
                    contentDescription = e.launcherName,
                    modifier = Modifier.size(104.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("\u2615\uD83D\uDD25", fontSize = 56.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${e.launcherName} ha lanciato un\nEXTREME COFFEE!!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))

            val min = (remaining / 60_000).coerceAtLeast(0)
            val sec = (remaining % 60_000 / 1_000).coerceAtLeast(0)
            Text(
                String.format("%02d:%02d", min, sec),
                fontSize = 72.sp, fontWeight = FontWeight.Black,
                color = if (remaining < 60_000) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.secondary
            )
            Text(
                "Hai ancora $min minuti per avere un caff\u00e8 GRATIS da ${e.barName}!",
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        CoffeeRepository.acceptInvite(
                            eventId, ParticipantLocation(userId = Profile.id(context), name = Profile.name(context), photo = Profile.photo64(context))
                        )
                        CoffeeRepository.sendResponse(e, Profile.id(context), Profile.name(context), "accepted")
                        nav.goFresh("tracking/$eventId/guest")
                    }
                },
                enabled = remaining > 0,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) { Text("\u2705  STO ARRIVANDO!", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = {
                scope.launch {
                    CoffeeRepository.declineLocally(e.id)
                    Profile.addDeclined(context, e.id)
                    CoffeeRepository.sendResponse(e, Profile.id(context), Profile.name(context), "declined")
                }
                nav.goFresh("home")
            }) { Text("Stavolta passo \uD83D\uDE34") }

            if (remaining <= 0) {
                Text("\u23F0 Tempo scaduto... il caff\u00e8 gratis \u00e8 volato!",
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
