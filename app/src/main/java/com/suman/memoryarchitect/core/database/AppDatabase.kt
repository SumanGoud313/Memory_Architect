package com.suman.memoryarchitect.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RemoteConfigCacheEntity::class,
        PlayerProgressCacheEntity::class,
        PendingScoreSubmissionEntity::class,
        StatisticsCacheEntity::class,
        UnlockedAchievementEntity::class,
        LevelCampaignProgressEntity::class,
        LevelBestTimeEntity::class,
        UnlockedRewardEntity::class,
        HintUsageEntity::class,
        RedoUsageEntity::class,
        RewatchUsageEntity::class,
        OwnedCosmeticEntity::class,
        EquippedCosmeticEntity::class,
        MissionProgressEntity::class,
        InventoryItemEntity::class,
    ],
    version = 19,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remoteConfigDao(): RemoteConfigDao
    abstract fun playerProgressDao(): PlayerProgressDao
    abstract fun pendingScoreSubmissionDao(): PendingScoreSubmissionDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun unlockedAchievementDao(): UnlockedAchievementDao
    abstract fun levelCampaignProgressDao(): LevelCampaignProgressDao
    abstract fun levelBestTimeDao(): LevelBestTimeDao
    abstract fun unlockedRewardDao(): UnlockedRewardDao
    abstract fun hintUsageDao(): HintUsageDao
    abstract fun redoUsageDao(): RedoUsageDao
    abstract fun rewatchUsageDao(): RewatchUsageDao
    abstract fun ownedCosmeticDao(): OwnedCosmeticDao
    abstract fun equippedCosmeticDao(): EquippedCosmeticDao
    abstract fun missionProgressDao(): MissionProgressDao
    abstract fun inventoryItemDao(): InventoryItemDao
}
