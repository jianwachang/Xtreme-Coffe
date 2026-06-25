package com.extremecoffee.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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

/* ---------- Lingua ---------- */

@Composable
fun LanguageScreen(nav: NavController) {
    InfoScaffold(nav, "language", "Lingua") {
        Para("Lingua attuale dell'app:")
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = true, onClick = {})
                Spacer(Modifier.width(8.dp))
                Text("Italiano", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        NoteBox("Al momento Extreme Coffee è disponibile in italiano. Altre lingue (es. English) " +
            "arriveranno in un prossimo aggiornamento.")
    }
}

/* ---------- Privacy (permessi e dati) ---------- */

@Composable
fun PrivacyScreen(nav: NavController) {
    val context = LocalContext.current
    InfoScaffold(nav, "privacy", "Privacy") {
        Para("Extreme Coffee usa solo i dati necessari a farti incontrare gli amici per un caffè.")
        H("Permessi usati")
        Para(
            "• Posizione: per mostrarti sulla mappa durante un Extreme Coffee e trovare i bar vicini.\n" +
            "• Contatti: per riconoscere quali amici hanno già l'app e poterli invitare.\n" +
            "• Fotocamera: per la foto profilo e il selfie finale.\n" +
            "• Notifiche: per avvisarti quando ricevi un invito."
        )
        Para("Puoi concedere o revocare ogni permesso quando vuoi dalle impostazioni di sistema.")
        Button(
            onClick = {
                val i = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Apri le impostazioni dell'app") }
        Spacer(Modifier.height(18.dp))
        H("I tuoi dati")
        Para("Puoi eliminare in qualsiasi momento il tuo profilo e i dati associati da " +
            "Account → \"Elimina dati e account\".")
        Para("Per i dettagli completi su quali dati raccogliamo e perché, leggi l'Informativa sulla privacy.")
    }
}

/* ---------- Termini e condizioni ---------- */

@Composable
fun TermsScreen(nav: NavController) {
    InfoScaffold(nav, "terms", "Termini e condizioni") {
        Para("Ultimo aggiornamento: giugno 2026")
        Para("Usando Extreme Coffee accetti questi termini. Se non sei d'accordo, non utilizzare l'app.")

        H("1. Il servizio")
        Para("Extreme Coffee è un'app che ti permette di invitare amici a incontrarti per un caffè " +
            "entro un breve intervallo di tempo, mostrando la posizione dei partecipanti durante l'evento.")

        H("2. Uso corretto")
        Para("Ti impegni a usare l'app in modo lecito e rispettoso, a non molestare altri utenti e a non " +
            "inserire contenuti offensivi o ingannevoli. Sei responsabile delle attività svolte dal tuo account.")

        H("3. Account")
        Para("Per usare l'app fornisci un nickname e un numero di telefono. Sei responsabile della veridicità " +
            "dei dati inseriti e della custodia del tuo dispositivo.")

        H("4. Posizione e incontri")
        Para("La condivisione della posizione avviene solo durante un Extreme Coffee attivo e con le persone " +
            "coinvolte. Gli incontri avvengono sotto la tua responsabilità: usa buon senso e prudenza.")

        H("5. Limitazione di responsabilità")
        Para("L'app è fornita \"così com'è\". Non garantiamo che sia priva di errori o sempre disponibile e, " +
            "nei limiti di legge, non siamo responsabili per danni derivanti dall'uso del servizio.")

        H("6. Modifiche")
        Para("Possiamo aggiornare questi termini; le modifiche saranno indicate dalla data di aggiornamento.")

        NoteBox("Questo testo è un modello iniziale da far verificare a un professionista prima della " +
            "pubblicazione. Non costituisce consulenza legale.")
    }
}

/* ---------- Informativa sulla privacy ---------- */

@Composable
fun PrivacyPolicyScreen(nav: NavController) {
    InfoScaffold(nav, "privacyPolicy", "Informativa sulla privacy") {
        Para("Ultimo aggiornamento: giugno 2026")
        Para("Questa informativa spiega quali dati tratta Extreme Coffee e come.")

        H("Dati che raccogliamo")
        Para(
            "• Profilo: nickname e numero di telefono, eventuale foto profilo.\n" +
            "• Posizione: posizione approssimativa/precisa solo durante un Extreme Coffee attivo.\n" +
            "• Contatti: usati sul dispositivo per riconoscere gli amici che hanno l'app.\n" +
            "• Dati tecnici: identificativo del dispositivo e token per le notifiche."
        )

        H("Perché li usiamo")
        Para("Per farti creare un profilo, inviare e ricevere inviti, mostrare i partecipanti sulla mappa " +
            "durante un evento e inviarti le notifiche.")

        H("Dove sono conservati")
        Para("I dati sono gestiti tramite i servizi Google Firebase (database e notifiche). Alcune " +
            "informazioni restano salvate solo sul tuo dispositivo.")

        H("Condivisione")
        Para("Non vendiamo i tuoi dati. Vengono condivisi solo con gli utenti coinvolti negli inviti che " +
            "tu invii o accetti, e con i fornitori tecnici necessari al funzionamento dell'app.")

        H("Conservazione e cancellazione")
        Para("Puoi eliminare il profilo e i dati associati in qualsiasi momento da " +
            "Account → \"Elimina dati e account\".")

        H("Contatti")
        Para("Per richieste sulla privacy puoi scrivere all'indirizzo di supporto indicato nella scheda " +
            "dell'app sullo store.")

        NoteBox("Questo testo è un modello iniziale da personalizzare e far verificare prima della " +
            "pubblicazione sullo store. Non costituisce consulenza legale.")
    }
}
