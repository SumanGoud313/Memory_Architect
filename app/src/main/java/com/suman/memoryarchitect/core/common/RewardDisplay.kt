package com.suman.memoryarchitect.core.common

import android.content.Context
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.RewardId

fun RewardId.toDisplayTitle(context: Context): String = when (this) {
    RewardId.ROOM_KITCHEN -> context.getString(R.string.reward_room_kitchen)
    RewardId.ROOM_COFFEE_SHOP -> context.getString(R.string.reward_room_coffee_shop)
    RewardId.ROOM_GAMING_ROOM -> context.getString(R.string.reward_room_gaming_room)
    RewardId.ROOM_OFFICE -> context.getString(R.string.reward_room_office)
    RewardId.ROOM_LIBRARY -> context.getString(R.string.reward_room_library)
    RewardId.ROOM_GARDEN -> context.getString(R.string.reward_room_garden)
    RewardId.ROOM_SPACE_STATION -> context.getString(R.string.reward_room_space_station)
    RewardId.PALETTE_OCEAN -> context.getString(R.string.reward_palette_ocean)
    RewardId.PALETTE_FOREST -> context.getString(R.string.reward_palette_forest)
    RewardId.PALETTE_ORCHID -> context.getString(R.string.reward_palette_orchid)
    RewardId.PALETTE_MIDNIGHT -> context.getString(R.string.reward_palette_midnight)
    RewardId.PALETTE_SUNSET -> context.getString(R.string.reward_palette_sunset)
    RewardId.PALETTE_EMBER -> context.getString(R.string.reward_palette_ember)
    RewardId.TITLE_APPRENTICE -> context.getString(R.string.reward_title_apprentice)
    RewardId.TITLE_RISING_TALENT -> context.getString(R.string.reward_title_rising_talent)
    RewardId.TITLE_SEASONED_DESIGNER -> context.getString(R.string.reward_title_seasoned_designer)
    RewardId.TITLE_MASTER_ARCHITECT -> context.getString(R.string.reward_title_master_architect)
    RewardId.TITLE_LEGENDARY_ARCHITECT -> context.getString(R.string.reward_title_legendary_architect)
    RewardId.TITLE_GRANDMASTER_ARCHITECT -> context.getString(R.string.reward_title_grandmaster_architect)
    RewardId.TITLE_ETERNAL_ARCHITECT -> context.getString(R.string.reward_title_eternal_architect)
    RewardId.BADGE_BRONZE_KEY -> context.getString(R.string.reward_badge_bronze_key)
    RewardId.BADGE_SILVER_KEY -> context.getString(R.string.reward_badge_silver_key)
    RewardId.BADGE_GOLD_KEY -> context.getString(R.string.reward_badge_gold_key)
    RewardId.BADGE_PLATINUM_KEY -> context.getString(R.string.reward_badge_platinum_key)
    RewardId.BADGE_DIAMOND_KEY -> context.getString(R.string.reward_badge_diamond_key)
    RewardId.BADGE_OBSIDIAN_KEY -> context.getString(R.string.reward_badge_obsidian_key)
    RewardId.BADGE_CELESTIAL_KEY -> context.getString(R.string.reward_badge_celestial_key)
}