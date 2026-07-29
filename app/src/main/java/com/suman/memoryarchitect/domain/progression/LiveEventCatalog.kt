package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.LiveEvent
import com.suman.memoryarchitect.domain.model.RemoteConfig
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Data-driven Live Events framework. Nine real seasonal templates below, each a real window plus
 * featured [CosmeticId]s already sold in [ShopCatalog]/[PremiumCatalog] - curation, not new
 * content, matching the Points Economy plan's "no new source of truth" framing for this phase.
 *
 * The windows here are only *defaults* - which template (if any) is actually live, and its real
 * window, is resolved from [RemoteConfig] by [activeEvent] (see
 * [com.suman.memoryarchitect.data.repository.FirebaseRemoteConfigSource]'s doc): the console picks
 * one `id` from [events] and can narrow/move its window without a release. Leaving
 * `event_active_id` unset keeps every template dormant, same as before this file had real entries.
 */
object LiveEventCatalog {

    private fun epochSecondsAt(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toEpochSecond()

    val events: List<LiveEvent> = listOf(
        LiveEvent(
            id = "NEW_YEAR",
            startEpochSecond = epochSecondsAt(2026, 1, 1),
            endEpochSecond = epochSecondsAt(2026, 1, 4),
            featuredCosmeticIds = listOf(
                CosmeticId.VICTORY_FIREWORK_BURST,
                CosmeticId.CONFETTI_RAINBOW_CASCADE,
                CosmeticId.BACKGROUND_STARFIELD_DEEP,
            ),
        ),
        LiveEvent(
            id = "VALENTINES_DAY",
            startEpochSecond = epochSecondsAt(2026, 2, 13),
            endEpochSecond = epochSecondsAt(2026, 2, 16),
            featuredCosmeticIds = listOf(
                CosmeticId.NAME_COLOR_SUNSET_GRADIENT,
                CosmeticId.CONFETTI_RIBBON_FALL,
                CosmeticId.BORDER_RUBY,
            ),
        ),
        LiveEvent(
            id = "HOLI",
            startEpochSecond = epochSecondsAt(2026, 3, 3),
            endEpochSecond = epochSecondsAt(2026, 3, 6),
            featuredCosmeticIds = listOf(
                CosmeticId.BACKGROUND_AURORA_DRIFT,
                CosmeticId.NAME_COLOR_PRISM_SHIFT,
                CosmeticId.CONFETTI_PAPER_TOSS,
            ),
        ),
        LiveEvent(
            id = "SUMMER",
            startEpochSecond = epochSecondsAt(2026, 6, 1),
            endEpochSecond = epochSecondsAt(2026, 9, 1),
            featuredCosmeticIds = listOf(
                CosmeticId.BACKGROUND_MISTY_DAWN,
                CosmeticId.TIMER_STARLIGHT_ARC,
                CosmeticId.BORDER_SAPPHIRE,
            ),
        ),
        LiveEvent(
            id = "INDEPENDENCE_DAY",
            startEpochSecond = epochSecondsAt(2026, 8, 14),
            endEpochSecond = epochSecondsAt(2026, 8, 17),
            featuredCosmeticIds = listOf(
                CosmeticId.BORDER_EMERALD,
                CosmeticId.BADGE_ARCHITECT_SIGIL,
                CosmeticId.VICTORY_GOLDEN_SPARKS,
            ),
        ),
        LiveEvent(
            id = "HALLOWEEN",
            startEpochSecond = epochSecondsAt(2026, 10, 30),
            endEpochSecond = epochSecondsAt(2026, 11, 2),
            featuredCosmeticIds = listOf(
                CosmeticId.BORDER_OBSIDIAN,
                CosmeticId.STICKERS_MYTHIC_PACK,
                CosmeticId.TIMER_EMBER_SWEEP,
            ),
        ),
        LiveEvent(
            id = "DIWALI",
            startEpochSecond = epochSecondsAt(2026, 11, 6),
            endEpochSecond = epochSecondsAt(2026, 11, 11),
            featuredCosmeticIds = listOf(
                CosmeticId.TROPHY_GOLDEN_HOURGLASS,
                CosmeticId.BADGE_GOLDEN_CREST,
                CosmeticId.CONFETTI_LUXURY_GOLDLEAF,
            ),
        ),
        LiveEvent(
            id = "CHRISTMAS",
            startEpochSecond = epochSecondsAt(2026, 12, 24),
            endEpochSecond = epochSecondsAt(2026, 12, 27),
            featuredCosmeticIds = listOf(
                CosmeticId.BORDER_DIAMOND_GLOW,
                CosmeticId.CONFETTI_STAR_SHOWER,
                CosmeticId.TROPHY_DIAMOND_CROWN,
            ),
        ),
        LiveEvent(
            id = "ANNIVERSARY",
            startEpochSecond = epochSecondsAt(2026, 7, 15),
            endEpochSecond = epochSecondsAt(2026, 7, 18),
            featuredCosmeticIds = listOf(
                CosmeticId.BORDER_ROYAL_GOLD,
                CosmeticId.FRAME_CELESTIAL_HALO,
                CosmeticId.VICTORY_SUPERNOVA,
            ),
        ),
    )

    /**
     * Resolves the live event, if any, per [remoteConfig]'s `event_active_id`/`event_start_epoch`/
     * `event_end_epoch` (see [FIREBASE_SETUP.md]'s table) - remote config only ever *selects* one of
     * [events] and optionally overrides its window; it never carries event content. An unset or
     * empty `event_active_id`, or one that doesn't match any [events] id, means no event is active.
     * A zero/blank override for either bound falls back to that template's own default window.
     */
    fun activeEvent(remoteConfig: RemoteConfig, nowEpochSecond: Long): LiveEvent? {
        val activeId = remoteConfig.values["event_active_id"]?.takeIf { it.isNotBlank() } ?: return null
        val template = events.firstOrNull { it.id == activeId } ?: return null
        val start = remoteConfig.values["event_start_epoch"]?.toLongOrNull()?.takeIf { it > 0L }
            ?: template.startEpochSecond
        val end = remoteConfig.values["event_end_epoch"]?.toLongOrNull()?.takeIf { it > 0L }
            ?: template.endEpochSecond
        val resolved = template.copy(startEpochSecond = start, endEpochSecond = end)
        return resolved.takeIf { nowEpochSecond in it.startEpochSecond..it.endEpochSecond }
    }
}
