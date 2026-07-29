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
        PendingMissionClaimEntity::class,
        LuckySpinStateEntity::class,
        MissionRefreshStateEntity::class,
    ],
    // Keep in sync with Migrations.CURRENT_VERSION - see that object's doc for the migration
    // policy this version number is part of. Not referenced directly (the @Database annotation
    // needs a compile-time constant), but Migrations.CURRENT_VERSION exists specifically to spell
    // out why 22 is significant, and AppDatabaseMigrationTest asserts the two stay equal.
    //
    // 21 -> 22 adds mission_refresh_state (the Missions pay-to-reroll-early overhaul) - see
    // Migrations.ALL's MIGRATION_21_22; this version is deliberately NOT added to
    // Migrations.LEGACY_DESTRUCTIBLE_VERSIONS, since a real Migration exists for it instead.
    //
    // 22 -> 23 adds pending_score_submissions.awardXp (the "no XP for repeat level completions"
    // fix) - see Migrations.ALL's MIGRATION_22_23.
    version = 23,
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
    abstract fun pendingMissionClaimDao(): PendingMissionClaimDao
    abstract fun luckySpinStateDao(): LuckySpinStateDao
    abstract fun missionRefreshStateDao(): MissionRefreshStateDao
}
