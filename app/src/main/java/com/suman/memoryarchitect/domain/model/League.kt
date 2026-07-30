package com.suman.memoryarchitect.domain.model

/**
 * A player's competitive tier, derived from lifetime XP by [forXp] - client code always computes
 * it this way, never sets it as an independent value. `firestore.rules`' `isValidIdentityFields`
 * only checks the written `league` string is one of this enum's names, not that it actually
 * matches the account's real XP - re-deriving and comparing would need a Cloud Function this
 * project doesn't run (see the Spark migration report's accepted-risk note), so a modified client
 * could in principle claim a League above its real standing.
 */
enum class League(val minXp: Long, val displayName: String) {
    APPRENTICE(minXp = 0L, displayName = "Apprentice"),
    JOURNEYMAN(minXp = 1_000L, displayName = "Journeyman"),
    ARCHITECT(minXp = 5_000L, displayName = "Architect"),
    MASTER_ARCHITECT(minXp = 20_000L, displayName = "Master Architect"),
    GRANDMASTER(minXp = 50_000L, displayName = "Grandmaster"),
    ;

    companion object {
        fun forXp(xp: Long): League = League.entries.sortedByDescending { it.minXp }.first { xp >= it.minXp }
    }
}
