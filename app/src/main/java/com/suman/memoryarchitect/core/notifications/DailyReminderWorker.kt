package com.suman.memoryarchitect.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suman.memoryarchitect.MainActivity
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logNotificationScheduled
import com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.usecase.GetPlayerProfileUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

/**
 * Runs roughly once a day (see [NotificationScheduler]) and decides whether *any* local
 * notification is worth sending today - the hard "at most one queued at a time, one daily cap
 * across all categories" rule from the retention plan's Notifications section is satisfied simply
 * by this worker's own cadence (one run, one candidate, one fixed [NOTIFICATION_ID] that a later
 * post always replaces rather than stacking) rather than needing separate bookkeeping.
 *
 * Never fires at all once the player has already played today (`lastPlayedEpochDay == today`) -
 * the whole point is nudging a player who *hasn't* opened the app yet, never a habit-forming ping
 * for someone already engaged. Between the two categories, a live streak takes priority (it's the
 * more time-sensitive, loss-averse signal); Daily Challenge is the fallback when there's no streak
 * to protect.
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getPlayerProfile: GetPlayerProfileUseCase,
    private val preferences: UserPreferencesDataStore,
    private val notificationChannels: NotificationChannels,
    private val analytics: AnalyticsLogger,
    private val clock: Clock,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val profile = (getPlayerProfile() as? Outcome.Success)?.data ?: return Result.success()
        val todayEpochDay = LocalDate.now(clock).toEpochDay()
        if (profile.lastPlayedEpochDay == todayEpochDay) return Result.success()

        val category = when {
            profile.currentStreak > 0 && preferences.streakReminderEnabled.first() -> NotificationCategory.STREAK_REMINDER
            preferences.dailyChallengeReminderEnabled.first() -> NotificationCategory.DAILY_CHALLENGE_READY
            else -> null
        } ?: return Result.success()

        postNotification(category)
        return Result.success()
    }

    private fun postNotification(category: NotificationCategory) {
        val context = applicationContext
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationChannels.ensureCreated()

        val (titleRes, textRes) = when (category) {
            NotificationCategory.STREAK_REMINDER -> R.string.notification_streak_reminder_title to R.string.notification_streak_reminder_text
            NotificationCategory.DAILY_CHALLENGE_READY -> R.string.notification_daily_challenge_title to R.string.notification_daily_challenge_text
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TAPPED_NOTIFICATION_CATEGORY, category.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, category.ordinal, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(textRes))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        analytics.logNotificationScheduled(category.name)
    }

    private companion object {
        // One fixed id for every category - a new day's notification always replaces the previous
        // one instead of stacking, satisfying "at most one queued at a time" without extra state.
        const val NOTIFICATION_ID = 1001
    }
}
