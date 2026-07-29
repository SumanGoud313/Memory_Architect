package com.suman.memoryarchitect.core.notifications

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules [DailyReminderWorker] once, from [com.suman.memoryarchitect.MemoryArchitectApp.onCreate] -
 * a periodic (~24h) request whose first run is timed to land at [REMINDER_LOCAL_HOUR] local time
 * today (or tomorrow, if that hour has already passed), the "never before local quiet hours" rule
 * from the retention plan's Notifications section. [ExistingPeriodicWorkPolicy.KEEP] makes this
 * safe to call on every cold start without ever re-scheduling (and so re-delaying) an already-queued
 * chain. */
@Singleton
class NotificationScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val clock: Clock,
) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(Duration.ofHours(24))
            .setInitialDelay(delayUntilNextReminderWindow(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun delayUntilNextReminderWindow(): Long {
        val now = ZonedDateTime.now(clock)
        val todayWindow = ZonedDateTime.of(LocalDate.now(clock), LocalTime.of(REMINDER_LOCAL_HOUR, 0), now.zone)
        val target = if (now.isBefore(todayWindow)) todayWindow else todayWindow.plusDays(1)
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "daily_reminder"
        // Early evening - late enough that "haven't played yet today" is a meaningful signal
        // rather than firing before most players' typical session, early enough it's never a
        // late-night ping.
        const val REMINDER_LOCAL_HOUR = 18
    }
}
