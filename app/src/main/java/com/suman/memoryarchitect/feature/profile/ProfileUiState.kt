package com.suman.memoryarchitect.feature.profile

import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.CosmeticCategory
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.PlayerStatistics
import com.suman.memoryarchitect.domain.model.RewardId

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val error: AppError) : ProfileUiState
    data class Content(
        val level: Int,
        val xpIntoLevel: Long,
        val xpForNextLevel: Long,
        val profile: PlayerProfile,
        val statistics: PlayerStatistics,
        val unlockedAchievementIds: Set<AchievementId>,
        val unlockedRewardIds: Set<RewardId>,
        val equippedTitleId: RewardId?,
        val equippedPaletteId: RewardId?,
        val dailyRewardStatus: DailyRewardStatus?,
        val isClaimingDailyReward: Boolean = false,
        // Points Economy - purely additive, see ShopRepository/CollectionsScreen/ProfileScreen.
        val memoryRank: String = "",
        val ownedCosmeticIds: Set<CosmeticId> = emptySet(),
        val equippedCosmetics: Map<CosmeticCategory, CosmeticId> = emptyMap(),
    ) : ProfileUiState
}
