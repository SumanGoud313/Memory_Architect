package com.suman.memoryarchitect.core.common

import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId

/** Derives a display name straight from the enum constant (`FRAME_CELESTIAL_HALO` -> "Celestial
 * Halo") rather than a 32-entry string-resource table like [toDisplayTitle] uses for [RewardId] -
 * cosmetic item names are flavor text, not user-facing copy that needs translation review, and
 * this keeps adding a new catalog item a one-line [CosmeticId] change with nothing else to update. */
fun CosmeticId.toDisplayName(): String {
    val prefix = when (category()) {
        CosmeticCategory.AVATAR_FRAME -> "FRAME_"
        CosmeticCategory.PROFILE_BORDER -> "BORDER_"
        CosmeticCategory.NAME_COLOR -> "NAME_COLOR_"
        CosmeticCategory.TIMER_STYLE -> "TIMER_"
        CosmeticCategory.VICTORY_ANIMATION -> "VICTORY_"
        CosmeticCategory.CONFETTI_EFFECT -> "CONFETTI_"
        CosmeticCategory.STICKER_PACK -> "STICKERS_"
        CosmeticCategory.TROPHY_RELIC -> "TROPHY_"
        CosmeticCategory.BACKGROUND_THEME -> "BACKGROUND_"
        CosmeticCategory.PROFILE_BADGE -> "BADGE_"
        CosmeticCategory.ROOM_SKIN -> "ROOM_"
        CosmeticCategory.OBJECT_MATERIAL -> "MATERIAL_"
    }
    return name.removePrefix(prefix)
        .split("_")
        .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
}

fun CosmeticId.category(): CosmeticCategory =
    com.suman.memoryarchitect.domain.progression.AllCosmeticsCatalog.requireDefinition(this).category

fun CosmeticCategory.toDisplayName(): String = when (this) {
    CosmeticCategory.AVATAR_FRAME -> "Avatar Frames"
    CosmeticCategory.PROFILE_BORDER -> "Button Borders"
    CosmeticCategory.NAME_COLOR -> "Name Colors"
    CosmeticCategory.TIMER_STYLE -> "Timer Styles"
    CosmeticCategory.VICTORY_ANIMATION -> "Victory Animations"
    CosmeticCategory.CONFETTI_EFFECT -> "Confetti Effects"
    CosmeticCategory.STICKER_PACK -> "Sticker Packs"
    CosmeticCategory.TROPHY_RELIC -> "Trophy & Relics"
    CosmeticCategory.BACKGROUND_THEME -> "Background Themes"
    CosmeticCategory.PROFILE_BADGE -> "Profile Badges"
    CosmeticCategory.ROOM_SKIN -> "Room Skins"
    CosmeticCategory.OBJECT_MATERIAL -> "Object Materials"
}
