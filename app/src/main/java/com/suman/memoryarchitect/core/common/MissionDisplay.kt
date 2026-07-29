package com.suman.memoryarchitect.core.common

import android.content.Context
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.MissionId

fun MissionId.toDisplayTitle(context: Context): String = when (this) {
    MissionId.CLEAR_TWO_LEVELS -> context.getString(R.string.mission_clear_two_levels)
    MissionId.CLEAR_PRACTICE_ROUND -> context.getString(R.string.mission_clear_practice_round)
    MissionId.WIN_DAILY_CHALLENGE -> context.getString(R.string.mission_win_daily_challenge)
    MissionId.EARN_150_COINS -> context.getString(R.string.mission_earn_150_coins)
    MissionId.ZERO_HINT_CLEAR -> context.getString(R.string.mission_zero_hint_clear)
    MissionId.HIGH_ACCURACY_CLEAR -> context.getString(R.string.mission_high_accuracy_clear)
    MissionId.UNLOCK_A_COSMETIC -> context.getString(R.string.mission_unlock_a_cosmetic)
    MissionId.EQUIP_A_COSMETIC -> context.getString(R.string.mission_equip_a_cosmetic)
    MissionId.WATCH_A_REWARDED_AD -> context.getString(R.string.mission_watch_a_rewarded_ad)
    MissionId.CLEAR_FIFTEEN_LEVELS -> context.getString(R.string.mission_clear_fifteen_levels)
    MissionId.THREE_STAR_TEN_TIMES -> context.getString(R.string.mission_three_star_ten_times)
    MissionId.WIN_WEEKLY_CHALLENGE -> context.getString(R.string.mission_win_weekly_challenge)
    MissionId.EARN_800_COINS -> context.getString(R.string.mission_earn_800_coins)
    MissionId.FIVE_ZERO_HINT_CLEARS -> context.getString(R.string.mission_five_zero_hint_clears)
    MissionId.CLEAR_FORTY_LEVELS -> context.getString(R.string.mission_clear_forty_levels)
    MissionId.FOUR_WEEKLY_SETS -> context.getString(R.string.mission_four_weekly_sets)
    MissionId.EARN_2500_COINS_MONTHLY -> context.getString(R.string.mission_earn_2500_coins_monthly)
    MissionId.WIN_DAILY_CHALLENGE_MONTHLY -> context.getString(R.string.mission_win_daily_challenge_monthly)
    MissionId.WIN_WEEKLY_CHALLENGE_MONTHLY -> context.getString(R.string.mission_win_weekly_challenge_monthly)
    MissionId.EVENT_CLEAR_FIVE_LEVELS -> context.getString(R.string.mission_event_clear_five_levels)
    MissionId.EVENT_EARN_FORTY_STARS -> context.getString(R.string.mission_event_earn_forty_stars)
    MissionId.EVENT_EARN_1000_COINS -> context.getString(R.string.mission_event_earn_1000_coins)
}
