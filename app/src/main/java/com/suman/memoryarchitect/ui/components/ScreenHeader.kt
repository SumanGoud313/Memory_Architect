package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** Circular back button + title - the same treatment `SettingsScreen`'s own header has always
 * used, pulled out here so Statistics/Leaderboard (both reached the same way, from Profile) reuse
 * it instead of a third copy of the same Row/Box/Icon/Text block. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberHapticsTick()
    val interactionSource = remember { MutableInteractionSource() }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .pressableScale(interactionSource)
                .background(MemoryArchitectColors.glassFill, CircleShape)
                .clickable(interactionSource = interactionSource, indication = null) {
                    tick()
                    onBack()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = MemoryArchitectColors.textPrimary)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MemoryArchitectColors.textPrimary,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
