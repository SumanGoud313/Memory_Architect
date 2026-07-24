package com.suman.memoryarchitect.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suman.memoryarchitect.core.analytics.AnalyticsLogger
import com.suman.memoryarchitect.core.analytics.logHapticsToggled
import com.suman.memoryarchitect.core.feedback.AudioSettings
import com.suman.memoryarchitect.core.feedback.AudioSettingsManager
import com.suman.memoryarchitect.domain.usecase.ResetProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val audioSettingsManager: AudioSettingsManager,
    private val resetProgressUseCase: ResetProgressUseCase,
    private val analytics: AnalyticsLogger,
) : ViewModel() {

    /** Everything Sound and Haptics needs, already combined and reactive - see
     * [AudioSettingsManager]. [com.suman.memoryarchitect.ui.screens.settings.SettingsScreen] reads
     * individual fields off this (e.g. `audioSettings.hapticsEnabled`) rather than the ViewModel
     * exposing one StateFlow per preference. */
    val audioSettings: StateFlow<AudioSettings> = audioSettingsManager.settings

    private val _isResetting = MutableStateFlow(false)
    val isResetting: StateFlow<Boolean> = _isResetting.asStateFlow()

    fun setHapticsEnabled(enabled: Boolean) {
        analytics.logHapticsToggled(enabled)
        viewModelScope.launch { audioSettingsManager.setHapticsEnabled(enabled) }
    }

    fun setReduceHaptics(enabled: Boolean) {
        viewModelScope.launch { audioSettingsManager.setReduceHaptics(enabled) }
    }

    fun setMuteAll(enabled: Boolean) {
        viewModelScope.launch { audioSettingsManager.setMuteAll(enabled) }
    }

    fun setMasterVolume(volume: Float) {
        viewModelScope.launch { audioSettingsManager.setMasterVolume(volume) }
    }

    fun setMusicVolume(volume: Float) {
        viewModelScope.launch { audioSettingsManager.setMusicVolume(volume) }
    }

    fun setEffectsVolume(volume: Float) {
        viewModelScope.launch { audioSettingsManager.setEffectsVolume(volume) }
    }

    /** [onComplete] pops back to Home and re-triggers its load so the wipe is visible immediately. */
    fun resetProgress(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isResetting.value = true
            resetProgressUseCase()
            _isResetting.value = false
            onComplete()
        }
    }
}
