package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.room.AppDatabase
import com.example.data.room.DailyChallengesGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker that periodically verifies active Daily Challenges in Room
 * and sends a rich local notification reminding the player to engage and claim rewards.
 */
class DailyChallengesReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "daily_challenges_reminder_channel"
        const val NOTIFICATION_ID = 4001
        const val TAG = "DailyChallengesReminderWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            createNotificationChannelIfNeeded()

            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.dailyChallengesDao()
            val todayKey = DailyChallengesGenerator.getTodayKey()

            // Ensure challenges exist for today
            var todayChallenges = dao.getChallengesForDate(todayKey)
            if (todayChallenges.isEmpty()) {
                val generated = DailyChallengesGenerator.generateDefaultChallengesForDate(todayKey)
                dao.insertDefaultChallenges(generated)
                todayChallenges = dao.getChallengesForDate(todayKey)
            }

            val unclaimedReady = todayChallenges.count { it.isTaskFinished && !it.isClaimed }
            val inProgressCount = todayChallenges.count { !it.isTaskFinished }

            val title: String
            val contentText: String

            when {
                unclaimedReady > 0 -> {
                    title = "🏁 Rewards Waiting to Claim!"
                    contentText = "You have $unclaimedReady completed daily challenge reward(s) ready! Tap to collect bonus Credits & REP."
                }
                inProgressCount > 0 -> {
                    title = "⚡ New Daily Challenges Are Live!"
                    contentText = "$inProgressCount fresh high-speed tasks await on the track today. Push your limits and claim bonus rewards!"
                }
                else -> {
                    title = "🏆 Daily Challenges Updated!"
                    contentText = "Check out today's circuit challenges and earn bonus credits towards your vehicle upgrades."
                }
            }

            sendNotification(title, contentText)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Challenges Reminder"
            val descriptionText = "Reminders for new and active daily racing tasks and rewards"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, content: String) {
        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!permissionGranted) {
                // Permission not granted by user, gracefully skip posting
                return
            }
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Select small icon: use app mipmap or android default if not resolved
        val smallIcon = R.mipmap.ic_launcher

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (ignored: SecurityException) {
                // Guard against permission changes at dispatch time
            }
        }
    }
}
