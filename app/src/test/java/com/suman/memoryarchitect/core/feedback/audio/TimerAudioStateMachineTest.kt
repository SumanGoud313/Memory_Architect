package com.suman.memoryarchitect.core.feedback.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerAudioStateMachineTest {

    @Test
    fun `requesting normal from a fresh machine plays normal fresh`() {
        val machine = TimerAudioStateMachine()

        assertEquals(TimerAudioAction.PLAY_NORMAL_FRESH, machine.requestNormal())
        assertEquals(TimerAudioState.NORMAL, machine.state)
    }

    @Test
    fun `requesting normal repeatedly while already normal is a no-op every time`() {
        val machine = TimerAudioStateMachine()
        machine.requestNormal()

        repeat(50) {
            assertEquals(TimerAudioAction.NO_OP, machine.requestNormal())
            assertEquals(TimerAudioState.NORMAL, machine.state)
        }
    }

    @Test
    fun `requesting final after normal crossfades`() {
        val machine = TimerAudioStateMachine()
        machine.requestNormal()

        assertEquals(TimerAudioAction.CROSSFADE_TO_FINAL, machine.requestFinal())
        assertEquals(TimerAudioState.FINAL, machine.state)
    }

    @Test
    fun `requesting final from a fresh machine plays final fresh, no crossfade`() {
        val machine = TimerAudioStateMachine()

        assertEquals(TimerAudioAction.PLAY_FINAL_FRESH, machine.requestFinal())
        assertEquals(TimerAudioState.FINAL, machine.state)
    }

    @Test
    fun `requesting final repeatedly while already final is a no-op every time`() {
        val machine = TimerAudioStateMachine()
        machine.requestNormal()
        machine.requestFinal()

        repeat(50) {
            assertEquals(TimerAudioAction.NO_OP, machine.requestFinal())
            assertEquals(TimerAudioState.FINAL, machine.state)
        }
    }

    @Test
    fun `stop from none is a no-op`() {
        val machine = TimerAudioStateMachine()

        assertEquals(TimerAudioAction.NO_OP, machine.requestStop())
        assertEquals(TimerAudioState.NONE, machine.state)
    }

    @Test
    fun `stop from normal or final actually stops`() {
        val fromNormal = TimerAudioStateMachine().apply { requestNormal() }
        assertEquals(TimerAudioAction.STOP, fromNormal.requestStop())
        assertEquals(TimerAudioState.NONE, fromNormal.state)

        val fromFinal = TimerAudioStateMachine().apply { requestNormal(); requestFinal() }
        assertEquals(TimerAudioAction.STOP, fromFinal.requestStop())
        assertEquals(TimerAudioState.NONE, fromFinal.state)
    }

    @Test
    fun `a fresh start after stop plays normal fresh again, not a no-op`() {
        val machine = TimerAudioStateMachine()
        machine.requestNormal()
        machine.requestFinal()
        machine.requestStop()

        assertEquals(TimerAudioAction.PLAY_NORMAL_FRESH, machine.requestNormal())
        assertEquals(TimerAudioState.NORMAL, machine.state)
    }

    @Test
    fun `a full phase lifecycle matches the expected action sequence`() {
        val machine = TimerAudioStateMachine()

        // Phase begins.
        assertEquals(TimerAudioAction.PLAY_NORMAL_FRESH, machine.requestNormal())
        // Many ticks above the threshold keep requesting normal - all no-ops.
        repeat(30) { assertEquals(TimerAudioAction.NO_OP, machine.requestNormal()) }
        // Threshold crossed once.
        assertEquals(TimerAudioAction.CROSSFADE_TO_FINAL, machine.requestFinal())
        // Many ticks at/under the threshold keep requesting final - all no-ops.
        repeat(30) { assertEquals(TimerAudioAction.NO_OP, machine.requestFinal()) }
        // Phase ends.
        assertEquals(TimerAudioAction.STOP, machine.requestStop())
        assertEquals(TimerAudioState.NONE, machine.state)
    }
}
