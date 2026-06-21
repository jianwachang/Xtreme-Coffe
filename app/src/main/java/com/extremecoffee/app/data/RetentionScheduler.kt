package com.extremecoffee.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Pianifica il recap settimanale (venerdì sera) con WorkManager: nessuna infrastruttura server. */
object RetentionScheduler {

    fun schedule(context: Context) {
        runCatching {
            val request = PeriodicWorkRequestBuilder<RecapWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setInitialDelay(millisUntilNextFridayEvening(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("weekly_recap", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    private fun millisUntilNextFridayEvening(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        while (target.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY || target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}
