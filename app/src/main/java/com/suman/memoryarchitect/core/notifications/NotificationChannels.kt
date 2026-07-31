package com.suman.memoryarchitect.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Creates this app's one notification channel - channels have existed on every API level this
 * app supports since minSdk was raised to 26, so unlike a library that still ships below that,
 * there's no pre-26 branch to skip here. Safe to call repeatedly (channel creation is idempotent).
 * One shared channel for both [NotificationCategory] values rather than one each - they're the
 * same "come back today" intent at the user-facing importance/sound/vibration level Android's
 * channel settings expose; splitting them would only fragment one system-settings toggle into two
 * for no real benefit, while the per-category opt-out that actually matters already lives in this
 * app's own Settings. */
@Singleton
class NotificationChannels @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun ensureCreated() {
        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        val channel = NotificationChannel(REMINDERS_CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Streak and Daily Challenge reminders"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val REMINDERS_CHANNEL_ID = "reminders"
    }
}
