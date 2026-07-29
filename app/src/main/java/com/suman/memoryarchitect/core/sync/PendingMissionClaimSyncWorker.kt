package com.suman.memoryarchitect.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suman.memoryarchitect.domain.repository.MissionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/** Flushes [com.suman.memoryarchitect.core.database.PendingMissionClaimDao] once WorkManager's own
 * [androidx.work.NetworkType.CONNECTED] constraint (see [PendingMissionClaimSyncScheduler]) is
 * satisfied - the mission-claim counterpart to [PendingScoreSyncWorker]. Retries with WorkManager's
 * default exponential backoff on failure (still offline, a transient server error) rather than
 * looping in-process. */
@HiltWorker
class PendingMissionClaimSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val missionRepository: MissionRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        missionRepository.retryPendingClaims()
        Result.success()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.retry()
    }
}
