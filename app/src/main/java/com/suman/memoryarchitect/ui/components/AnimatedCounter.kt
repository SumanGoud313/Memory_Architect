package com.suman.memoryarchitect.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private const val COUNT_UP_DURATION_MS = 700

/**
 * Counts up from 0 to [target] once, the shared implementation behind every animated number on
 * the results screen (score, coins, XP, accuracy, placement score, time bonus, combo bonus) —
 * previously hand-rolled per call site. Re-triggers whenever [target] changes.
 */
@Composable
fun AnimatedCounter(
    target: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    formatter: (Int) -> String = { it.toString() },
) {
    var animatedValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) {
        val anim = Animatable(0f)
        anim.animateTo(target.toFloat(), animationSpec = tween(COUNT_UP_DURATION_MS)) {
            animatedValue = value.toInt()
        }
    }
    Text(text = formatter(animatedValue), modifier = modifier, style = style, color = color)
}
