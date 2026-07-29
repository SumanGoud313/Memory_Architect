package com.suman.memoryarchitect.domain.model

/**
 * Every mission that can ever appear in [com.suman.memoryarchitect.domain.progression.MissionCatalog]'s
 * pool, across all three periods - one flat enum (rather than per-period pools of raw strings)
 * so a claim/progress record can be persisted as `.name` exactly like [AchievementId] already is
 * (see `UnlockedAchievementEntity`), and so `mock-backend/missions.js`/`functions/src/missions.ts`
 * mirror the exact same identifiers.
 */
enum class MissionId {
    // Daily pool
    CLEAR_TWO_LEVELS,
    CLEAR_PRACTICE_ROUND,
    WIN_DAILY_CHALLENGE,
    EARN_150_COINS,
    ZERO_HINT_CLEAR,
    HIGH_ACCURACY_CLEAR,
    UNLOCK_A_COSMETIC,
    EQUIP_A_COSMETIC,
    WATCH_A_REWARDED_AD,

    // Weekly pool
    CLEAR_FIFTEEN_LEVELS,
    THREE_STAR_TEN_TIMES,
    WIN_WEEKLY_CHALLENGE,
    EARN_800_COINS,
    FIVE_ZERO_HINT_CLEARS,

    // Monthly pool
    CLEAR_FORTY_LEVELS,
    FOUR_WEEKLY_SETS,
    EARN_2500_COINS_MONTHLY,
    // Tied to Daily/Weekly Challenge win-*counts*, both already capped at one win per real
    // calendar day/week (see GameMode.challengeLockDurationSeconds()) - structurally impossible to
    // rush in a single heavy play session no matter how many levels get played, unlike the three
    // above (see MissionCatalog's doc for why their own thresholds were also raised).
    WIN_DAILY_CHALLENGE_MONTHLY,
    WIN_WEEKLY_CHALLENGE_MONTHLY,

    // Event pool - generic, reused as-is by every LiveEventCatalog template (see MissionCatalog's
    // doc); never rotate, always all three active together whenever an event is live.
    EVENT_CLEAR_FIVE_LEVELS,
    EVENT_EARN_FORTY_STARS,
    EVENT_EARN_1000_COINS,
}
