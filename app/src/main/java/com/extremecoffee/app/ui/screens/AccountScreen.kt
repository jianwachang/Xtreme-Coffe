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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.R
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
                Toast.makeText(context, context.getString(R.string.account_toast_photo_ok), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.account_toast_photo_fail), Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun editPhoto() = pickImage.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    val name = Profile.name(context)
    val profileFallback = stringResource(R.string.account_profile)
    val phone = Phones.normalizeIt(Profile.phone(context)) ?: Profile.phone(context)

    TabScaffold(stringResource(R.string.nav_account), nav, "account") { mod ->
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
                        Text(name.ifBlank { profileFallback }, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        if (phone.isNotBlank())
                            Text(phone, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            AccountRow(Icons.Filled.PhotoCamera, stringResource(R.string.account_change_photo), stringResource(R.string.account_change_photo_sub)) { editPhoto() }
            AccountRow(Icons.Filled.Edit, stringResource(R.string.account_edit), stringResource(R.string.account_edit_sub)) { nav.navigate("register") }
            AccountRow(Icons.AutoMirrored.Filled.ExitToApp, stringResource(R.string.account_exit), stringResource(R.string.account_exit_sub)) { showExit = true }

            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.account_info_section), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            AccountRow(Icons.Filled.Language, stringResource(R.string.account_language), stringResource(R.string.account_language_sub)) { nav.navigate("language") }
            AccountRow(Icons.Filled.Lock, stringResource(R.string.account_privacy), stringResource(R.string.account_privacy_sub)) { nav.navigate("privacy") }
            AccountRow(Icons.Filled.Description, stringResource(R.string.account_terms), stringResource(R.string.account_terms_sub)) { nav.navigate("terms") }
            AccountRow(Icons.Filled.Shield, stringResource(R.string.account_pp), stringResource(R.string.account_pp_sub)) { nav.navigate("privacyPolicy") }

            Spacer(Modifier.height(10.dp))
            AccountRow(Icons.Filled.Delete, stringResource(R.string.account_delete),
                stringResource(R.string.account_delete_sub), error = true) { showDelete = true }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.account_delete_title)) },
            text = { Text(stringResource(R.string.account_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch {
                        CoffeeRepository.deleteMyAccountAndData(context)
                        nav.goFresh("register")
                    }
                }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text(stringResource(R.string.account_exit_title)) },
            text = { Text(stringResource(R.string.account_exit_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showExit = false
                    (context as? Activity)?.finishAffinity()
                }) { Text(stringResource(R.string.common_exit)) }
            },
            dismissButton = { TextButton(onClick = { showExit = false }) { Text(stringResource(R.string.common_cancel)) } }
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
