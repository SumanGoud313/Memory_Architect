package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.data.remote.LevelApi
import com.suman.memoryarchitect.data.remote.dto.ChallengeResponseDto
import com.suman.memoryarchitect.domain.model.ChallengePreview
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.repository.ChallengeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preview-only reads of the shared daily/weekly puzzle (same seed for every player — see
 * [LevelApi]) for Home's challenge cards. Unlike [RemoteConfigRepositoryImpl] there's no local
 * cache: a stale challenge preview isn't useful while offline, so a failure just means the
 * calling card is omitted rather than shown with day-old data.
 */
@Singleton
class ChallengeRepositoryImpl @Inject constructor(
    private val api: LevelApi,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
) : ChallengeRepository {

    override suspend fun getDailyChallenge(): Outcome<ChallengePreview> = fetch { api.getDailyChallenge() }

    override suspend fun getWeeklyChallenge(): Outcome<ChallengePreview> = fetch { api.getWeeklyChallenge() }

    private suspend fun fetch(call: suspend () -> ChallengeResponseDto): Outcome<ChallengePreview> = withContext(dispatchers.io) {
        try {
            val response = call()
            Outcome.Success(ChallengePreview(response.expiresAtEpochSecond, response.level.sceneType))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }
}
