package com.suman.memoryarchitect.domain.model

enum class RewardId {
    // Room-theme gallery unlocks. Bedroom is available from the start, so it has no reward.
    ROOM_KITCHEN,
    ROOM_COFFEE_SHOP,
    ROOM_GAMING_ROOM,
    ROOM_OFFICE,
    ROOM_LIBRARY,
    ROOM_GARDEN,
    ROOM_SPACE_STATION,

    // Equippable accent palettes.
    PALETTE_OCEAN,
    PALETTE_FOREST,
    PALETTE_ORCHID,
    PALETTE_MIDNIGHT,
    PALETTE_SUNSET,
    PALETTE_EMBER,

    // Equippable player titles.
    TITLE_APPRENTICE,
    TITLE_RISING_TALENT,
    TITLE_SEASONED_DESIGNER,
    TITLE_MASTER_ARCHITECT,
    TITLE_LEGENDARY_ARCHITECT,
    TITLE_GRANDMASTER_ARCHITECT,
    TITLE_ETERNAL_ARCHITECT,

    // Collectible badges.
    BADGE_BRONZE_KEY,
    BADGE_SILVER_KEY,
    BADGE_GOLD_KEY,
    BADGE_PLATINUM_KEY,
    BADGE_DIAMOND_KEY,
    BADGE_OBSIDIAN_KEY,
    BADGE_CELESTIAL_KEY,
}