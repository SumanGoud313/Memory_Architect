package com.suman.memoryarchitect.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteConfigDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RemoteConfigDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.remoteConfigDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertThenGetByKey_returnsStoredValue() = runTest {
        dao.upsert(listOf(RemoteConfigCacheEntity("difficulty_curve", "{\"base\":1.0}", 1_000L)))

        val result = dao.getByKey("difficulty_curve")

        assertEquals("{\"base\":1.0}", result?.value)
    }

    @Test
    fun upsert_replacesExistingValueForSameKey() = runTest {
        dao.upsert(listOf(RemoteConfigCacheEntity("ad_cadence", "3", 1_000L)))
        dao.upsert(listOf(RemoteConfigCacheEntity("ad_cadence", "5", 2_000L)))

        val all = dao.getAll()

        assertEquals(1, all.size)
        assertEquals("5", all.single().value)
    }

    @Test
    fun deleteFetchedBefore_removesOnlyStaleEntries() = runTest {
        dao.upsert(
            listOf(
                RemoteConfigCacheEntity("stale_key", "old", 1_000L),
                RemoteConfigCacheEntity("fresh_key", "new", 5_000L),
            ),
        )

        dao.deleteFetchedBefore(olderThan = 3_000L)

        assertNull(dao.getByKey("stale_key"))
        assertEquals("new", dao.getByKey("fresh_key")?.value)
    }
}