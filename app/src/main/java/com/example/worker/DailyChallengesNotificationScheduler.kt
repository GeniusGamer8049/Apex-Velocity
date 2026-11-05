package com.example.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Scheduler for registering and managing WorkManager background triggers
 * for Daily Challenges local notification reminders.
 */
object DailyChallengesNotificationScheduler {

    const val WORK_NAME_PERIODIC = "daily_challenges_periodic_reminder"
    const val WORK_NAME_ONE_TIME = "daily_challenges_one_time_check"

    /**
     * Schedules a recurring 24-hour periodic WorkManager trigger.
     * Computes the initial delay until the next 9:00 AM local time.
     */
    fun scheduleDailyReminder(context: Context, forceUpdate: Boolean = false) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelayMs = (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)

        val periodicWorkRequest = PeriodicWorkRequestBuilder<DailyChallengesReminderWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .addTag(DailyChallengesReminderWorker.TAG)
            .build()

        val policy = if (forceUpdate) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            policy,
            periodicWorkRequest
        )
    }

    /**
     * Enqueues an immediate one-time WorkManager task to evaluate today's Daily Challenges
     * and trigger a local notification.
     */
    fun triggerImmediateReminderCheck(context: Context) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DailyChallengesReminderWorker>()
            .addTag(DailyChallengesReminderWorker.TAG)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_NAME_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }

    /**
     * Cancels scheduled reminder work.
     */
    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME_PERIODIC)
    }
}
