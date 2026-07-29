package com.suman.memoryarchitect.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the migration policy documented on [Migrations] - see that object's doc for the full
 * reasoning, including the real-device crash that shaped it. Three scenarios:
 *
 * 1. [legacyVersionDatabase_opensThroughDestructiveFallback_withoutCrashing] - a database seeded at
 *    schema version 1 (real, exported schema - `app/schemas/.../1.json`) opens successfully through
 *    the exact same builder configuration [com.suman.memoryarchitect.di.DatabaseModule] ships,
 *    proving the legacy destructive-fallback path (versions 1-19, never a real install with a
 *    single stable schema) recreates the schema cleanly rather than crashing the app.
 * 2. [version19DatabaseWithDriftedColumns_opensThroughDestructiveFallback_withoutCrashing] - the
 *    exact regression this policy exists to prevent: a v19 database seeded from the *old*,
 *    pre-drift `pending_score_submissions` shape (missing `comboCount`/`newlyUnlockedAchievementCount`,
 *    the real shape a real device had) still opens cleanly, because 19 is in the destructive range
 *    rather than requiring an incremental migration that would assume a fixed shape it never had.
 * 3. [currentVersionDatabase_roundTripsWithNoDataLoss] - a database at the current schema version
 *    closes and reopens with zero data loss and no migration triggered at all - the "upgrading to
 *    the same version you're already on never touches your data" baseline every real install sits
 *    on right now.
 *
 * Requires a connected device/emulator (real SQLite), same as every other Room-backed test in this
 * `androidTest` source set already does - not runnable as a local JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun legacyVersionDatabase_opensThroughDestructiveFallback_withoutCrashing() = runTest {
        // Seeds a real v1 SQLite file from the exported schema - the same file shape a device
        // still on that ancient dev build would have (v1 predates every column/table this app has
        // added since; see this class's own doc for why no such device actually exists in the
        // wild, this is a defensive test of the *mechanism*, not a real migration path).
        migrationTestHelper.createDatabase(TEST_DB_NAME, 1).close()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            TEST_DB_NAME,
        )
            .fallbackToDestructiveMigrationFrom(*Migrations.LEGACY_DESTRUCTIBLE_VERSIONS)
            .addMigrations(*Migrations.ALL)
            .build()

        // Any real DAO call forces Room to actually open (and, since no Migration covers this
        // version, destructively recreate) the underlying file - if the destructive-fallback
        // configuration were wrong, this throws instead of returning cleanly.
        assertEquals(emptyList<RemoteConfigCacheEntity>(), database.remoteConfigDao().getAll())
        database.close()
    }

    @Test
    fun version19DatabaseWithDriftedColumns_opensThroughDestructiveFallback_withoutCrashing() = runTest {
        // Seeds a real v19 database, then manually reverts pending_score_submissions to the exact
        // pre-drift shape a real device had (missing comboCount/newlyUnlockedAchievementCount) -
        // reproducing the precise mismatch that crashed a real device with "Migration didn't
        // properly handle: pending_score_submissions - Expected 12 columns, Found 10" when this
        // hop was previously handled by an incremental Migration instead of the destructive
        // fallback. See Migrations' own doc for the full story.
        migrationTestHelper.createDatabase(TEST_DB_NAME, 19).apply {
            execSQL("DROP TABLE pending_score_submissions")
            execSQL(
                "CREATE TABLE pending_score_submissions (" +
                    "localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "mode TEXT NOT NULL, levelSeed INTEGER NOT NULL, finalScore INTEGER NOT NULL, " +
                    "sceneAccuracy REAL NOT NULL, playedOnEpochDay INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL, retryCount INTEGER NOT NULL, " +
                    "submissionNonce TEXT NOT NULL)",
            )
            close()
        }

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            TEST_DB_NAME,
        )
            .fallbackToDestructiveMigrationFrom(*Migrations.LEGACY_DESTRUCTIBLE_VERSIONS)
            .addMigrations(*Migrations.ALL)
            .build()

        // Would throw "Migration didn't properly handle..." if 19 required an incremental
        // Migration instead of falling back to a full, correct-by-construction recreate.
        assertEquals(emptyList<PendingScoreSubmissionEntity>(), database.pendingScoreSubmissionDao().getAll())
        database.close()
    }

    @Test
    fun currentVersionDatabase_roundTripsWithNoDataLoss() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB_NAME)
            .fallbackToDestructiveMigrationFrom(*Migrations.LEGACY_DESTRUCTIBLE_VERSIONS)
            .addMigrations(*Migrations.ALL)
            .build()
            .apply {
                remoteConfigDao().upsert(listOf(RemoteConfigCacheEntity("migration_test_key", "value", 1_000L)))
            }
            .close()

        // Reopening at the exact same version must never invoke the destructive path (there is no
        // version change to migrate across) - if it did, this row would be gone.
        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB_NAME)
            .fallbackToDestructiveMigrationFrom(*Migrations.LEGACY_DESTRUCTIBLE_VERSIONS)
            .addMigrations(*Migrations.ALL)
            .build()

        assertEquals("value", reopened.remoteConfigDao().getByKey("migration_test_key")?.value)
        reopened.close()
    }

    private companion object {
        const val TEST_DB_NAME = "migration-test.db"
    }
}
