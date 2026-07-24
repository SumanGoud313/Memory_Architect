package com.suman.memoryarchitect.feature.gameplay.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pausable/resumable coroutine-based countdown, ticking every [tickIntervalMs] until
 * [remainingMs] hits zero, then invoking the [start]-supplied callback once. Reused for
 * both the memorize-phase and reconstruct-phase timers.
 */
class CountdownTimer(
    private val scope: CoroutineScope,
    private val tickIntervalMs: Long = 100L,
) {
    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private var job: Job? = null
    private var onFinished: (() -> Unit)? = null

    fun start(durationMs: Long, onFinished: () -> Unit) {
        job?.cancel()
        this.onFinished = onFinished
        _remainingMs.value = durationMs
        job = scope.launch { runUntilZero() }
    }

    fun pause() {
        job?.cancel()
        job = null
    }

    fun resume() {
        if (_remainingMs.value <= 0 || job != null) return
        job = scope.launch { runUntilZero() }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    private suspend fun runUntilZero() {
        while (_remainingMs.value > 0) {
            delay(tickIntervalMs)
            _remainingMs.value = (_remainingMs.value - tickIntervalMs).coerceAtLeast(0)
        }
        onFinished?.invoke()
    }
}