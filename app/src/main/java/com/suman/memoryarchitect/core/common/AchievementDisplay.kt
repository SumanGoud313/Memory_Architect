package com.suman.memoryarchitect.core.common

import android.content.Context
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.AchievementId

fun AchievementId.toDisplayTitle(context: Context): String = when (this) {
    AchievementId.FIRST_STEPS -> context.getString(R.string.achievement_first_steps)
    AchievementId.DEDICATED -> context.getString(R.string.achievement_dedicated)
    AchievementId.CENTURY -> context.getString(R.string.achievement_century)
    AchievementId.SHARP_EYE -> context.getString(R.string.achievement_sharp_eye)
    AchievementId.PERFECTIONIST -> context.getString(R.string.achievement_perfectionist)
    AchievementId.WEEK_STREAK -> context.getString(R.string.achievement_week_streak)
    AchievementId.MONTH_STREAK -> context.getString(R.string.achievement_month_streak)
    AchievementId.RISING_STAR -> context.getString(R.string.achievement_rising_star)
    AchievementId.ARCHITECT -> context.getString(R.string.achievement_architect)
    AchievementId.FLAWLESS -> context.getString(R.string.achievement_flawless)
    AchievementId.GRAND_ARCHITECT -> context.getString(R.string.achievement_grand_architect)
    AchievementId.FIRST_PERFECT_ROOM -> context.getString(R.string.achievement_first_perfect_room)
    AchievementId.PERFECT_ROOMS_10 -> context.getString(R.string.achievement_perfect_rooms_10)
    AchievementId.PERFECT_ROOMS_100 -> context.getString(R.string.achievement_perfect_rooms_100)
    AchievementId.LEVEL_25 -> context.getString(R.string.achievement_level_25)
    AchievementId.LEVEL_50 -> context.getString(R.string.achievement_level_50)
    AchievementId.LEVEL_100 -> context.getString(R.string.achievement_level_100)
    AchievementId.TOP_100_DAILY -> context.getString(R.string.achievement_top_100_daily)
    AchievementId.TOP_10_WEEKLY -> context.getString(R.string.achievement_top_10_weekly)
    AchievementId.MEMORY_MASTER -> context.getString(R.string.achievement_memory_master)
    AchievementId.SPEED_RUNNER -> context.getString(R.string.achievement_speed_runner)
    AchievementId.PRECISION_EXPERT -> context.getString(R.string.achievement_precision_expert)
}
