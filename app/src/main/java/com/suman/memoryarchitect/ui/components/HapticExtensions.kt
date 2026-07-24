package com.suman.memoryarchitect.ui.components

import androidx.compose.runtime.Composable
import com.suman.memoryarchitect.core.feedback.ui.rememberFeedback

/** The single choke point every tappable composable calls for its tap feedback (sound + haptic),
 * instead of touching [androidx.compose.ui.hapticfeedback.HapticFeedback] or any audio API
 * directly - see [com.suman.memoryarchitect.core.feedback.FeedbackManager], which this delegates
 * to and which alone decides whether/how loud a tap is, based on the player's Settings. */
@Composable
fun rememberHapticsTick(): () -> Unit {
    val feedback = rememberFeedback()
    return { feedback.onUiTap() }
}
