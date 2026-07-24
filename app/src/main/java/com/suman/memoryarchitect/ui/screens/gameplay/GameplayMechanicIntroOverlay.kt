package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.domain.model.GamePhase
import com.suman.memoryarchitect.domain.model.LevelSpec
import com.suman.memoryarchitect.feature.gameplay.NewMechanic
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/**
 * A one-shot "here's a new mechanic" beat for the two campaign systems that debut mid-campaign
 * rather than at level 1 (rotation at level 30, placement order at level 55) — see
 * [com.suman.memoryarchitect.feature.gameplay.GameplayViewModel.pendingMechanicIntro]. Shows the
 * real upcoming room dimmed behind the explanation (the same level [GameplayScenePanel] is about
 * to render for real) rather than a blank dialog, so the callout reads as part of this level
 * rather than a generic interstitial. The real Memorize timer never starts until [onDismiss]
 * fires, mirroring [GameplayTutorialOverlay]'s "never eat into the player's actual time budget"
 * guarantee.
 */
@Composable
fun GameplayMechanicIntroOverlay(
    level: LevelSpec,
    mechanic: NewMechanic,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        GameplayScenePanel(
            level = level,
            phase = GamePhase.MEMORIZE,
            visibleObjects = level.objects,
            onRotate = {},
            onPickUp = {},
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(MemoryArchitectColors.bgBase.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            GlassCard(
                modifier = Modifier.padding(32.dp),
                tint = MemoryArchitectColors.accentGold.copy(alpha = 0.14f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = mechanic.icon(),
                        contentDescription = null,
                        tint = MemoryArchitectColors.accentGold,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = stringResource(mechanic.titleRes()),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MemoryArchitectColors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                    Text(
                        text = stringResource(mechanic.descriptionRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MemoryArchitectColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.gameplay_mechanic_intro_got_it),
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

private fun NewMechanic.icon(): ImageVector = when (this) {
    NewMechanic.ROTATION -> Icons.Filled.Rotate90DegreesCw
    NewMechanic.ORDER_MODE -> Icons.Filled.FormatListNumbered
}

private fun NewMechanic.titleRes(): Int = when (this) {
    NewMechanic.ROTATION -> R.string.gameplay_mechanic_intro_rotation_title
    NewMechanic.ORDER_MODE -> R.string.gameplay_mechanic_intro_order_title
}

private fun NewMechanic.descriptionRes(): Int = when (this) {
    NewMechanic.ROTATION -> R.string.gameplay_mechanic_intro_rotation_description
    NewMechanic.ORDER_MODE -> R.string.gameplay_mechanic_intro_order_description
}
