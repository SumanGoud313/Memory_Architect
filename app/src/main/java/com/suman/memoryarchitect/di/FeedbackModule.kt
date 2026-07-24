package com.suman.memoryarchitect.di

import com.suman.memoryarchitect.core.feedback.AudioSettingsManager
import com.suman.memoryarchitect.core.feedback.AudioSettingsManagerImpl
import com.suman.memoryarchitect.core.feedback.FeedbackManager
import com.suman.memoryarchitect.core.feedback.FeedbackManagerImpl
import com.suman.memoryarchitect.core.feedback.audio.AudioAssetManager
import com.suman.memoryarchitect.core.feedback.audio.AudioAssetManagerImpl
import com.suman.memoryarchitect.core.feedback.audio.GameAudioManager
import com.suman.memoryarchitect.core.feedback.audio.GameAudioManagerImpl
import com.suman.memoryarchitect.core.feedback.audio.MusicManager
import com.suman.memoryarchitect.core.feedback.audio.MusicManagerImpl
import com.suman.memoryarchitect.core.feedback.audio.TimerAudioManager
import com.suman.memoryarchitect.core.feedback.audio.TimerAudioManagerImpl
import com.suman.memoryarchitect.core.feedback.haptics.HapticsManager
import com.suman.memoryarchitect.core.feedback.haptics.HapticsManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedbackModule {

    @Binds
    @Singleton
    abstract fun bindAudioAssetManager(impl: AudioAssetManagerImpl): AudioAssetManager

    @Binds
    @Singleton
    abstract fun bindGameAudioManager(impl: GameAudioManagerImpl): GameAudioManager

    @Binds
    @Singleton
    abstract fun bindMusicManager(impl: MusicManagerImpl): MusicManager

    @Binds
    @Singleton
    abstract fun bindTimerAudioManager(impl: TimerAudioManagerImpl): TimerAudioManager

    @Binds
    @Singleton
    abstract fun bindHapticsManager(impl: HapticsManagerImpl): HapticsManager

    @Binds
    @Singleton
    abstract fun bindAudioSettingsManager(impl: AudioSettingsManagerImpl): AudioSettingsManager

    @Binds
    @Singleton
    abstract fun bindFeedbackManager(impl: FeedbackManagerImpl): FeedbackManager
}
