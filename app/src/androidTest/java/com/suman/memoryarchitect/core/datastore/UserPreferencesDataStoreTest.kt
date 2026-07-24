package com.suman.memoryarchitect.core.datastore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferencesDataStoreTest {

    private val preferences = UserPreferencesDataStore(ApplicationProvider.getApplicationContext())

    @Before
    @After
    fun resetToDefault() = runTest {
        preferences.setThemeMode(ThemeMode.SYSTEM)
    }

    @Test
    fun themeMode_defaultsToSystem() = runTest {
        assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
    }

    @Test
    fun setThemeMode_persistsAndIsReadBack() = runTest {
        preferences.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, preferences.themeMode.first())
    }
}