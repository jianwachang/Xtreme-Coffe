@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.extremecoffee.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.ui.res.stringResource
import com.extremecoffee.app.R
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
import androidx.compose.ui.graphics.Color
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
import com.extremecoffee.app.ui.makeAvatarBase64
import com.extremecoffee.app.ui.saveProfilePhoto
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myId = remember { Profile.id(context) }

    val editMode = remember { Profile.isRegistered(context) }
    var nickname by remember { mutableStateOf(if (editMode) Profile.name(context) else "") }
    var phone by remember { mutableStateOf(if (editMode) Profile.phone(context) else "") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val normPhone = Phones.normalizeIt(phone)
    var photoPath by remember { mutableStateOf(Profile.photoPath(context)) }
    var photoVersion by remember { mutableStateOf(0) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val saved = saveProfilePhoto(context, uri)
            if (saved != null) {
                photoPath = saved
                photoVersion++
                Profile.setPhotoPath(context, saved)
                Profile.setPhoto64(context, makeAvatarBase64(saved))
            }
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
                val bmp = remember(pp, photoVersion) { if (pp != null) BitmapFactory.decodeFile(pp) else null }
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), contentDescription = stringResource(R.string.reg_photo_cd),
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null,
                        modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (photoPath != null) stringResource(R.string.reg_photo_change) else stringResource(R.string.reg_photo_add),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (editMode) stringResource(R.string.reg_title_edit) else stringResource(R.string.reg_title_new),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            if (editMode) stringResource(R.string.reg_sub_edit)
            else stringResource(R.string.reg_sub_new1) +
                stringResource(R.string.reg_sub_new2),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF241309),
            unfocusedTextColor = Color(0xFF241309),
            disabledTextColor = Color(0xFF241309),
            cursorColor = Color(0xFF241309)
        )
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it; error = null },
            label = { Text(stringResource(R.string.reg_nick_label)) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            supportingText = { Text(stringResource(R.string.reg_nick_help)) },
            isError = error != null,
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; error = null },
            label = { Text(stringResource(R.string.reg_phone_label)) },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            supportingText = {
                Text(
                    if (normPhone != null) stringResource(R.string.reg_phone_recognised, normPhone)
                    else stringResource(R.string.reg_phone_help1) +
                        stringResource(R.string.reg_phone_help2)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))

        val submit: () -> Unit = {
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
                        error = context.getString(R.string.reg_err_taken)
                    RegisterResult.InvalidNickname ->
                        error = context.getString(R.string.reg_err_invalid_nick)
                    RegisterResult.InvalidPhone ->
                        error = context.getString(R.string.reg_err_invalid_phone)
                    RegisterResult.Error ->
                        error = context.getString(R.string.reg_err_generic)
                }
                loading = false
            }
        }

        Button(
            enabled = !loading && nickname.isNotBlank() && normPhone != null,
            onClick = submit,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (loading) CircularProgressIndicator(
                strokeWidth = 2.dp, modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            ) else Text(if (editMode) stringResource(R.string.reg_save) else stringResource(R.string.reg_create), fontWeight = FontWeight.Bold)
        }

        if (editMode) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { nav.goFresh("home") },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.common_cancel), fontWeight = FontWeight.SemiBold)
            }
        }

    }
}

// saveProfilePhoto / makeAvatarBase64 sono ora condivise in ui/AvatarUtil.kt
