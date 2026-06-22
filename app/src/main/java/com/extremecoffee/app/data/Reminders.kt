package com.extremecoffee.app.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Promemoria settimanale (giorno + ora + etichetta) per ricordarti di lanciare un Extreme Coffee. */
data class Reminder(
    val id: String,
    val dow: Int,        // Calendar.DAY_OF_WEEK (1=Dom ... 7=Sab)
    val hour: Int,
    val minute: Int,
    val label: String
)

object Reminders {
    private const val PREFS = "extreme_coffee_reminders"
    private const val KEY = "list"

    fun all(context: Context): List<Reminder> =
        current(context).mapNotNull { parse(it) }
            .sortedWith(compareBy({ it.dow }, { it.hour }, { it.minute }))

    fun add(context: Context, r: Reminder) {
        val set = current(context).toMutableSet()
        set.add(serialize(r))
        save(context, set)
        schedule(context, r)
    }

    fun remove(context: Context, id: String) {
        val set = current(context).filterNot { parse(it)?.id == id }.toMutableSet()
        save(context, set)
        cancel(context, id)
    }

    /** Ri-pianifica tutti i promemoria salvati (idempotente): chiamata all'avvio. */
    fun rescheduleAll(context: Context) {
        runCatching { all(context).forEach { schedule(context, it) } }
    }

    private fun current(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY, emptySet()) ?: emptySet()

    private fun save(context: Context, set: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY, set).apply()
    }

    private fun serialize(r: Reminder): String {
        val label = r.label.replace("|", " ").replace("\n", " ").trim()
        return "${r.id}|${r.dow}|${r.hour}|${r.minute}|$label"
    }

    private fun parse(s: String): Reminder? = runCatching {
        val p = s.split("|", limit = 5)
        Reminder(p[0], p[1].toInt(), p[2].toInt(), p[3].toInt(), if (p.size > 4) p[4] else "")
    }.getOrNull()

    private fun schedule(context: Context, r: Reminder) {
        runCatching {
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delayMillisTo(r.dow, r.hour, r.minute), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("label" to r.label, "notifId" to r.id.hashCode()))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("recurring_${r.id}", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }

    private fun cancel(context: Context, id: String) {
        runCatching { WorkManager.getInstance(context).cancelUniqueWork("recurring_$id") }
    }

    private fun delayMillisTo(dow: Int, hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val t = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        while (t.get(Calendar.DAY_OF_WEEK) != dow || t.timeInMillis <= now.timeInMillis) {
            t.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (t.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
