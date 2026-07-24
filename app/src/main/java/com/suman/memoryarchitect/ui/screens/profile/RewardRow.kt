package com.suman.memoryarchitect.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toDisplayTitle
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.model.RewardKind
import com.suman.memoryarchitect.domain.progression.RewardCatalog
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.illustration.RoomArtRegistry
import com.suman.memoryarchitect.ui.theme.AccentPaletteCatalog
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

private fun RewardId.badgeTint(): Color = when (this) {
    RewardId.BADGE_BRONZE_KEY -> Color(0xFFB08D57)
    RewardId.BADGE_SILVER_KEY -> Color(0xFFC7CBD1)
    RewardId.BADGE_GOLD_KEY -> MemoryArchitectColors.accentGold
    RewardId.BADGE_PLATINUM_KEY -> Color(0xFFE0DCEB)
    RewardId.BADGE_DIAMOND_KEY -> Color(0xFFAEE9E8)
    RewardId.BADGE_OBSIDIAN_KEY -> Color(0xFF4A4458)
    RewardId.BADGE_CELESTIAL_KEY -> Color(0xFFCBB8F0)
    else -> MemoryArchitectColors.accentGold
}

/** One entry in the Profile "Rewards" list or the results-screen unlock celebration — mirrors
 * [AchievementRow]'s shape, but the icon (or, for a room theme, the actual room art) depends on
 * [RewardKind]. */
@Composable
fun RewardRow(id: RewardId, kind: RewardKind, isUnlocked: Boolean, unlockLevel: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val title = id.toDisplayTitle(context)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        tint = if (isUnlocked) MemoryArchitectColors.accentGold.copy(alpha = 0.1f) else null,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RewardGlyph(id = id, kind = kind, isUnlocked = isUnlocked)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUnlocked) MemoryArchitectColors.textPrimary else MemoryArchitectColors.textTertiary,
                )
                if (!isUnlocked) {
                    Text(
                        text = context.getString(R.string.reward_locked_at_level, unlockLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MemoryArchitectColors.textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardGlyph(id: RewardId, kind: RewardKind, isUnlocked: Boolean) {
    when {
        !isUnlocked -> Box(
            modifier = Modifier.size(40.dp).background(MemoryArchitectColors.glassFill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = MemoryArchitectColors.textTertiary)
        }
        kind == RewardKind.ROOM_THEME -> RoomThemeGlyph(id)
        kind == RewardKind.PALETTE -> PaletteGlyph(id)
        else -> Box(
            modifier = Modifier.size(40.dp).background(MemoryArchitectColors.accentGold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (kind == RewardKind.TITLE) Icons.Filled.Star else Icons.Filled.VpnKey,
                contentDescription = null,
                tint = if (kind == RewardKind.BADGE) id.badgeTint() else MemoryArchitectColors.bgBase,
            )
        }
    }
}

@Composable
private fun RoomThemeGlyph(id: RewardId) {
    val sceneId = remember(id) { RewardCatalog.roomSceneId(id) ?: "unknown" }
    val roomArt = remember(sceneId) { RoomArtRegistry.get(sceneId) }
    Box(modifier = Modifier.size(40.dp).background(Color.Black, RoundedCornerShape(10.dp))) {
        roomArt.backdrop(Modifier.fillMaxSize())
    }
}

@Composable
private fun PaletteGlyph(id: RewardId) {
    val sweep = remember(id) { AccentPaletteCatalog.get(id)?.sweep ?: listOf(MemoryArchitectColors.accentGold) }
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(brush = Brush.sweepGradient(sweep))
        }
        Icon(
            imageVector = Icons.Filled.Palette,
            contentDescription = null,
            tint = MemoryArchitectColors.bgBase,
            modifier = Modifier.size(18.dp),
        )
    }
}