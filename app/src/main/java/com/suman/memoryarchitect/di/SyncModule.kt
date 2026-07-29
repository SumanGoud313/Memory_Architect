package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.core.sync.PendingMissionClaimSyncScheduler
import com.suman.memoryarchitect.core.sync.PendingScoreSyncScheduler
import com.suman.memoryarchitect.core.sync.WorkManagerPendingMissionClaimSyncScheduler
import com.suman.memoryarchitect.core.sync.WorkManagerPendingScoreSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindPendingScoreSyncScheduler(impl: WorkManagerPendingScoreSyncScheduler): PendingScoreSyncScheduler

    @Binds
    @Singleton
    abstract fun bindPendingMissionClaimSyncScheduler(impl: WorkManagerPendingMissionClaimSyncScheduler): PendingMissionClaimSyncScheduler
}
