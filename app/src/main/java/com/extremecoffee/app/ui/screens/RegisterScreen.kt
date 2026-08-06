@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.app.Activity
import com.extremecoffee.app.data.CoffeeRepository
import com.extremecoffee.app.data.LocaleManager
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
    // Numero diviso in due: prefisso (menu a tendina) + numero nazionale (box separato).
    // In modifica profilo pre-compilo entrambi a partire dal numero già salvato; per un nuovo
    // utente il prefisso parte già impostato in base al paese del dispositivo.
    val initialPhoneSplit = remember {
        if (editMode) Phones.splitForEdit(Profile.phone(context))
        else Phones.dialCodeForRegion(Phones.defaultRegion()) to ""
    }
    var dialCode by remember { mutableStateOf(initialPhoneSplit.first) }
    var phoneNational by remember { mutableStateOf(initialPhoneSplit.second) }
    val phone = dialCode + phoneNational
    // Punto 4: scelta lingua alla registrazione (solo per nuovi utenti; chi ha già un profilo
    // cambia lingua dalle Impostazioni, per non alterare il flusso di modifica esistente).
    var selectedLang by remember { mutableStateOf(LocaleManager.getLang(context)) }
    fun switchLang(lang: String) {
        if (selectedLang == lang) return
        selectedLang = lang
        LocaleManager.setLang(context, lang)
        (context as? Activity)?.recreate()
    }
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
        modifier = Modifier.fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        if (!editMode) {
            Text(
                stringResource(R.string.reg_lang_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().testTag("reg_lang_row")) {
                FilterChip(
                    selected = selectedLang == "it",
                    onClick = { switchLang("it") },
                    label = { Text(stringResource(R.string.lang_italian)) },
                    modifier = Modifier.weight(1f).testTag("reg_lang_it")
                )
                Spacer(Modifier.width(10.dp))
                FilterChip(
                    selected = selectedLang == "en",
                    onClick = { switchLang("en") },
                    label = { Text(stringResource(R.string.lang_english)) },
                    modifier = Modifier.weight(1f).testTag("reg_lang_en")
                )
            }
            Spacer(Modifier.height(20.dp))
        }
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
            modifier = Modifier.fillMaxWidth().testTag("reg_nickname")
        )

        Spacer(Modifier.height(12.dp))
        var showCountryPicker by remember { mutableStateOf(false) }
        var countrySearch by remember { mutableStateOf("") }
        val allDialCodes = remember { Phones.allDialCodes() }
        val selectedCountryName = remember(dialCode) {
            allDialCodes.firstOrNull { it.code == dialCode }?.name
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Box 1: prefisso, apre il menu a tendina di selezione paese
            Surface(
                onClick = { countrySearch = ""; showCountryPicker = true },
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.width(108.dp).height(58.dp).testTag("reg_dialcode")
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dialCode,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF241309),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Box 2: numero nazionale (es. 3935672548)
            OutlinedTextField(
                value = phoneNational,
                onValueChange = { phoneNational = it.filter { ch -> ch.isDigit() || ch == ' ' }; error = null },
                label = { Text(stringResource(R.string.reg_phone_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = fieldColors,
                modifier = Modifier.weight(1f).testTag("reg_phone")
            )
        }
        // Nome del paese selezionato, per conferma visiva sotto ai due box.
        if (selectedCountryName != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                selectedCountryName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (normPhone != null) stringResource(R.string.reg_phone_recognised, normPhone)
            else stringResource(R.string.reg_phone_help1) +
                stringResource(R.string.reg_phone_help2),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )

        if (showCountryPicker) {
            AlertDialog(
                onDismissRequest = { showCountryPicker = false },
                title = { Text(stringResource(R.string.reg_country_pick_title)) },
                text = {
                    Column(Modifier.heightIn(max = 420.dp)) {
                        OutlinedTextField(
                            value = countrySearch,
                            onValueChange = { countrySearch = it },
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.reg_country_search)) },
                            modifier = Modifier.fillMaxWidth().testTag("reg_country_search")
                        )
                        Spacer(Modifier.height(10.dp))
                        val filtered = remember(countrySearch, allDialCodes) {
                            if (countrySearch.isBlank()) allDialCodes
                            else allDialCodes.filter {
                                it.name.contains(countrySearch, ignoreCase = true) ||
                                    it.code.contains(countrySearch)
                            }
                        }
                        LazyColumn {
                            items(filtered) { dc ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            dialCode = dc.code
                                            showCountryPicker = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dc.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Text(dc.code, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCountryPicker = false }) { Text(stringResource(R.string.common_cancel)) }
                }
            )
        }

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
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("reg_submit"),
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
