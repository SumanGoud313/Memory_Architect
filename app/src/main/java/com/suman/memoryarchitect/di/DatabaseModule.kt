package com.suman.memoryarchitect.di

import android.content.Context
import androidx.room.Room
import com.suman.memoryarchitect.core.database.AppDatabase
import com.suman.memoryarchitect.core.database.Migrations
import com.suman.memoryarchitect.core.database.EquippedCosmeticDao
import com.suman.memoryarchitect.core.database.HintUsageDao
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.LevelBestTimeDao
import com.suman.memoryarchitect.core.database.LevelCampaignProgressDao
import com.suman.memoryarchitect.core.database.LuckySpinStateDao
import com.suman.memoryarchitect.core.database.MissionProgressDao
import com.suman.memoryarchitect.core.database.MissionRefreshStateDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.PendingMissionClaimDao
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
            // See Migrations' own doc for the full policy this pair of calls implements: only the
            // enumerated pre-release dev versions (1-19, none of which was ever a single stable
            // schema to migrate from - see Migrations' doc for the real-device crash that proved
            // this) may be destructively wiped on upgrade; anything from Migrations.CURRENT_VERSION
            // onward with no matching entry in Migrations.ALL fails loudly instead of silently
            // deleting data.
            .fallbackToDestructiveMigrationFrom(*Migrations.LEGACY_DESTRUCTIBLE_VERSIONS)
            .addMigrations(*Migrations.ALL)
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

    @Provides
    fun provideMissionProgressDao(database: AppDatabase): MissionProgressDao = database.missionProgressDao()

    @Provides
    fun provideInventoryItemDao(database: AppDatabase): InventoryItemDao = database.inventoryItemDao()

    @Provides
    fun providePendingMissionClaimDao(database: AppDatabase): PendingMissionClaimDao = database.pendingMissionClaimDao()

    @Provides
    fun provideLuckySpinStateDao(database: AppDatabase): LuckySpinStateDao = database.luckySpinStateDao()

    @Provides
    fun provideMissionRefreshStateDao(database: AppDatabase): MissionRefreshStateDao = database.missionRefreshStateDao()
}
