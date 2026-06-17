@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.extremecoffee.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import java.io.File
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
    var photoPath by remember { mutableStateOf(Profile.photoPath(context)) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val saved = saveProfilePhoto(context, uri)
            if (saved != null) { photoPath = saved; Profile.setPhotoPath(context, saved) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(104.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val pp = photoPath
                val bmp = remember(pp) { if (pp != null) BitmapFactory.decodeFile(pp) else null }
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), contentDescription = "Foto profilo",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null,
                        modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (photoPath != null) "Tocca per cambiare la foto" else "Tocca per aggiungere una foto (opzionale)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
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

private fun saveProfilePhoto(context: android.content.Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, "profile").apply { mkdirs() }
    val out = File(dir, "avatar.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        out.outputStream().use { output -> input.copyTo(output) }
    }
    out.absolutePath
}.getOrNull()
