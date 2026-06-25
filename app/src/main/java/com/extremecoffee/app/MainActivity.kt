@file:OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)

package com.extremecoffee.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.data.Reminders
import com.extremecoffee.app.data.RetentionScheduler
import com.google.firebase.auth.FirebaseAuth
import com.extremecoffee.app.ui.screens.*
import com.extremecoffee.app.ui.theme.ExtremeCoffeeTheme
import com.google.firebase.messaging.FirebaseMessaging
import com.extremecoffee.app.Notifier
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.extremecoffee.app.data.LocaleManager.wrap(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Accesso anonimo Firebase: necessario perché le regole Firestore richiedono un utente autenticato.
        runCatching {
            if (FirebaseAuth.getInstance().currentUser == null) {
                FirebaseAuth.getInstance().signInAnonymously()
            }
        }
        Notifier.ensureChannel(applicationContext)
        RetentionScheduler.schedule(applicationContext)
        Reminders.rescheduleAll(applicationContext)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoffeeRepository.saveFcmToken(Profile.id(this), token, Profile.name(this))
        }
        val incomingEventId = intent.getStringExtra("eventId")
        setContent { ExtremeCoffeeApp(incomingEventId) }
    }
}

@Composable
fun ExtremeCoffeeApp(incomingEventId: String?) {
    val context = LocalContext.current
    val nav = rememberNavController()
    val registered = remember { Profile.isRegistered(context) }
    val start = when {
        !registered -> "register"
        incomingEventId != null -> "invite/$incomingEventId"
        else -> "home"
    }

    // Permesso notifiche (Android 13+)
    if (Build.VERSION.SDK_INT >= 33) {
        val notifPerm = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) { if (!notifPerm.status.isGranted) notifPerm.launchPermissionRequest() }
    }

    // CICLO DI VITA DELLE NOTIFICHE DI INVITO (modalità cerchia):
    // - NASCE col push FCM (PushService.showRaw, con scadenza) nel momento in cui vieni invitato;
    // - si ESAURISCE da sola alla scadenza (setTimeoutAfter) o con il push di annullamento (cancelInvite).
    // Qui NON ricreiamo MAI le notifiche (era la causa della ricomparsa a ogni apertura):
    // facciamo solo da rete di sicurezza, CANCELLANDO quelle di inviti non più attivi (scaduti/annullati/rifiutati).
    val myId = remember { Profile.id(context) }
    val incoming by CoffeeRepository.incomingInvites(myId).collectAsState(initial = emptyList())
    val tracked = remember { mutableStateListOf<String>() }   // id inviti visti come attivi (solo per poterli cancellare)
    LaunchedEffect(incoming) {
        val now = System.currentTimeMillis()
        val activeIds = incoming.filter { it.remainingMillis(now) > 0 }.map { it.id }.toSet()
        activeIds.forEach { if (it !in tracked) tracked.add(it) }   // traccia soltanto: la notifica la crea il push
        tracked.toList().forEach { id ->
            if (id !in activeIds) { Notifier.cancelInvite(context, id); tracked.remove(id) }
        }
    }
    // Tick periodico: rimuove la notifica alla scadenza/annullamento anche senza aggiornamenti da Firestore.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            val now = System.currentTimeMillis()
            tracked.toList().forEach { id ->
                val ev = CoffeeRepository.eventById(id)
                if (ev == null || ev.cancelled || ev.remainingMillis(now) <= 0) {
                    Notifier.cancelInvite(context, id); tracked.remove(id)
                }
            }
        }
    }

    // Inviti già rifiutati: non devono riapparire
    LaunchedEffect(Unit) {
        CoffeeRepository.setDeclined(Profile.declined(context))
        CoffeeRepository.setJoined(Profile.joinedEvent(context))
    }

    // Mi registro nel registro "users" (se ho già messo il numero) così gli amici mi trovano
    LaunchedEffect(Unit) {
        val ph = Phones.normalizeIt(Profile.phone(context))
        if (!ph.isNullOrBlank()) CoffeeRepository.registerMe(ph, myId, Profile.name(context))
    }

    // Notifiche per CHI HA LANCIATO: risposte accetta/rifiuta degli invitati
    val responses by CoffeeRepository.responsesForMe(myId).collectAsState(initial = emptyList())
    val notifiedResp = remember { mutableStateListOf<String>() }
    var respSeeded by remember { mutableStateOf(false) }
    LaunchedEffect(responses) {
        if (!respSeeded) {
            responses.forEach { notifiedResp.add("${it.eventId}_${it.fromId}_${it.status}") }
            respSeeded = true
        } else {
            responses.forEach { r ->
                val k = "${r.eventId}_${r.fromId}_${r.status}"
                if (notifiedResp.add(k)) {
                    val ev = CoffeeRepository.eventById(r.eventId)
                    val exp = ev?.let { it.createdAt + it.minutes * 60_000L }
                    Notifier.showResponse(context, r, exp)
                }
            }
        }
    }

    ExtremeCoffeeTheme {
        NavHost(navController = nav, startDestination = start) {
            composable("register") { RegisterScreen(nav) }
            composable("home") { HomeScreen(nav) }
            composable("account") { AccountScreen(nav) }
            composable("language") { LanguageScreen(nav) }
            composable("privacy") { PrivacyScreen(nav) }
            composable("terms") { TermsScreen(nav) }
            composable("privacyPolicy") { PrivacyPolicyScreen(nav) }
            composable("launch") { LaunchCoffeeScreen(nav) }
            composable("inviteCircle/{eventId}") {
                InviteCircleScreen(nav, it.arguments?.getString("eventId") ?: "")
            }
            composable("launched/{eventId}") {
                LauncherStatusScreen(nav, it.arguments?.getString("eventId") ?: "")
            }
            composable("invite/{eventId}") {
                InvitePopupScreen(nav, it.arguments?.getString("eventId") ?: "")
            }
            composable("tracking/{eventId}/{role}") {
                TrackingScreen(
                    nav = nav,
                    eventId = it.arguments?.getString("eventId") ?: "",
                    isLauncher = it.arguments?.getString("role") == "launcher"
                )
            }
            composable("selfie/{eventId}") {
                SelfieCoffeeScreen(nav, it.arguments?.getString("eventId") ?: "")
            }
            composable("radar") { RadarScreen(nav) }
            composable("badges") { BadgesScreen(nav) }
            composable("recurring") { RecurringScreen(nav) }
            composable("leaderboard") { LeaderboardScreen(nav) }
            composable("notifications") { NotificationsScreen(nav) }
            composable("circles") { CirclesScreen(nav) }
            composable("inviteFriends") { InviteFriendsScreen(nav) }
        }
    }
}
