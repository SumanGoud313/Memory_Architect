package com.suman.memoryarchitect.domain.model

/**
 * A gameplay/economy signal that may advance one or more active missions' progress - reported via
 * [com.suman.memoryarchitect.domain.repository.MissionRepository.recordMissionEvent]. Deliberately
 * reuses the exact completion points [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel]
 * and friends already have (see the plan doc) rather than threading new instrumentation through
 * gameplay code - each variant below corresponds 1:1 to one [MissionRequirementType].
 */
sealed interface MissionEvent {
    data object LevelCompleted : MissionEvent
    data object PracticeRoundCompleted : MissionEvent
    data object DailyChallengeWon : MissionEvent
    data object WeeklyChallengeWon : MissionEvent
    data class CoinsEarned(val amount: Long) : MissionEvent
    data object ZeroHintLevelClear : MissionEvent
    data object HighAccuracyClear : MissionEvent
    data class StarsEarned(val count: Int) : MissionEvent
    data object CosmeticUnlocked : MissionEvent
    data object CosmeticEquipped : MissionEvent
    data object RewardedAdWatched : MissionEvent
}
