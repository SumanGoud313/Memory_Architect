package com.suman.memoryarchitect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.core.common.toDisplayName
import com.suman.memoryarchitect.domain.progression.PlayerRank
import com.suman.memoryarchitect.ui.illustration.idlePulse
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** [PlayerRank]'s identity color - a metal/gem read for the first few tiers, then this game's own
 * warm accents for the top tiers so [PlayerRank.LEGEND] reads as "the best this app's own palette
 * has," not just another gem color. */
fun PlayerRank.badgeColor(): Color = when (this) {
    PlayerRank.BRONZE -> Color(0xFFCD7F32)
    PlayerRank.SILVER -> Color(0xFFC0C6CC)
    PlayerRank.GOLD -> MemoryArchitectColors.accentGold
    PlayerRank.PLATINUM -> Color(0xFFA7E5D6)
    PlayerRank.DIAMOND -> Color(0xFF8FD9E8)
    PlayerRank.MASTER -> MemoryArchitectColors.accentTerracotta
    PlayerRank.GRANDMASTER -> MemoryArchitectColors.accentCoral
    PlayerRank.LEGEND -> MemoryArchitectColors.accentGold
}

/** Icon + name pill for a [PlayerRank] - the small, inline form (leaderboard rows, Profile's
 * header). See [RankHero] for the larger Statistics Dashboard treatment. */
@Composable
fun RankBadge(rank: PlayerRank, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val color = rank.badgeColor()
    GlassCard(modifier = modifier, shape = RoundedCornerShape(50), tint = color.copy(alpha = 0.14f)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(
                text = rank.toDisplayName(context),
                color = MemoryArchitectColors.textPrimary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** The large, centered hero treatment for the Statistics Dashboard - a glowing rank medal plus
 * name, distinct from [RankBadge]'s compact inline pill. */
@Composable
fun RankHero(rank: PlayerRank, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val color = rank.badgeColor()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .idlePulse()
                    .background(color.copy(alpha = 0.16f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MemoryArchitectColors.glassFillPressed, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = color, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = rank.toDisplayName(context),
            color = MemoryArchitectColors.textPrimary,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
