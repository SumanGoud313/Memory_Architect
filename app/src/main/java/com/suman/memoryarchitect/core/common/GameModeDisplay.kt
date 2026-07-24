package com.suman.memoryarchitect.core.common

import android.content.Context
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.progression.ProgressionRules

fun GameMode.toDisplayTitle(context: Context): String = when (this) {
    GameMode.CLASSIC -> context.getString(R.string.mode_classic_title)
    GameMode.DAILY_CHALLENGE -> context.getString(R.string.mode_daily_title)
    GameMode.PRACTICE -> context.getString(R.string.mode_practice_title)
    GameMode.WEEKLY_CHALLENGE -> context.getString(R.string.mode_weekly_title)
}

fun GameMode.toSubtitleRes(): Int = when (this) {
    GameMode.CLASSIC -> R.string.mode_classic_subtitle
    GameMode.DAILY_CHALLENGE -> R.string.mode_daily_subtitle
    GameMode.PRACTICE -> R.string.mode_practice_subtitle
    GameMode.WEEKLY_CHALLENGE -> R.string.mode_weekly_subtitle
}

/** Non-null only for the two modes that pay a flat win bonus instead of (or in addition to) XP -
 * see [ProgressionRules.dailyChallengeWinCoins]/[ProgressionRules.weeklyChallengeWinCoins], the
 * single source of truth this reads from rather than a duplicated number in the UI layer. */
fun GameMode.winRewardCoins(): Long? = when (this) {
    GameMode.DAILY_CHALLENGE -> ProgressionRules.Default.dailyChallengeWinCoins
    GameMode.WEEKLY_CHALLENGE -> ProgressionRules.Default.weeklyChallengeWinCoins
    GameMode.CLASSIC, GameMode.PRACTICE -> null
}

/** How long a win locks this mode's Mode Select card for - 24h Daily / 168h (7 days) Weekly,
 * matching the shared puzzle's own daily/weekly reset cadence. Null for modes with no win lock. */
fun GameMode.challengeLockDurationSeconds(): Long? = when (this) {
    GameMode.DAILY_CHALLENGE -> 24L * 60 * 60
    GameMode.WEEKLY_CHALLENGE -> 7L * 24 * 60 * 60
    GameMode.CLASSIC, GameMode.PRACTICE -> null
}

