package com.extremecoffee.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.extremecoffee.app.Notifier
import com.extremecoffee.app.R

/** Calcola il recap settimanale e invia una notifica locale, anche se l'app è chiusa. */
class RecapWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val loc = LocaleManager.wrap(ctx)   // testi del recap nella lingua scelta
        return try {
            if (!Profile.isRegistered(ctx)) return Result.success()
            val now = System.currentTimeMillis()
            val week = weekIndex(now)
            if (Profile.lastRecapWeek(ctx) == week) return Result.success()  // già inviato questa settimana

            val stats = CoffeeRepository.loadMyStats(ctx)
            if (stats.total <= 0) { Profile.setLastRecapWeek(ctx, week); return Result.success() }

            val title = loc.getString(R.string.recap_title)
            val body = buildString {
                append(loc.getString(R.string.recap_month, stats.thisMonth))
                if (stats.streakWeeks > 0) append(loc.getString(R.string.recap_streak, stats.streakWeeks))
                append(loc.getString(R.string.recap_counts, stats.launched, stats.joined))
                if (stats.atRisk) append(loc.getString(R.string.recap_atrisk))
            }
            Notifier.showRecap(ctx, title, body)
            Profile.setLastRecapWeek(ctx, week)
            Result.success()
        } catch (e: Throwable) {
            Result.success()   // il recap non deve mai bloccare o ritentare in loop
        }
    }
}
