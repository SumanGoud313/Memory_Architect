package com.suman.memoryarchitect.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/** Flushes [com.suman.memoryarchitect.core.database.PendingScoreSubmissionDao] once WorkManager's
 * own [androidx.work.NetworkType.CONNECTED] constraint (see [PendingScoreSyncScheduler]) is
 * satisfied - the "future sync worker" that entity's own doc always pointed at. Retries with
 * WorkManager's default exponential backoff on failure (still offline, a transient server error)
 * rather than looping in-process. */
@HiltWorker
class PendingScoreSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val progressionRepository: ProgressionRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        progressionRepository.retryPendingSubmissions()
        Result.success()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.retry()
    }
}
