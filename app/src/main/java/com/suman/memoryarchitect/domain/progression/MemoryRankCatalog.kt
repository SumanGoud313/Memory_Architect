package com.suman.memoryarchitect.domain.progression

/**
 * Pure display-tier label derived from existing player level - no new stored/earned state, unlike
 * [XpCurve]'s level itself. Intentionally NOT "Prestige": a real prestige-reset mechanic is a
 * genuine economy undertaking, out of scope for this pass (see the Points Economy plan). Bands run
 * through level 100 with headroom to extend later as a pure data change.
 */
object MemoryRankCatalog {
    private data class Band(val minLevel: Int, val title: String)

    private val bands = listOf(
        Band(1, "Novice"),
        Band(10, "Apprentice"),
        Band(20, "Journeyman"),
        Band(30, "Adept"),
        Band(40, "Architect"),
        Band(55, "Senior Architect"),
        Band(70, "Master Architect"),
        Band(85, "Grandmaster Architect"),
        Band(100, "Eternal Architect"),
    )

    fun rankFor(level: Int): String =
        bands.lastOrNull { level >= it.minLevel }?.title ?: bands.first().title
}
