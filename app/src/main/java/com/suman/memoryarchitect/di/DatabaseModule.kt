package com.suman.memoryarchitect.di

import android.content.Context
import androidx.room.Room
import com.suman.memoryarchitect.core.database.AppDatabase
import com.suman.memoryarchitect.core.database.EquippedCosmeticDao
import com.suman.memoryarchitect.core.database.HintUsageDao
import com.suman.memoryarchitect.core.database.LevelBestTimeDao
import com.suman.memoryarchitect.core.database.LevelCampaignProgressDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.PendingScoreSubmissionDao
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.database.RedoUsageDao
import com.suman.memoryarchitect.core.database.RemoteConfigDao
import com.suman.memoryarchitect.core.database.RewatchUsageDao
import com.suman.memoryarchitect.core.database.StatisticsDao
import com.suman.memoryarchitect.core.database.UnlockedAchievementDao
import com.suman.memoryarchitect.core.database.UnlockedRewardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "memory_architect.db")
            // Pre-1.0, no shipped schema to preserve yet. Replace with real Migrations
            // before release.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideRemoteConfigDao(database: AppDatabase): RemoteConfigDao = database.remoteConfigDao()

    @Provides
    fun providePlayerProgressDao(database: AppDatabase): PlayerProgressDao = database.playerProgressDao()

    @Provides
    fun providePendingScoreSubmissionDao(database: AppDatabase): PendingScoreSubmissionDao =
        database.pendingScoreSubmissionDao()

    @Provides
    fun provideStatisticsDao(database: AppDatabase): StatisticsDao = database.statisticsDao()

    @Provides
    fun provideUnlockedAchievementDao(database: AppDatabase): UnlockedAchievementDao =
        database.unlockedAchievementDao()

    @Provides
    fun provideLevelCampaignProgressDao(database: AppDatabase): LevelCampaignProgressDao =
        database.levelCampaignProgressDao()

    @Provides
    fun provideLevelBestTimeDao(database: AppDatabase): LevelBestTimeDao = database.levelBestTimeDao()

    @Provides
    fun provideUnlockedRewardDao(database: AppDatabase): UnlockedRewardDao = database.unlockedRewardDao()

    @Provides
    fun provideHintUsageDao(database: AppDatabase): HintUsageDao = database.hintUsageDao()

    @Provides
    fun provideRedoUsageDao(database: AppDatabase): RedoUsageDao = database.redoUsageDao()

    @Provides
    fun provideRewatchUsageDao(database: AppDatabase): RewatchUsageDao = database.rewatchUsageDao()

    @Provides
    fun provideOwnedCosmeticDao(database: AppDatabase): OwnedCosmeticDao = database.ownedCosmeticDao()

    @Provides
    fun provideEquippedCosmeticDao(database: AppDatabase): EquippedCosmeticDao = database.equippedCosmeticDao()
}
