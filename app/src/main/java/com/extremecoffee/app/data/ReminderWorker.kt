package com.extremecoffee.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.extremecoffee.app.Notifier

/** Invia la notifica del promemoria ricorrente all'orario pianificato. */
class ReminderWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val label = inputData.getString("label") ?: ""
            val notifId = inputData.getInt("notifId", "recurring".hashCode())
            Notifier.showReminder(applicationContext, label, notifId)
            Result.success()
        } catch (e: Throwable) {
            Result.success()
        }
    }
}
