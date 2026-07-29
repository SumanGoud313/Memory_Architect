package com.suman.memoryarchitect.domain.model

// Declaration order is Mode Select's display order (GameMode.entries) - never looked up by
// ordinal elsewhere (every persisted/logged reference uses .name), so reordering here is safe.
enum class GameMode {
    CLASSIC,
    PRACTICE,
    DAILY_CHALLENGE,
    WEEKLY_CHALLENGE,
}