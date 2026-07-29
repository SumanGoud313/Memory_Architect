package com.suman.memoryarchitect.domain.model

/** How long a player has been away, bucketed per the retention plan's Returning-player
 * experience section - see [com.suman.memoryarchitect.domain.progression.ReturningPlayerRules].
 * Tone escalates with the gap; material rewards only start at [MEDIUM]. */
enum class ReturningPlayerTier {
    /** Played today, or the most recent gap is too short to be worth remarking on. */
    NONE,

    /** A few days away - warm tone only, never a reward ("a reward for a 4-day gap would read as
     * a bribe, not a welcome" - see the plan doc). */
    SHORT,

    /** Long enough to be a real absence - banner plus a modest gift. */
    MEDIUM,

    /** A month or more - banner, gift, and a reminder of exactly how much Memory Journey progress
     * is still banked, to counter the fear of having lost ground. */
    LONG,
}
