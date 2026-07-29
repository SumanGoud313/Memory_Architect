package com.suman.memoryarchitect.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.ads.AdaptiveBannerAd
import com.suman.memoryarchitect.core.feedback.AudioSettings
import com.suman.memoryarchitect.core.feedback.ui.rememberFeedback
import com.suman.memoryarchitect.feature.settings.SettingsViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.components.staggeredReveal
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors

/** New screen — Home's settings gear opens this. Sound &amp; Haptics (master/music/effects volume,
 * haptics on/off, reduce haptics, mute all - all backed by
 * [com.suman.memoryarchitect.core.feedback.AudioSettingsManager]), How to Play, a destructive
 * local-progress reset behind a confirmation dialog, and static app/version info. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAnalyticsDashboard: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val audioSettings by viewModel.audioSettings.collectAsStateWithLifecycle()
    val isResetting by viewModel.isResetting.collectAsStateWithLifecycle()
    val streakReminderEnabled by viewModel.streakReminderEnabled.collectAsStateWithLifecycle()
    val dailyChallengeReminderEnabled by viewModel.dailyChallengeReminderEnabled.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showHowToPlay by remember { mutableStateOf(false) }

    // Only relevant on API 33+ (POST_NOTIFICATIONS didn't exist before) - below that, toggling a
    // reminder on just works once DailyReminderWorker next runs, no prompt needed. Requested only
    // when the player actually turns a reminder on, never speculatively at screen-open, matching
    // the "don't re-prompt aggressively" rule from the retention plan's Notifications section.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            SettingsHeader(onBack = onBack, modifier = Modifier.staggeredReveal(0))

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_sound_and_haptics),
                    style = MaterialTheme.typography.labelLarge,
                    color = MemoryArchitectColors.textTertiary,
                    modifier = Modifier.staggeredReveal(1),
                )
                MuteAllRow(
                    enabled = audioSettings.muteAll,
                    onToggle = viewModel::setMuteAll,
                    modifier = Modifier.staggeredReveal(2),
                )
                SoundAndHapticsCard(
                    audioSettings = audioSettings,
                    onMasterVolumeChange = viewModel::setMasterVolume,
                    onMusicVolumeChange = viewModel::setMusicVolume,
                    onEffectsVolumeChange = viewModel::setEffectsVolume,
                    onHapticsToggle = viewModel::setHapticsEnabled,
                    onReduceHapticsToggle = viewModel::setReduceHaptics,
                    modifier = Modifier.staggeredReveal(3),
                )
                Text(
                    text = stringResource(R.string.settings_notifications),
                    style = MaterialTheme.typography.labelLarge,
                    color = MemoryArchitectColors.textTertiary,
                    modifier = Modifier.padding(top = 8.dp).staggeredReveal(4),
                )
                NotificationsCard(
                    streakReminderEnabled = streakReminderEnabled,
                    dailyChallengeReminderEnabled = dailyChallengeReminderEnabled,
                    onStreakReminderToggle = { enabled ->
                        viewModel.setStreakReminderEnabled(enabled)
                        if (enabled) requestNotificationPermissionIfNeeded()
                    },
                    onDailyChallengeReminderToggle = { enabled ->
                        viewModel.setDailyChallengeReminderEnabled(enabled)
                        if (enabled) requestNotificationPermissionIfNeeded()
                    },
                    modifier = Modifier.staggeredReveal(5),
                )
                HowToPlayRow(
                    onClick = { showHowToPlay = true },
                    modifier = Modifier.staggeredReveal(6),
                )
                ResetProgressRow(
                    isResetting = isResetting,
                    onClick = { showResetConfirmation = true },
                    modifier = Modifier.staggeredReveal(7),
                )
                AboutRow(
                    // Debug builds only, and never advertised in the UI - tapping the app name 7
                    // times (the same convention Android's own Settings uses to reveal Developer
                    // Options) is the only way in, so a tester holding a debug build never
                    // stumbles onto internal analytics data by casually browsing Settings.
                    onSecretUnlock = if (BuildConfig.DEBUG) onOpenAnalyticsDashboard else null,
                    modifier = Modifier.staggeredReveal(8),
                )
            }

            // Explicitly "optional" per the placement audit (Settings is a low-traffic, low-
            // dwell-time screen) but included for completeness - see AdaptiveBannerAd's own doc
            // for why this renders nothing at all for a Remove Ads purchaser.
            AdaptiveBannerAd(placement = "settings", modifier = Modifier.padding(top = 12.dp))
        }
    }

    if (showResetConfirmation) {
        ResetProgressDialog(
            onConfirm = {
                showResetConfirmation = false
                viewModel.resetProgress(onComplete = onBack)
            },
            onDismiss = { showResetConfirmation = false },
        )
    }

    if (showHowToPlay) {
        HowToPlayDialog(onDismiss = { showHowToPlay = false })
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberHapticsTick()
    val interactionSource = remember { MutableInteractionSource() }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MemoryArchitectColors.textPrimary,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun MuteAllRow(enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (enabled) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MemoryArchitectColors.accentGold,
                )
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = stringResource(R.string.settings_mute_all),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MemoryArchitectColors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.settings_mute_all_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MemoryArchitectColors.textSecondary,
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = MemoryArchitectColors.accentGold),
            )
        }
    }
}

/** Volume sliders dim (but stay usable) while [AudioSettings.muteAll] is on, reading as "here's
 * what you'll hear once you unmute" rather than a locked/disabled control. */
@Composable
private fun SoundAndHapticsCard(
    audioSettings: AudioSettings,
    onMasterVolumeChange: (Float) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onEffectsVolumeChange: (Float) -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    onReduceHapticsToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().alpha(if (audioSettings.muteAll) 0.5f else 1f)) {
            VolumeSliderRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = stringResource(R.string.settings_master_volume),
                value = audioSettings.masterVolume,
                onValueChange = onMasterVolumeChange,
            )
            VolumeSliderRow(
                icon = Icons.Filled.MusicNote,
                label = stringResource(R.string.settings_music_volume),
                value = audioSettings.musicVolume,
                onValueChange = onMusicVolumeChange,
                modifier = Modifier.padding(top = 12.dp),
            )
            VolumeSliderRow(
                icon = Icons.Filled.GraphicEq,
                label = stringResource(R.string.settings_effects_volume),
                value = audioSettings.effectsVolume,
                onValueChange = onEffectsVolumeChange,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
    GlassCard(modifier = modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ToggleRow(
                icon = Icons.Filled.Vibration,
                label = stringResource(R.string.settings_haptics),
                checked = audioSettings.hapticsEnabled,
                onToggle = onHapticsToggle,
                modifier = Modifier.padding(16.dp),
            )
            ToggleRow(
                icon = Icons.Filled.Vibration,
                label = stringResource(R.string.settings_reduce_haptics),
                subtitle = stringResource(R.string.settings_reduce_haptics_subtitle),
                checked = audioSettings.reduceHaptics,
                enabled = audioSettings.hapticsEnabled,
                onToggle = onReduceHapticsToggle,
                modifier = Modifier.padding(16.dp).alpha(if (audioSettings.hapticsEnabled) 1f else 0.5f),
            )
        }
    }
}

@Composable
private fun VolumeSliderRow(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MemoryArchitectColors.accentGold, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MemoryArchitectColors.textPrimary,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = MemoryArchitectColors.accentGold,
                activeTrackColor = MemoryArchitectColors.accentGold,
            ),
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MemoryArchitectColors.accentGold)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MemoryArchitectColors.textPrimary)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MemoryArchitectColors.textSecondary)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = MemoryArchitectColors.accentGold),
        )
    }
}

/** Per-category opt-out for [com.suman.memoryarchitect.core.notifications.DailyReminderWorker] -
 * both default on (matches [com.suman.memoryarchitect.core.datastore.UserPreferencesDataStore]'s
 * own defaults), independent of whether the OS notification permission is actually granted; a
 * toggle turned on without that permission simply means [DailyReminderWorker] silently no-ops
 * until it's granted, rather than this screen blocking on it. */
@Composable
private fun NotificationsCard(
    streakReminderEnabled: Boolean,
    dailyChallengeReminderEnabled: Boolean,
    onStreakReminderToggle: (Boolean) -> Unit,
    onDailyChallengeReminderToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ToggleRow(
                icon = Icons.Filled.Notifications,
                label = stringResource(R.string.settings_streak_reminder),
                subtitle = stringResource(R.string.settings_streak_reminder_subtitle),
                checked = streakReminderEnabled,
                onToggle = onStreakReminderToggle,
                modifier = Modifier.padding(16.dp),
            )
            ToggleRow(
                icon = Icons.Filled.Notifications,
                label = stringResource(R.string.settings_daily_challenge_reminder),
                subtitle = stringResource(R.string.settings_daily_challenge_reminder_subtitle),
                checked = dailyChallengeReminderEnabled,
                onToggle = onDailyChallengeReminderToggle,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun HowToPlayRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .pressableScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = MemoryArchitectColors.accentGold)
            Text(
                text = stringResource(R.string.settings_how_to_play),
                style = MaterialTheme.typography.bodyLarge,
                color = MemoryArchitectColors.textPrimary,
                modifier = Modifier.padding(start = 14.dp).weight(1f),
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MemoryArchitectColors.textTertiary)
        }
    }
}

@Composable
private fun ResetProgressRow(isResetting: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .pressableScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, enabled = !isResetting, onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MemoryArchitectColors.danger)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_reset_progress),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MemoryArchitectColors.danger,
                )
                Text(
                    text = stringResource(R.string.settings_reset_progress_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MemoryArchitectColors.textSecondary,
                )
            }
            if (isResetting) {
                CircularProgressIndicator(color = MemoryArchitectColors.danger, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** [onSecretUnlock], when non-null (debug builds only - see the call site), fires once this row
 * is tapped [SECRET_TAP_COUNT] times in a row - the app's own hidden entry point to the debug
 * Analytics Dashboard, deliberately never advertised anywhere in the UI. A tap that lands more
 * than [SECRET_TAP_TIMEOUT_MS] after the previous one resets the count, so it has to be a real
 * deliberate rapid-tap sequence, not an accumulation of casual taps over a session. */
@Composable
private fun AboutRow(onSecretUnlock: (() -> Unit)?, modifier: Modifier = Modifier) {
    var tapCount by remember { mutableStateOf(0) }
    var lastTapAtMs by remember { mutableStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onSecretUnlock != null) {
                    Modifier
                        .pressableScale(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            val now = System.currentTimeMillis()
                            tapCount = if (now - lastTapAtMs > SECRET_TAP_TIMEOUT_MS) 1 else tapCount + 1
                            lastTapAtMs = now
                            if (tapCount >= SECRET_TAP_COUNT) {
                                tapCount = 0
                                onSecretUnlock()
                            }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // The real app logo (see MainActivity/SplashScreen.kt) rather than a generic Material
            // info glyph - this row is this app's closest equivalent to an "About" section, so it's
            // one of the few in-app spots (alongside the splash) the brand mark itself belongs.
            // No clip() here: the artwork's own rounded-square shape and dark background are
            // already baked into the image, and at this small a size any additional system clip
            // would cut into the brain/puzzle-piece marks near the corners.
            Image(
                painter = painterResource(R.drawable.memory_architect_logo),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge, color = MemoryArchitectColors.textPrimary)
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MemoryArchitectColors.textSecondary,
                )
            }
        }
    }
}

private const val SECRET_TAP_COUNT = 7
private const val SECRET_TAP_TIMEOUT_MS = 1_500L

@Composable
private fun ResetProgressDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val feedback = rememberFeedback()
    LaunchedEffect(Unit) { feedback.onDialogOpen() }
    AlertDialog(
        onDismissRequest = { feedback.onDialogClose(); onDismiss() },
        title = { Text(stringResource(R.string.settings_reset_progress_dialog_title)) },
        text = { Text(stringResource(R.string.settings_reset_progress_dialog_message)) },
        confirmButton = {
            TextButton(onClick = { feedback.onDialogClose(); onConfirm() }) {
                Text(stringResource(R.string.settings_reset_progress), color = MemoryArchitectColors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = { feedback.onDialogClose(); onDismiss() }) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** A static rules summary — the manual fallback entry point the "never show the interactive
 * tutorial again after Level 1" flow is meant to hand off to (see the onboarding follow-up work);
 * this dialog itself doesn't depend on that system and works standalone today. */
@Composable
private fun HowToPlayDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val feedback = rememberFeedback()
    LaunchedEffect(Unit) { feedback.onDialogOpen() }
    val steps = remember {
        listOf(
            R.string.settings_how_to_play_step_1,
            R.string.settings_how_to_play_step_2,
            R.string.settings_how_to_play_step_3,
            R.string.settings_how_to_play_step_4,
            R.string.settings_how_to_play_step_5,
        ).map { context.getString(it) }
    }
    AlertDialog(
        onDismissRequest = { feedback.onDialogClose(); onDismiss() },
        title = { Text(stringResource(R.string.settings_how_to_play)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, step ->
                    Row {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MemoryArchitectColors.accentGold,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(text = step, style = MaterialTheme.typography.bodyMedium, color = MemoryArchitectColors.textSecondary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { feedback.onDialogClose(); onDismiss() }) { Text(stringResource(R.string.action_close)) }
        },
    )
}
