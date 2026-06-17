@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.extremecoffee.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.data.RegisterResult
import com.extremecoffee.app.ui.goFresh
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myId = remember { Profile.id(context) }

    var nickname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val normPhone = Phones.normalizeIt(phone)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text("\u2615", fontSize = 56.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Crea il tuo profilo",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Ti serve una sola volta. Scegli un nickname e metti il numero: " +
                "così gli amici ti riconoscono e ricevi gli inviti direttamente nell'app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it; error = null },
            label = { Text("Nickname (unico)") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            supportingText = { Text("Da 3 a 20 caratteri: lettere, numeri, . _ -") },
            isError = error != null,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; error = null },
            label = { Text("Numero di telefono") },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            supportingText = {
                Text(
                    if (normPhone != null) "Riconosciuto come $normPhone"
                    else "Inserisci il tuo numero di cellulare"
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !loading && nickname.isNotBlank() && normPhone != null,
            onClick = {
                scope.launch {
                    loading = true; error = null
                    when (val r = CoffeeRepository.registerOnce(nickname, phone, myId)) {
                        is RegisterResult.Success -> {
                            Profile.setName(context, r.nickname)
                            Profile.setPhone(context, r.phone)
                            Profile.setRegistered(context, true)
                            CoffeeRepository.registerMe(r.phone, myId, r.nickname)
                            nav.goFresh("home")
                        }
                        RegisterResult.NicknameTaken ->
                            error = "Questo nickname \u00e8 gi\u00e0 preso, scegline un altro."
                        RegisterResult.InvalidNickname ->
                            error = "Nickname non valido (3\u201320 caratteri: lettere, numeri, . _ -)."
                        RegisterResult.InvalidPhone ->
                            error = "Numero di telefono non valido."
                        RegisterResult.Error ->
                            error = "Qualcosa \u00e8 andato storto. Controlla la connessione e riprova."
                    }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (loading) CircularProgressIndicator(
                strokeWidth = 2.dp, modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            ) else Text("Crea profilo", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Il numero serve solo a farti trovare dai tuoi contatti che hanno l'app. " +
                "Non viene mostrato pubblicamente.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
