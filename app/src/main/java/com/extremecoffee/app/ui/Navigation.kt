package com.extremecoffee.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Profile

/** Pila per il tasto "avanti": viene riempita quando si preme "indietro". */
object NavHistory { val forward = mutableStateListOf<String>() }

/** Navigazione "nuova": azzera la pila avanti (come quando apri una pagina nuova nel browser). */
fun NavController.goFresh(route: String) {
    NavHistory.forward.clear()
    navigate(route)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeScaffold(
    title: String,
    nav: NavController,
    currentRoute: String,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        NavHistory.forward.add(currentRoute)
                        if (!nav.popBackStack()) nav.navigate("home")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(
                        enabled = NavHistory.forward.isNotEmpty(),
                        onClick = {
                            val r = NavHistory.forward.removeAt(NavHistory.forward.lastIndex)
                            nav.navigate(r)
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Avanti")
                    }
                    IconButton(onClick = {
                        NavHistory.forward.clear()
                        nav.navigate("home") { popUpTo("home") { inclusive = true } }
                    }) {
                        Icon(Icons.Filled.Home, contentDescription = "Home")
                    }
                }
            )
        }
    ) { padding -> content(Modifier.padding(padding)) }
}

/** Barra di navigazione inferiore condivisa tra le schede principali. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomBar(nav: NavController, selected: String) {
    val context = LocalContext.current
    val myId = remember { Profile.id(context) }
    val incoming by CoffeeRepository.incomingInvites(myId).collectAsState(initial = emptyList())
    val pending = incoming.size

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        NavigationBarItem(
            selected = selected == "home",
            onClick = { if (selected != "home") nav.goFresh("home") },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Home", maxLines = 1, softWrap = false, fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = selected == "radar",
            onClick = { if (selected != "radar") nav.goFresh("radar") },
            icon = { Icon(Icons.Filled.TrackChanges, contentDescription = null) },
            label = { Text("Radar", maxLines = 1, softWrap = false, fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = selected == "leaderboard",
            onClick = { if (selected != "leaderboard") nav.navigate("leaderboard") },
            icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
            label = { Text("Classifica", maxLines = 1, softWrap = false, fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = selected == "notifications",
            onClick = { if (selected != "notifications") nav.navigate("notifications") },
            icon = {
                BadgedBox(badge = {
                    if (pending > 0) Badge { Text(if (pending > 9) "9+" else pending.toString()) }
                }) { Icon(Icons.Filled.Notifications, contentDescription = null) }
            },
            label = { Text("Notifiche", maxLines = 1, softWrap = false, fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = selected == "account",
            onClick = { if (selected != "account") nav.navigate("account") },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Account", maxLines = 1, softWrap = false, fontSize = 10.sp) }
        )
    }
}

/** Scaffold per le schede principali: titolo serif (senza freccia indietro) + barra inferiore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScaffold(
    title: String,
    nav: NavController,
    selected: String,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = { AppBottomBar(nav, selected) }
    ) { padding -> content(Modifier.padding(padding)) }
}
