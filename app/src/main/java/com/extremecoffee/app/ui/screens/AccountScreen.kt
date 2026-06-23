package com.extremecoffee.app.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.Phones
import com.extremecoffee.app.data.Profile
import com.extremecoffee.app.ui.TabScaffold
import com.extremecoffee.app.ui.goFresh
import com.extremecoffee.app.ui.makeAvatarBase64
import com.extremecoffee.app.ui.saveProfilePhoto
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoPath by remember { mutableStateOf(Profile.photoPath(context)) }
    var photoVersion by remember { mutableStateOf(0) }
    var showDelete by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val saved = saveProfilePhoto(context, uri)
            if (saved != null) {
                Profile.setPhotoPath(context, saved)
                Profile.setPhoto64(context, makeAvatarBase64(saved))
                photoPath = saved; photoVersion++
                Toast.makeText(context, "Foto profilo aggiornata", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Impossibile usare questa immagine", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun editPhoto() = pickImage.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    val name = Profile.name(context)
    val phone = Phones.normalizeIt(Profile.phone(context)) ?: Profile.phone(context)

    TabScaffold("Account", nav, "account") { mod ->
        Column(mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            // Intestazione profilo
            Surface(shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val bmp = remember(photoPath, photoVersion) {
                        if (photoPath != null) BitmapFactory.decodeFile(photoPath) else null
                    }
                    if (bmp != null) {
                        Image(bmp.asImageBitmap(), contentDescription = "Foto profilo",
                            modifier = Modifier.size(64.dp).clip(CircleShape).clickable { editPhoto() },
                            contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.size(64.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface).clickable { editPhoto() },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name.ifBlank { "Profilo" }, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        if (phone.isNotBlank())
                            Text(phone, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            AccountRow(Icons.Filled.PhotoCamera, "Cambia foto", "Aggiorna la tua immagine profilo") { editPhoto() }
            AccountRow(Icons.Filled.Edit, "Modifica profilo", "Nickname e numero di telefono") { nav.navigate("register") }
            AccountRow(Icons.AutoMirrored.Filled.ExitToApp, "Esci dall'app", "Chiudi Extreme Coffee") { showExit = true }

            Spacer(Modifier.height(10.dp))
            AccountRow(Icons.Filled.Delete, "Elimina dati e account",
                "Rimuove profilo, nickname e dati. Definitivo.", error = true) { showDelete = true }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Eliminare account e dati?") },
            text = { Text("Verranno rimossi il tuo profilo, il nickname e i dati associati. L'azione \u00e8 definitiva.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        CoffeeRepository.deleteMyAccountAndData(context)
                        nav.goFresh("register")
                    }
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Annulla") } }
        )
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text("Uscire dall'app?") },
            text = { Text("Extreme Coffee verr\u00e0 chiusa. I tuoi dati restano salvati sul dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    showExit = false
                    (context as? Activity)?.finishAffinity()
                }) { Text("Esci") }
            },
            dismissButton = { TextButton(onClick = { showExit = false }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun AccountRow(icon: ImageVector, title: String, subtitle: String, error: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null,
                tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("\u203A", style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
