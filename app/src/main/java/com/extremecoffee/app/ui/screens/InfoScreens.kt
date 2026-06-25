package com.extremecoffee.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.R
import com.extremecoffee.app.data.LocaleManager
import com.extremecoffee.app.ui.CoffeeScaffold

/* ---------- helper di layout ---------- */

@Composable
private fun InfoScaffold(
    nav: NavController, route: String, title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    CoffeeScaffold(title, nav, route) { mod ->
        Column(mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            content()
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Para(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun H(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun NoteBox(text: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
    Spacer(Modifier.height(16.dp))
}

/* ---------- Lingua (con switch reale) ---------- */

@Composable
fun LanguageScreen(nav: NavController) {
    val context = LocalContext.current
    val current = remember { LocaleManager.getLang(context) }

    fun switchTo(lang: String) {
        if (LocaleManager.getLang(context) == lang) return
        LocaleManager.setLang(context, lang)
        (context as? Activity)?.recreate()
    }

    InfoScaffold(nav, "language", stringResource(R.string.account_language)) {
        Para(stringResource(R.string.lang_current))
        LangOption(stringResource(R.string.lang_italian), current == "it") { switchTo("it") }
        Spacer(Modifier.height(10.dp))
        LangOption(stringResource(R.string.lang_english), current == "en") { switchTo("en") }
        Spacer(Modifier.height(16.dp))
        NoteBox(stringResource(R.string.lang_note))
    }
}

@Composable
private fun LangOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

/* ---------- Privacy (permessi e dati) ---------- */

@Composable
fun PrivacyScreen(nav: NavController) {
    val context = LocalContext.current
    InfoScaffold(nav, "privacy", stringResource(R.string.account_privacy)) {
        Para(stringResource(R.string.privacy_intro))
        H(stringResource(R.string.privacy_perms_h))
        Para(stringResource(R.string.privacy_perms_body))
        Para(stringResource(R.string.privacy_perms_note))
        Button(
            onClick = {
                val i = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.privacy_open_settings)) }
        Spacer(Modifier.height(18.dp))
        H(stringResource(R.string.privacy_data_h))
        Para(stringResource(R.string.privacy_data_body))
        Para(stringResource(R.string.privacy_data_more))
    }
}

/* ---------- Termini e condizioni ---------- */

@Composable
fun TermsScreen(nav: NavController) {
    InfoScaffold(nav, "terms", stringResource(R.string.account_terms)) {
        Para(stringResource(R.string.terms_updated))
        Para(stringResource(R.string.terms_intro))
        H(stringResource(R.string.terms_1_h)); Para(stringResource(R.string.terms_1_b))
        H(stringResource(R.string.terms_2_h)); Para(stringResource(R.string.terms_2_b))
        H(stringResource(R.string.terms_3_h)); Para(stringResource(R.string.terms_3_b))
        H(stringResource(R.string.terms_4_h)); Para(stringResource(R.string.terms_4_b))
        H(stringResource(R.string.terms_5_h)); Para(stringResource(R.string.terms_5_b))
        H(stringResource(R.string.terms_6_h)); Para(stringResource(R.string.terms_6_b))
        NoteBox(stringResource(R.string.terms_note))
    }
}

/* ---------- Informativa sulla privacy ---------- */

@Composable
fun PrivacyPolicyScreen(nav: NavController) {
    InfoScaffold(nav, "privacyPolicy", stringResource(R.string.account_pp)) {
        Para(stringResource(R.string.pp_updated))
        Para(stringResource(R.string.pp_intro))
        H(stringResource(R.string.pp_collect_h)); Para(stringResource(R.string.pp_collect_b))
        H(stringResource(R.string.pp_why_h)); Para(stringResource(R.string.pp_why_b))
        H(stringResource(R.string.pp_where_h)); Para(stringResource(R.string.pp_where_b))
        H(stringResource(R.string.pp_share_h)); Para(stringResource(R.string.pp_share_b))
        H(stringResource(R.string.pp_keep_h)); Para(stringResource(R.string.pp_keep_b))
        H(stringResource(R.string.pp_contact_h)); Para(stringResource(R.string.pp_contact_b))
        NoteBox(stringResource(R.string.pp_note))
    }
}
