package com.bearbones.kumaflow

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = appContext.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
            val intervalMode = prefs.getString("auto_backup_interval", "off") ?: "off"
            if (intervalMode == "off") return Result.success()

            // Run backup synchronously via suspending coroutine
            var success = false
            val latch = java.util.concurrent.CountDownLatch(1)
            backupAppToJSON(appContext, isAuto = true) { ok ->
                success = ok
                latch.countDown()
            }
            latch.await(60, TimeUnit.SECONDS)

            if (success) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "kumaflow_auto_backup"

        fun schedule(context: Context) {
            val prefs = context.getSharedPreferences("kumaflow_prefs", Context.MODE_PRIVATE)
            val intervalMode = prefs.getString("auto_backup_interval", "off") ?: "off"

            if (intervalMode == "off") {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val customDays = prefs.getInt("auto_backup_custom_days", 1).coerceAtLeast(1)
            val intervalHours = when (intervalMode) {
                "daily" -> 24L
                "weekly" -> 24L * 7L
                "monthly" -> 24L * 30L
                "custom" -> 24L * customDays.toLong()
                else -> { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME); return }
            }

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalHours, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
