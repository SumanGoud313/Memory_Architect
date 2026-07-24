package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.repository.LevelCampaignRepository
import javax.inject.Inject

data class LevelCampaignProgress(
    val maxUnlockedLevel: Int,
    val bestTimes: Map<Int, Long>,
    val bestStars: Map<Int, Int>,
)

class GetLevelCampaignProgressUseCase @Inject constructor(
    private val repository: LevelCampaignRepository,
) {
    suspend operator fun invoke(): LevelCampaignProgress = LevelCampaignProgress(
        maxUnlockedLevel = repository.getMaxUnlockedLevel(),
        bestTimes = repository.getAllBestTimes(),
        bestStars = repository.getAllBestStars(),
    )
}
