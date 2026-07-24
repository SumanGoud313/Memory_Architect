package com.suman.memoryarchitect.core.feedback.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.suman.memoryarchitect.core.feedback.FeedbackManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FeedbackEntryPoint {
    fun feedbackManager(): FeedbackManager
}

private fun Context.resolveFeedbackManager(): FeedbackManager =
    EntryPointAccessors.fromApplication(applicationContext, FeedbackEntryPoint::class.java).feedbackManager()

/**
 * The single Compose-side resolution point for [FeedbackManager] - for stateless composables
 * with no nearby ViewModel to inject it into (buttons, cards, dialogs). Everything from here on
 * routes through the real manager stack (audio/haptics/music); nothing in the UI layer calls
 * [android.os.Vibrator] or [android.media.AudioTrack] directly.
 *
 * A ViewModel that already needs [FeedbackManager] should constructor-inject it directly rather
 * than reach for this - this hook exists specifically for the composables that have no ViewModel
 * of their own (e.g. [com.suman.memoryarchitect.ui.components.PrimaryButton]).
 */
@Composable
fun rememberFeedback(): FeedbackManager {
    val context = LocalContext.current
    return remember(context) { context.resolveFeedbackManager() }
}
