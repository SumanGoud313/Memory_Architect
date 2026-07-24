package com.suman.memoryarchitect.domain.generation

/**
 * Furniture-type grouping for a room's designed slots, so [LevelGenerator] can place an object
 * where it would actually belong (books on a shelf, a mug on a table) instead of any empty slot
 * at random. Declaration order here is the canonical fallback-search order mirrored in
 * `mock-backend/generation.js` — keep both in sync.
 */
enum class SlotCategory {
    SHELF,
    TABLE,
    COUNTER,
    DESK,
    WINDOWSILL,
    WALL,
    BED_SIDE,
    STORAGE_TOP,
    FLOOR,
}
