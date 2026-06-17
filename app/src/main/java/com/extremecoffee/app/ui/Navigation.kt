package com.extremecoffee.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

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
