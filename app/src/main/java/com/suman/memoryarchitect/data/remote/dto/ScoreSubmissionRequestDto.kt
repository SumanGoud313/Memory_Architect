package com.suman.memoryarchitect.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScoreSubmissionRequestDto(
    val mode: String,
    val levelSeed: Long,
    val finalScore: Int,
    val sceneAccuracy: Float,
    val comboCount: Int,
    val playedOnEpochDay: Long,
    /** How many achievements this exact round newly unlocked (locally evaluated - see
     * [com.suman.memoryarchitect.domain.model.ScoreSubmissionResult]'s doc) - the one Memory
     * Journey point source the server can't independently recompute, since achievements are
     * local-only. Level-completed/perfect-accuracy/streak-milestone bonuses are all recomputed
     * server-side from the fields already above. */
    val newlyUnlockedAchievementCount: Int = 0,
    /** `false` only for a repeat clear of an already-completed Classic level - see
     * [com.suman.memoryarchitect.domain.repository.ProgressionRepository.submitScore]'s doc. The
     * mock backend zeroes the xp contribution when `false`, mirroring [FirestoreProgressionRemoteSource]'s
     * own handling. */
    val awardXp: Boolean = true,
)
