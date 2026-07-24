package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.data.repository.AvatarUploadRepositoryImpl
import com.suman.memoryarchitect.data.repository.ChallengeRepositoryImpl
import com.suman.memoryarchitect.data.repository.HintRepositoryImpl
import com.suman.memoryarchitect.data.repository.LeaderboardRepositoryImpl
import com.suman.memoryarchitect.data.repository.LevelCampaignRepositoryImpl
import com.suman.memoryarchitect.data.repository.LevelRepositoryImpl
import com.suman.memoryarchitect.data.repository.LocalProgressResetRepositoryImpl
import com.suman.memoryarchitect.data.repository.ProgressionRepositoryImpl
import com.suman.memoryarchitect.data.repository.RedoRepositoryImpl
import com.suman.memoryarchitect.data.repository.RemoteConfigRepositoryImpl
import com.suman.memoryarchitect.data.repository.RewatchRepositoryImpl
import com.suman.memoryarchitect.data.repository.ShopRepositoryImpl
import com.suman.memoryarchitect.domain.repository.AvatarUploadRepository
import com.suman.memoryarchitect.domain.repository.ChallengeRepository
import com.suman.memoryarchitect.domain.repository.HintRepository
import com.suman.memoryarchitect.domain.repository.LeaderboardRepository
import com.suman.memoryarchitect.domain.repository.LevelCampaignRepository
import com.suman.memoryarchitect.domain.repository.LevelRepository
import com.suman.memoryarchitect.domain.repository.LocalProgressResetRepository
import com.suman.memoryarchitect.domain.repository.ProgressionRepository
import com.suman.memoryarchitect.domain.repository.RedoRepository
import com.suman.memoryarchitect.domain.repository.RemoteConfigRepository
import com.suman.memoryarchitect.domain.repository.RewatchRepository
import com.suman.memoryarchitect.domain.repository.ShopRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRemoteConfigRepository(
        impl: RemoteConfigRepositoryImpl,
    ): RemoteConfigRepository

    @Binds
    @Singleton
    abstract fun bindLevelRepository(
        impl: LevelRepositoryImpl,
    ): LevelRepository

    @Binds
    @Singleton
    abstract fun bindProgressionRepository(
        impl: ProgressionRepositoryImpl,
    ): ProgressionRepository

    @Binds
    @Singleton
    abstract fun bindLevelCampaignRepository(
        impl: LevelCampaignRepositoryImpl,
    ): LevelCampaignRepository

    @Binds
    @Singleton
    abstract fun bindChallengeRepository(
        impl: ChallengeRepositoryImpl,
    ): ChallengeRepository

    @Binds
    @Singleton
    abstract fun bindLocalProgressResetRepository(
        impl: LocalProgressResetRepositoryImpl,
    ): LocalProgressResetRepository

    @Binds
    @Singleton
    abstract fun bindHintRepository(
        impl: HintRepositoryImpl,
    ): HintRepository

    @Binds
    @Singleton
    abstract fun bindRedoRepository(
        impl: RedoRepositoryImpl,
    ): RedoRepository

    @Binds
    @Singleton
    abstract fun bindRewatchRepository(
        impl: RewatchRepositoryImpl,
    ): RewatchRepository

    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(
        impl: LeaderboardRepositoryImpl,
    ): LeaderboardRepository

    @Binds
    @Singleton
    abstract fun bindAvatarUploadRepository(
        impl: AvatarUploadRepositoryImpl,
    ): AvatarUploadRepository

    @Binds
    @Singleton
    abstract fun bindShopRepository(
        impl: ShopRepositoryImpl,
    ): ShopRepository
}