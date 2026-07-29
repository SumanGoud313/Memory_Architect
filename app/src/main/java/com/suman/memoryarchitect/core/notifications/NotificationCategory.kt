package com.suman.memoryarchitect.core.notifications

/** Every local notification this app can post - see [DailyReminderWorker]. Each has its own
 * per-category opt-out in Settings (see [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore])
 * and its own [com.suman.memoryarchitect.core.analytics.logNotificationScheduled]/
 * [com.suman.memoryarchitect.core.analytics.logNotificationTapped] tag. */
enum class NotificationCategory {
    STREAK_REMINDER,
    DAILY_CHALLENGE_READY,
}
