package com.suman.memoryarchitect.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MemoryArchitectColorScheme = darkColorScheme(
    primary = MemoryArchitectColors.accentTerracotta,
    onPrimary = MemoryArchitectColors.bgBase,
    secondary = MemoryArchitectColors.accentGold,
    onSecondary = MemoryArchitectColors.bgBase,
    tertiary = MemoryArchitectColors.accentSage,
    onTertiary = MemoryArchitectColors.bgBase,
    background = MemoryArchitectColors.bgBase,
    onBackground = MemoryArchitectColors.textPrimary,
    surface = MemoryArchitectColors.bgElevated,
    onSurface = MemoryArchitectColors.textPrimary,
    surfaceVariant = MemoryArchitectColors.bgGlow,
    onSurfaceVariant = MemoryArchitectColors.textSecondary,
    error = MemoryArchitectColors.danger,
    onError = MemoryArchitectColors.textPrimary,
    outline = MemoryArchitectColors.glassStroke,
)

/**
 * Root theme wrapper. Intentionally dark-only regardless of system setting — the game should
 * look the same in every lighting condition, matching the old `Theme.MemoryArchitect` choice.
 */
@Composable
fun MemoryArchitectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MemoryArchitectColorScheme,
        typography = MemoryArchitectTypography,
        shapes = MemoryArchitectShapes,
        content = content,
    )
}
