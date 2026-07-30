package com.suman.memoryarchitect.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The production migration policy for [AppDatabase], as of schema version [CURRENT_VERSION].
 *
 * Versions 1 through 19 were exclusively this app's own pre-release development builds - this
 * project has never shipped to a real device outside this working tree (no Play Store release, no
 * tagged version, `versionCode` is still 1) - so there is no installed base anywhere holding data
 * at any of those versions worth migrating. [DatabaseModule] scopes the destructive fallback to
 * exactly that closed, enumerated range via `fallbackToDestructiveMigrationFrom`, which is the
 * explicit developer acknowledgement this audit finding asked for: a literal, reviewable list of
 * "these specific old versions may be wiped, and no others."
 *
 * 19 belongs in that range too, not just 1-18, and this was learned the hard way: an earlier
 * version of this policy tried writing a real, incremental [Migration] from 19 to 20 instead
 * (adding just the one new table this version needed). It crashed a real device immediately on
 * launch with `IllegalStateException: Migration didn't properly handle: pending_score_submissions
 * - Expected 12 columns, Found 10`. The reason: version 19 was the schema in place for this
 * entire session's development, the whole time the *previous* policy (an unconditional
 * `fallbackToDestructiveMigration`) was active - every entity change along the way (this
 * particular table gained `comboCount`/`newlyUnlockedAchievementCount` partway through) silently
 * wiped and recreated a real device's database under that same unchanged version number, with no
 * per-version schema ever actually being validated. "Version 19" was never one fixed, known
 * shape - it was whatever a given device happened to have the last time it was recreated, which
 * could differ from what the exported `19.json` snapshot (frozen at whatever moment this file was
 * last generated) assumes. There is no way to write a correct incremental migration off of a
 * starting point that was never actually stable, and Room's own post-migration validation will
 * keep finding the next drifted table one crash at a time rather than all at once. Destructively
 * recreating from scratch sidesteps the whole problem: every table is built directly from the
 * current entity annotations, so it can never mismatch what the running code actually expects.
 *
 * From version 20 onward, that fallback no longer applies to anything - a schema bump with no
 * matching entry in [ALL] is not silently destructive, it throws `IllegalStateException: A
 * migration from X to Y was required but not found` the moment a real device tries to open the
 * upgraded database, which is Room's own way of refusing to guess. Adding a real schema change
 * means adding both a bump to [AppDatabase.version] and a real [Migration] to [ALL] in the same
 * change - never one without the other. 20 is the first version this app was actually built and
 * shipped under this discipline from the start, with no prior "silently mutated under an unmoved
 * version number" history to worry about - which is why it's the one this policy can actually
 * protect.
 *
 * [AppDatabaseMigrationTest] (androidTest) exercises this policy directly: it builds a real v1
 * database from the schema already exported to `app/schemas/`, opens it through the exact same
 * builder configuration [DatabaseModule] uses, and asserts that succeeds without a crash (the
 * legacy destructive path); and confirms a v[CURRENT_VERSION] database round-trips with zero data
 * loss when reopened at the same version.
 */
object Migrations {
    const val CURRENT_VERSION = 25

    /** Every version in this (inclusive) range may still fall back to a destructive wipe on
     * upgrade - see this object's own doc for why 19 belongs here too, not just 1-18: it was never
     * a single, stable, validated schema to migrate from in the first place. This is the one place
     * this range is allowed to grow *backward* (a newly-discovered old dev-only version) but never
     * forward past a version this app was actually built and shipped under this policy from the
     * start (20 onward). */
    val LEGACY_DESTRUCTIBLE_VERSIONS: IntArray = (1..19).toList().toIntArray()

    /** 20 -> 21: adds the `lucky_spin_state` table (one singleton row, same shape
     * [PlayerProgressCacheEntity]'s table has) for the Lucky Spin daily-gate/first-spin overhaul -
     * see [LuckySpinStateEntity]'s doc. A pure `CREATE TABLE`, so every existing row in every other
     * table is untouched. */
    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `lucky_spin_state` (`id` INTEGER NOT NULL, `lastFreeSpinEpochDay` INTEGER, `lastAdSpinEpochDay` INTEGER, `hasEverSpun` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    /** 21 -> 22: adds the `mission_refresh_state` table (one singleton row, same shape
     * `lucky_spin_state`'s table has) for the Missions pay-1000-coins-to-reroll-early feature -
     * see [MissionRefreshStateEntity]'s doc. A pure `CREATE TABLE`, so every existing row in every
     * other table is untouched. */
    private val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mission_refresh_state` (`id` INTEGER NOT NULL, `dailyForcedPeriodKey` INTEGER, `weeklyForcedPeriodKey` INTEGER, `monthlyForcedPeriodKey` INTEGER, PRIMARY KEY(`id`))",
            )
        }
    }

    /** 22 -> 23: adds `pending_score_submissions.awardXp` (default `1`/true, so every already-queued
     * offline submission keeps awarding XP exactly as it would have before this column existed -
     * see [PendingScoreSubmissionEntity.awardXp]'s own doc) for the "don't award XP for a repeat
     * clear of an already-completed level" fix. A pure `ALTER TABLE ADD COLUMN`, so every existing
     * row and every other table is untouched. */
    private val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `pending_score_submissions` ADD COLUMN `awardXp` INTEGER NOT NULL DEFAULT 1")
        }
    }

    /** 23 -> 24: adds `lucky_spin_state.adSpinsUsedToday` (default `0`, so every already-existing
     * row starts at "no ad spins used today yet" - correct regardless of what
     * `lastAdSpinEpochDay` happened to hold, since that field is only ever consulted together with
     * this one) for the "1 -> 3 ad-gated bonus spins per day" change - see
     * [LuckySpinStateEntity.adSpinsUsedToday]'s own doc. A pure `ALTER TABLE ADD COLUMN`, so every
     * existing row and every other table is untouched. */
    private val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `lucky_spin_state` ADD COLUMN `adSpinsUsedToday` INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** 24 -> 25: adds the `mystery_chest_ad_state` table (one singleton row, same shape
     * `lucky_spin_state`'s table has) for the watch-ad-only Mystery Chest claim feature on
     * [com.suman.memoryarchitect.ui.screens.shop.LuckySpinScreen] - see
     * [MysteryChestAdStateEntity]'s doc. A pure `CREATE TABLE`, so every existing row in every
     * other table is untouched. */
    private val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `mystery_chest_ad_state` (`id` INTEGER NOT NULL, `lastClaimEpochDay` INTEGER, `claimsUsedToday` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    /** Real, incremental migrations for version 20 and beyond, in order - the next schema change
     * adds one more [Migration] here (from [CURRENT_VERSION] to `CURRENT_VERSION + 1`) alongside
     * bumping [AppDatabase]'s version, never one without the other - see this object's own doc for
     * why this is the version that discipline actually starts being trustworthy from. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25)
}
