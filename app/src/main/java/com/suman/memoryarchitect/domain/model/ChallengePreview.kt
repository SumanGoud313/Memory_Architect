package com.suman.memoryarchitect.domain.model

/** A lightweight preview of the shared daily/weekly puzzle — enough to render a Home card
 * (countdown to reset) without generating/committing to the full [LevelSpec] the way starting
 * gameplay does. */
data class ChallengePreview(val expiresAtEpochSecond: Long, val sceneType: String)
