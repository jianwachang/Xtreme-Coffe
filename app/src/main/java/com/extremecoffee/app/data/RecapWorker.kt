package com.extremecoffee.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.extremecoffee.app.Notifier

/** Calcola il recap settimanale e invia una notifica locale, anche se l'app è chiusa. */
class RecapWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        return try {
            if (!Profile.isRegistered(ctx)) return Result.success()
            val now = System.currentTimeMillis()
            val week = weekIndex(now)
            if (Profile.lastRecapWeek(ctx) == week) return Result.success()  // già inviato questa settimana

            val stats = CoffeeRepository.loadMyStats(ctx)
            if (stats.total <= 0) { Profile.setLastRecapWeek(ctx, week); return Result.success() }

            val title = "La tua settimana di caffè \u2615"
            val body = buildString {
                append("${stats.thisMonth} caffè questo mese")
                if (stats.streakWeeks > 0) append(" \u00b7 streak ${stats.streakWeeks} settimane \uD83D\uDD25")
                append("\n${stats.launched} lanciati \u00b7 ${stats.joined} partecipati")
                if (stats.atRisk) append("\nLancia un Extreme Coffee per non perdere lo streak!")
            }
            Notifier.showRecap(ctx, title, body)
            Profile.setLastRecapWeek(ctx, week)
            Result.success()
        } catch (e: Throwable) {
            Result.success()   // il recap non deve mai bloccare o ritentare in loop
        }
    }
}
