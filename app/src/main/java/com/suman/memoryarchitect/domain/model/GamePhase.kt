package com.suman.memoryarchitect.domain.model

/**
 * FINISHED collapses Scoring+Result from the architecture doc into one terminal state
 * until the scoring algorithm exists — split it once real scoring lands.
 */
enum class GamePhase {
    MEMORIZE,
    HIDDEN,
    RECONSTRUCT,
    FINISHED,
}