package com.extremecoffee.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.extremecoffee.app.R
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
        R.string.rec_d_mon to Calendar.MONDAY, R.string.rec_d_tue to Calendar.TUESDAY,
        R.string.rec_d_wed to Calendar.WEDNESDAY, R.string.rec_d_thu to Calendar.THURSDAY,
        R.string.rec_d_fri to Calendar.FRIDAY, R.string.rec_d_sat to Calendar.SATURDAY,
        R.string.rec_d_sun to Calendar.SUNDAY
    )

    CoffeeScaffold(stringResource(R.string.home_recurring_title), nav, "recurring") { mod ->
        Column(mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.rec_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.rec_day), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { (nameRes, value) ->
                    val selected = dow == value
                    Surface(
                        onClick = { dow = value },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(nameRes), modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.rec_time), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(value = hour.toString(), onMinus = { hour = (hour + 23) % 24 }, onPlus = { hour = (hour + 1) % 24 })
                Text(" : ", style = MaterialTheme.typography.titleLarge)
                Stepper(value = "%02d".format(minute), onMinus = { minute = (minute + 55) % 60 }, onPlus = { minute = (minute + 5) % 60 })
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(value = label, onValueChange = { label = it.take(30) },
                label = { Text(stringResource(R.string.rec_with)) },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                Reminders.add(context, Reminder(System.currentTimeMillis().toString(), dow, hour, minute, label.trim()))
                items = Reminders.all(context)
                label = ""
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rec_add)) }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.rec_yours), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Text(stringResource(R.string.rec_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.forEach { r ->
                    Surface(shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                val timeStr = "%02d:%02d".format(r.hour, r.minute)
                                Text(stringResource(R.string.rec_every, dayFullName(r.dow), timeStr),
                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                if (r.label.isNotBlank())
                                    Text(stringResource(R.string.rec_with_label, r.label), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = {
                                Reminders.remove(context, r.id); items = Reminders.all(context)
                            }) { Text(stringResource(R.string.common_delete)) }
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

@Composable
private fun dayFullName(dow: Int): String = stringResource(
    when (dow) {
        Calendar.MONDAY -> R.string.rec_full_mon
        Calendar.TUESDAY -> R.string.rec_full_tue
        Calendar.WEDNESDAY -> R.string.rec_full_wed
        Calendar.THURSDAY -> R.string.rec_full_thu
        Calendar.FRIDAY -> R.string.rec_full_fri
        Calendar.SATURDAY -> R.string.rec_full_sat
        else -> R.string.rec_full_sun
    }
)
