package com.suman.memoryarchitect.core.sync

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules [PendingMissionClaimSyncWorker] - called once from
 * [com.suman.memoryarchitect.MemoryArchitectApp.onCreate] (in case a claim was already queued from
 * a previous session) and again immediately after
 * [com.suman.memoryarchitect.data.repository.MissionRepositoryImpl.claimMissionReward] queues a new
 * one, mirroring [PendingScoreSyncScheduler]'s exact reasoning for the equivalent score-submission
 * queue.
 *
 * An interface for the same test-substitutability reason [PendingScoreSyncScheduler]'s doc gives -
 * [MissionRepositoryImpl][com.suman.memoryarchitect.data.repository.MissionRepositoryImpl] is
 * unit-tested in a plain JVM test with no real [WorkManager] available. */
interface PendingMissionClaimSyncScheduler {
    fun scheduleRetry()
}

@Singleton
class WorkManagerPendingMissionClaimSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) : PendingMissionClaimSyncScheduler {

    /** [ExistingWorkPolicy.KEEP] makes repeated calls safe - a sync already queued or running is
     * never duplicated. */
    override fun scheduleRetry() {
        val request = OneTimeWorkRequestBuilder<PendingMissionClaimSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "pending_mission_claim_sync"
    }
}
