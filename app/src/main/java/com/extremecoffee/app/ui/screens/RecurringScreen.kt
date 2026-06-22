package com.extremecoffee.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.data.Reminder
import com.extremecoffee.app.data.Reminders
import com.extremecoffee.app.ui.CoffeeScaffold
import java.util.Calendar

@Composable
fun RecurringScreen(nav: NavController) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(Reminders.all(context)) }
    var dow by remember { mutableStateOf(Calendar.MONDAY) }
    var hour by remember { mutableStateOf(16) }
    var minute by remember { mutableStateOf(0) }
    var label by remember { mutableStateOf("") }

    val days = listOf(
        "Lun" to Calendar.MONDAY, "Mar" to Calendar.TUESDAY, "Mer" to Calendar.WEDNESDAY,
        "Gio" to Calendar.THURSDAY, "Ven" to Calendar.FRIDAY, "Sab" to Calendar.SATURDAY, "Dom" to Calendar.SUNDAY
    )

    CoffeeScaffold("Caffè ricorrenti", nav, "recurring") { mod ->
        Column(mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Programma un promemoria settimanale che ti ricorda di lanciare un Extreme Coffee.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            Text("Giorno", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { (name, value) ->
                    val selected = dow == value
                    Surface(
                        onClick = { dow = value },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(name, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("Ora", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(value = hour.toString(), onMinus = { hour = (hour + 23) % 24 }, onPlus = { hour = (hour + 1) % 24 })
                Text(" : ", style = MaterialTheme.typography.titleLarge)
                Stepper(value = "%02d".format(minute), onMinus = { minute = (minute + 55) % 60 }, onPlus = { minute = (minute + 5) % 60 })
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = label, onValueChange = { label = it.take(30) },
                label = { Text("Con chi? (opzionale, es. Colleghi)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                Reminders.add(context, Reminder(System.currentTimeMillis().toString(), dow, hour, minute, label.trim()))
                items = Reminders.all(context)
                label = ""
            }, modifier = Modifier.fillMaxWidth()) { Text("Aggiungi promemoria") }

            Spacer(Modifier.height(24.dp))
            Text("I tuoi promemoria", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text("Nessun promemoria. Aggiungine uno qui sopra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.forEach { r ->
                    Surface(shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Ogni ${dayName(r.dow)} alle ${"%02d:%02d".format(r.hour, r.minute)}",
                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                if (r.label.isNotBlank())
                                    Text("con ${r.label}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                Reminders.remove(context, r.id); items = Reminders.all(context)
                            }) { Text("Elimina") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onMinus, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(44.dp)) { Text("\u2212") }
        Text(value, modifier = Modifier.padding(horizontal = 14.dp),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onPlus, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(44.dp)) { Text("+") }
    }
}

private fun dayName(dow: Int): String = when (dow) {
    Calendar.MONDAY -> "lunedì"; Calendar.TUESDAY -> "martedì"; Calendar.WEDNESDAY -> "mercoledì"
    Calendar.THURSDAY -> "giovedì"; Calendar.FRIDAY -> "venerdì"; Calendar.SATURDAY -> "sabato"
    Calendar.SUNDAY -> "domenica"; else -> "?"
}
