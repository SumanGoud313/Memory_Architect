package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** A small icon + label + value tile — a generic stat-card pattern, used on Profile today. */
@Composable
fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = MemoryArchitectColors.accentGold)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MemoryArchitectColors.textPrimary,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MemoryArchitectColors.textSecondary,
            )
        }
    }
}
