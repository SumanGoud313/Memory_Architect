package com.suman.memoryarchitect.domain.model

/**
 * A player's competitive tier, purely derived from lifetime XP - never a separately stored or
 * client-supplied field anywhere (Firestore, Room, or the wire). A client can no more "set" its
 * League than it can set its own XP total; the only way to change it is to actually earn XP.
 * [forXp] is mirrored exactly (same thresholds, same order) in `functions/src/index.ts` so the
 * server-side re-validation of `players/{uid}` writes can independently compute the same League a
 * client claims - the same "two independent copies of one small formula" precedent
 * [com.suman.memoryarchitect.data.repository.FirestoreProgressionRemoteSource]'s own doc already
 * establishes for `coinsAwardedFor`.
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
