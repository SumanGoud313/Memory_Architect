package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * Small rounded status pill — replaces `bg_pill_glass` usages (phase/timer/status text). An
 * optional [icon] renders before the text (e.g. a coin or streak glyph on Profile) — omitting
 * it (the default) keeps every existing text-only call site unchanged.
 */
@Composable
fun PillBadge(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MemoryArchitectColors.textPrimary,
    icon: ImageVector? = null,
) {
    GlassCard(modifier = modifier, shape = RoundedCornerShape(50)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.padding(end = 6.dp))
            }
            Text(text = text, color = contentColor)
        }
    }
}
