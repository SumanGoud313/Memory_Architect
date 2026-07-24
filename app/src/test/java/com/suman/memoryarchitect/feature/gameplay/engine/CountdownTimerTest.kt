package com.suman.memoryarchitect.feature.gameplay.engine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownTimerTest {

    @Test
    fun `counts down to zero and invokes onFinished`() = runTest {
        val timer = CountdownTimer(scope = this, tickIntervalMs = 100L)
        var finished = false

        timer.start(durationMs = 500L) { finished = true }
        advanceTimeBy(600L)
        runCurrent()

        assertEquals(0L, timer.remainingMs.value)
        assertTrue(finished)
    }

    @Test
    fun `pause stops ticking and resume continues from where it left off`() = runTest {
        val timer = CountdownTimer(scope = this, tickIntervalMs = 100L)
        timer.start(durationMs = 500L) {}

        advanceTimeBy(200L)
        runCurrent()
        timer.pause()
        val remainingAtPause = timer.remainingMs.value

        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(remainingAtPause, timer.remainingMs.value)

        timer.resume()
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(0L, timer.remainingMs.value)
    }
}