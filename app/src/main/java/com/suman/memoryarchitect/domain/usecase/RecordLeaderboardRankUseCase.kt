package com.suman.memoryarchitect.domain.usecase

import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import javax.inject.Inject

class RecordLeaderboardRankUseCase @Inject constructor(
    private val repository: ProgressionRepository,
) {
    suspend operator fun invoke(dailyRank: Int?, weeklyRank: Int?, todayEpochDay: Long): List<AchievementId> =
        repository.recordLeaderboardRank(dailyRank, weeklyRank, todayEpochDay)
}
