package com.suman.memoryarchitect.core.sync

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules [PendingScoreSyncWorker] - called once from
 * [com.suman.memoryarchitect.MemoryArchitectApp.onCreate] (in case entries were already queued
 * from a previous session) and again immediately after
 * [com.suman.memoryarchitect.data.repository.ProgressionRepositoryImpl.submitScore] queues a new
 * one, so a retry is attempted as soon as WorkManager's own [NetworkType.CONNECTED] constraint is
 * satisfied rather than waiting for the next cold start.
 *
 * An interface (rather than [WorkManagerPendingScoreSyncScheduler] injected directly), same
 * reasoning as [com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider]'s doc -
 * [ProgressionRepositoryImpl][com.suman.memoryarchitect.data.repository.ProgressionRepositoryImpl]
 * is unit-tested in a plain JVM test with no real [WorkManager] available, so this seam lets a
 * test substitute a no-op fake instead. */
interface PendingScoreSyncScheduler {
    fun scheduleRetry()
}

@Singleton
class WorkManagerPendingScoreSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) : PendingScoreSyncScheduler {

    /** [ExistingWorkPolicy.KEEP] makes repeated calls safe - a sync already queued or running is
     * never duplicated. */
    override fun scheduleRetry() {
        val request = OneTimeWorkRequestBuilder<PendingScoreSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "pending_score_sync"
    }
}
