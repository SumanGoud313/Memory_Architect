package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.common.toSecondsLabel
import com.suman.memoryarchitect.core.feedback.ResultMood
import com.suman.memoryarchitect.core.feedback.ui.rememberFeedback
import com.suman.memoryarchitect.domain.model.AchievementId
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.LevelCompletionOutcome
import com.suman.memoryarchitect.domain.model.RewardDefinition
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.model.ScoreSubmissionResult
import com.suman.memoryarchitect.domain.progression.XpCurve
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog
import com.suman.memoryarchitect.feature.gameplay.GameplayUiState
import com.suman.memoryarchitect.ui.components.AnimatedCounter
import com.suman.memoryarchitect.ui.components.ConfettiBurst
import com.suman.memoryarchitect.ui.components.OutlineButton
import com.suman.memoryarchitect.ui.components.PillBadge
import com.suman.memoryarchitect.ui.components.PopInOvershoot
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.StarRow
import com.suman.memoryarchitect.ui.components.UnlockBurst
import com.suman.memoryarchitect.ui.components.confettiBurst
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.shakeOnTrigger
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.illustration.idlePulse
import com.suman.memoryarchitect.ui.screens.profile.AchievementRow
import com.suman.memoryarchitect.ui.screens.profile.LevelProgressRing
import com.suman.memoryarchitect.ui.screens.profile.RewardRow
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.delay

private const val CELEBRATION_BURST_DELAY_MS = 120L

/** Emotional tier for the hero title — independent of (but informed by) the raw star rating, so
 * a flawless placement run stands out even above a normal 3-star result. */
private enum class ResultTier { MEMORY_MASTER, PERFECT, GREAT, GOOD_TRY, ALMOST_THERE }

private fun resultTier(stars: Int, accuracy: Float): ResultTier = when {
    accuracy >= 0.999f -> ResultTier.MEMORY_MASTER
    stars == 3 -> ResultTier.PERFECT
    stars == 2 -> ResultTier.GREAT
    stars == 1 -> ResultTier.GOOD_TRY
    else -> ResultTier.ALMOST_THERE
}

private fun ResultTier.titleRes(): Int = when (this) {
    ResultTier.MEMORY_MASTER -> R.string.results_tier_memory_master
    ResultTier.PERFECT -> R.string.results_tier_perfect
    ResultTier.GREAT -> R.string.results_tier_great
    ResultTier.GOOD_TRY -> R.string.results_tier_good_try
    ResultTier.ALMOST_THERE -> R.string.results_tier_almost_there
}

private fun ResultTier.color(): Color = when (this) {
    ResultTier.MEMORY_MASTER, ResultTier.PERFECT -> MemoryArchitectColors.accentGold
    ResultTier.GREAT -> MemoryArchitectColors.accentSage
    ResultTier.GOOD_TRY, ResultTier.ALMOST_THERE -> MemoryArchitectColors.accentTerracotta
}

/** Compose results screen — tiered hero title, full scoring breakdown, best streak, new-record
 * and level-up celebrations, and either a single "Continue" (non-Classic) or the Classic
 * pass/fail button set. Never shows red/punishing styling — even a 0-star run stays upbeat. */
@Composable
fun GameplayResultsPanel(
    state: GameplayUiState.Finished,
    onContinue: () -> Unit,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit,
    onBackToLevels: () -> Unit,
    equippedConfettiId: CosmeticId? = null,
    equippedVictoryAnimationId: CosmeticId? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val result = state.result
    val levelOutcome = state.levelOutcome
    val submission = state.submission
    val tier = remember(state.stars, result.sceneAccuracy) { resultTier(state.stars, result.sceneAccuracy) }
    val confettiState = rememberParticleFieldState(ambientCount = 0)
    val isCelebratory = tier == ResultTier.PERFECT || tier == ResultTier.MEMORY_MASTER
    val feedback = rememberFeedback()

    LaunchedEffect(state.stars, equippedConfettiId) {
        val count = when {
            state.stars <= 0 -> 0
            state.stars == 1 -> 20
            else -> 60
        }
        if (count > 0) {
            delay(CELEBRATION_BURST_DELAY_MS)
            val confettiColors = equippedConfettiId?.let { CosmeticVisualCatalog.get(it).gradientColors }
            if (confettiColors != null) {
                confettiState.confettiBurst(Offset(400f, 300f), colors = confettiColors, count = count)
            } else {
                confettiState.confettiBurst(Offset(400f, 300f), count = count)
            }
        }
    }

    // Victory Animation used to just fire a second, differently-colored confetti burst - mechanically
    // identical to Confetti Effect, which the cosmetic audit flagged as the two categories' central
    // overlap (neither reads as a distinct purchase). It's now a genuinely different effect - an
    // expanding radial flare behind the results title (see [VictoryTitleFlare]) - rather than more
    // particles. Gated on state.stars > 0 (not the rarer isCelebratory tier) so a cosmetic a player
    // paid coins for is visible on any normal win, not just a near-flawless run; unchanged from before.
    val victoryFlareColors = remember(state.stars, equippedVictoryAnimationId) {
        if (state.stars > 0 && equippedVictoryAnimationId != null) {
            CosmeticVisualCatalog.get(equippedVictoryAnimationId).gradientColors
        } else {
            null
        }
    }

    // Fires once per freshly-revealed result - never re-triggers on unrelated recompositions
    // since [tier] is itself remember()'d off (stars, accuracy), both stable for a given round.
    LaunchedEffect(tier) {
        feedback.onResultsRevealed(
            mood = when (tier) {
                ResultTier.MEMORY_MASTER, ResultTier.PERFECT -> ResultMood.VICTORY
                ResultTier.GREAT -> ResultMood.GREAT
                ResultTier.GOOD_TRY, ResultTier.ALMOST_THERE -> ResultMood.ENCOURAGE
            },
            passed = state.passed,
        )
    }

    // Reward cues, timed to roughly land alongside each section's own staggeredReveal(index) -
    // see StaggeredReveal.kt's baseDelayMs=60/staggerMs=90 - so a coin/XP/star sound never plays
    // before the number it's confirming has actually appeared on screen.
    LaunchedEffect(submission) {
        val data = submission ?: return@LaunchedEffect
        delay(60L + 4 * 90L) // XpAndCoinsSection, index 4
        if (data.xpAwarded > 0) feedback.onXpAwarded()
        if (data.coinsAwarded > 0) feedback.onCoinsAwarded()
        if (state.stars > 0) feedback.onStarAwarded()
        if (data.newlyUnlockedAchievements.isNotEmpty() || data.newlyUnlockedRewards.isNotEmpty()) {
            delay(60L + 7 * 90L - (60L + 4 * 90L)) // AchievementUnlocksSection/RewardUnlocksSection, index 7
            feedback.onAchievementUnlocked()
        }
        if (levelOutcome?.nextLevelUnlocked == true) {
            delay(60L + 8 * 90L - (60L + 7 * 90L)) // LevelUnlockBanner, index 8
            feedback.onLevelUnlocked()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ConfettiBurst(state = confettiState, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .shakeOnTrigger(trigger = isCelebratory),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ResultsTitle(
                tier = tier,
                levelOutcome = levelOutcome,
                stars = state.stars,
                victoryFlareColors = victoryFlareColors,
                modifier = Modifier.staggeredReveal(0),
            )

            StarRow(stars = state.stars, modifier = Modifier.padding(top = 16.dp).staggeredReveal(1))
            Text(
                text = starsPhrase(context, state.stars),
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp).staggeredReveal(1),
            )

            AnimatedCounter(
                target = result.finalScore,
                style = MaterialTheme.typography.displayLarge,
                color = MemoryArchitectColors.accentGold,
                modifier = Modifier.padding(top = 24.dp).staggeredReveal(2),
            )

            ScoringBreakdown(result = result, modifier = Modifier.padding(top = 8.dp).staggeredReveal(3))

            XpAndCoinsSection(state = state, modifier = Modifier.padding(top = 16.dp).staggeredReveal(4))

            if (submission != null) {
                StreakAndRecordSection(
                    submission = submission,
                    levelOutcome = levelOutcome,
                    result = result,
                    modifier = Modifier.padding(top = 16.dp).staggeredReveal(5),
                )
                ProgressToNextLevelSection(submission = submission, modifier = Modifier.padding(top = 16.dp).staggeredReveal(6))
            }

            if (submission?.newlyUnlockedAchievements?.isNotEmpty() == true) {
                AchievementUnlocksSection(
                    ids = submission.newlyUnlockedAchievements,
                    modifier = Modifier.padding(top = 20.dp).staggeredReveal(7),
                )
            }

            if (submission?.newlyUnlockedRewards?.isNotEmpty() == true) {
                RewardUnlocksSection(
                    definitions = submission.newlyUnlockedRewards,
                    modifier = Modifier.padding(top = 20.dp).staggeredReveal(7),
                )
            }

            if (levelOutcome != null && levelOutcome.nextLevelUnlocked) {
                LevelUnlockBanner(
                    unlockedLevelNumber = levelOutcome.levelNumber + 1,
                    modifier = Modifier.padding(top = 16.dp).staggeredReveal(8),
                )
            }

            if (levelOutcome != null) {
                CampaignActions(levelOutcome, onPlayAgain, onNextLevel, onBackToLevels, Modifier.padding(top = 32.dp).staggeredReveal(9))
            } else {
                PrimaryButton(
                    text = context.getString(R.string.gameplay_continue),
                    onClick = onContinue,
                    // Daily/Weekly's post-win Mode Select lock (see ModeCard's unlockAtEpochSecond)
                    // is stamped by the async submitScore call this state.isSubmitting tracks -
                    // gating Continue on it finishing guarantees the lock is already persisted by
                    // the time the player can navigate away, so it shows locked immediately rather
                    // than needing an extra visit to Mode Select to catch up.
                    enabled = !state.isSubmitting,
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .staggeredReveal(9)
                        .idlePulse(minScale = 0.98f, maxScale = 1.02f, periodMs = 2000),
                )
            }
        }
    }
}

@Composable
private fun ResultsTitle(
    tier: ResultTier,
    levelOutcome: LevelCompletionOutcome?,
    stars: Int,
    victoryFlareColors: List<Color>?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(tier) { scale.animateTo(1f, tween(500, easing = PopInOvershoot)) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (victoryFlareColors != null) {
            VictoryTitleFlare(colors = victoryFlareColors, modifier = Modifier.size(260.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getString(tier.titleRes()),
                style = MaterialTheme.typography.displayMedium,
                color = tier.color(),
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            )
            Text(
                text = subtitleText(context, levelOutcome),
                style = MaterialTheme.typography.bodyLarge,
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            // The 70% pass threshold is intentionally forgiving (no partial credit is needed on any
            // single object to clear a level) - this makes that cushion visible instead of leaving it
            // implicit in an accuracy formula the player never sees, so a passed-but-imperfect run
            // reads as "you're done, come back anytime," not "you have unfinished business."
            if (levelOutcome != null && levelOutcome.passed && stars < 3) {
                Text(
                    text = context.getString(R.string.gameplay_cleared_not_perfect),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MemoryArchitectColors.textTertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Victory Animation's actual effect - an expanding, fading radial flare drawn behind the results
 * title. Mechanically distinct from Confetti Effect's particle burst on purpose - the cosmetic
 * audit's central finding was that the two categories used to both just look like "more confetti."
 * One [Animatable] drives both the expanding radius and the fading alpha off the same 0..1
 * progress, so the flare can never outlive its own fade or vice versa. */
@Composable
private fun VictoryTitleFlare(colors: List<Color>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(colors) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900))
    }
    Canvas(modifier = modifier) {
        val radius = (progress.value * size.maxDimension / 2f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(colors + Color.Transparent, radius = radius),
            radius = radius,
            alpha = (1f - progress.value).coerceIn(0f, 1f),
        )
    }
}

private fun subtitleText(context: android.content.Context, outcome: LevelCompletionOutcome?): String = when {
    outcome == null -> context.getString(R.string.gameplay_finished)
    outcome.passed && outcome.isFinalLevel -> context.getString(R.string.gameplay_campaign_complete, outcome.levelNumber)
    outcome.passed -> context.getString(R.string.gameplay_level_passed, outcome.levelNumber)
    else -> context.getString(R.string.gameplay_level_failed, outcome.levelNumber)
}

@Composable
private fun ScoringBreakdown(result: ScoreResult, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val feedback = rememberFeedback()

    // A rising step-per-combo tick, synced to roughly AnimatedCounter's own 700ms count-up so the
    // "combo" feel lands with the number climbing rather than firing all at once immediately -
    // see FeedbackManager.onComboStep for why correctness is never leaked live during Reconstruct
    // and this reveal only ever happens here, after Submit.
    LaunchedEffect(result.comboCount, result.comboBonus) {
        if (result.comboBonus <= 0 || result.comboCount <= 1) return@LaunchedEffect
        val stepDelayMs = (700L / result.comboCount).coerceIn(30L, 160L)
        for (step in 1..result.comboCount) {
            feedback.onComboStep(step)
            delay(stepDelayMs)
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedCounter(
            target = result.placementScore,
            style = MaterialTheme.typography.bodyLarge,
            color = MemoryArchitectColors.textSecondary,
            formatter = { context.getString(R.string.gameplay_result_placement_score, it) },
        )
        AnimatedCounter(
            target = result.timeBonus,
            style = MaterialTheme.typography.bodyLarge,
            color = MemoryArchitectColors.textSecondary,
            formatter = { context.getString(R.string.gameplay_result_time_bonus, it) },
            modifier = Modifier.padding(top = 4.dp),
        )
        if (result.comboBonus > 0) {
            AnimatedCounter(
                target = result.comboBonus,
                style = MaterialTheme.typography.bodyLarge,
                color = MemoryArchitectColors.accentTerracotta,
                formatter = { context.getString(R.string.gameplay_result_combo_bonus_points, it) },
                modifier = Modifier.padding(top = 4.dp),
            )
            PillBadge(
                text = context.getString(R.string.gameplay_combo_bonus, result.comboCount),
                contentColor = MemoryArchitectColors.accentTerracotta,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        AnimatedCounter(
            target = (result.sceneAccuracy * 100).toInt(),
            style = MaterialTheme.typography.bodyLarge,
            color = MemoryArchitectColors.textSecondary,
            formatter = { context.getString(R.string.gameplay_result_accuracy, it) },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun XpAndCoinsSection(state: GameplayUiState.Finished, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            state.isSubmitting -> Text(
                text = context.getString(R.string.gameplay_submitting),
                color = MemoryArchitectColors.textSecondary,
            )
            state.submission != null -> {
                AnimatedCounter(
                    target = state.submission.xpAwarded.toInt(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MemoryArchitectColors.success,
                    formatter = { context.getString(R.string.gameplay_xp_awarded, it) },
                )
                Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = null,
                        tint = MemoryArchitectColors.accentGold,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    AnimatedCounter(
                        target = state.submission.coinsAwarded.toInt(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MemoryArchitectColors.accentGold,
                        formatter = { context.getString(R.string.gameplay_coins_awarded, it) },
                    )
                }
                if (state.submission.isPendingSync) {
                    Text(
                        text = context.getString(R.string.gameplay_pending_sync),
                        color = MemoryArchitectColors.textTertiary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakAndRecordSection(
    submission: ScoreSubmissionResult,
    levelOutcome: LevelCompletionOutcome?,
    result: ScoreResult,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isNewRecord = levelOutcome?.let { it.isNewBest || it.isNewBestStars }
        ?: (submission.statistics.bestScore == result.finalScore && result.finalScore > 0)

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (submission.profile.currentStreak > 0) {
            PillBadge(
                text = context.getString(R.string.profile_streak, submission.profile.currentStreak, submission.profile.longestStreak),
                icon = Icons.Filled.LocalFireDepartment,
                contentColor = MemoryArchitectColors.accentTerracotta,
            )
        }
        if (isNewRecord) {
            UnlockBurst {
                PillBadge(
                    text = context.getString(R.string.results_new_record),
                    icon = Icons.Filled.EmojiEvents,
                    contentColor = MemoryArchitectColors.accentGold,
                )
            }
        }
    }
}

@Composable
private fun ProgressToNextLevelSection(submission: ScoreSubmissionResult, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val xpCurve = remember { XpCurve() }
    val level = remember(submission.profile.xp) { xpCurve.levelForXp(submission.profile.xp) }
    val progress = remember(submission.profile.xp) { xpCurve.xpProgressWithinLevel(submission.profile.xp) }
    val fraction = if (progress.second > 0) progress.first.toFloat() / progress.second.toFloat() else 0f

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (submission.leveledUp) {
            UnlockBurst {
                PillBadge(
                    text = context.getString(R.string.results_level_up),
                    contentColor = MemoryArchitectColors.accentGold,
                )
            }
        }
        LevelProgressRing(
            level = level,
            progressFraction = fraction,
            sizeDp = 96.dp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AchievementUnlocksSection(ids: List<AchievementId>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = context.getString(R.string.profile_achievements_header),
            color = MemoryArchitectColors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        ids.forEach { id ->
            UnlockBurst(modifier = Modifier.fillMaxWidth()) {
                AchievementRow(id = id, isUnlocked = true)
            }
        }
    }
}

@Composable
private fun RewardUnlocksSection(definitions: List<RewardDefinition>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = context.getString(R.string.profile_rewards_header),
            color = MemoryArchitectColors.textPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        definitions.forEach { definition ->
            UnlockBurst(modifier = Modifier.fillMaxWidth()) {
                RewardRow(id = definition.id, kind = definition.kind, isUnlocked = true, unlockLevel = definition.unlockLevel)
            }
        }
    }
}

@Composable
private fun LevelUnlockBanner(unlockedLevelNumber: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    UnlockBurst(modifier = modifier) {
        PillBadge(
            text = context.getString(R.string.results_level_unlocked, unlockedLevelNumber),
            icon = Icons.Filled.LockOpen,
            contentColor = MemoryArchitectColors.accentGold,
        )
    }
}

@Composable
private fun CampaignActions(
    outcome: LevelCompletionOutcome,
    onPlayAgain: () -> Unit,
    onNextLevel: () -> Unit,
    onBackToLevels: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val bestTimeMs = outcome.bestTimeMs
        if (outcome.passed && bestTimeMs != null) {
            Text(
                text = context.getString(
                    if (outcome.isNewBest) R.string.gameplay_new_best_time else R.string.gameplay_best_time,
                    bestTimeMs.toSecondsLabel(),
                ),
                color = MemoryArchitectColors.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (outcome.passed && !outcome.isFinalLevel) {
                PrimaryButton(
                    text = context.getString(R.string.gameplay_next_level),
                    onClick = onNextLevel,
                    modifier = Modifier.idlePulse(minScale = 0.98f, maxScale = 1.02f, periodMs = 2000),
                )
            }
            OutlineButton(text = context.getString(R.string.gameplay_play_again), onClick = onPlayAgain)
        }
        if (outcome.passed) {
            OutlineButton(
                text = context.getString(R.string.gameplay_back_to_levels),
                onClick = onBackToLevels,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

private fun starsPhrase(context: android.content.Context, stars: Int): String {
    val glyphs = (1..3).joinToString("") { if (it <= stars) "★" else "☆" }
    val phrase = when (stars) {
        3 -> context.getString(R.string.gameplay_stars_three)
        2 -> context.getString(R.string.gameplay_stars_two)
        1 -> context.getString(R.string.gameplay_stars_one)
        else -> null
    }
    return if (phrase != null) "$glyphs  $phrase" else glyphs
}
