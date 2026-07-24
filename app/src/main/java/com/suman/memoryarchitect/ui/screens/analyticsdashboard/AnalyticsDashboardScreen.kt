package com.suman.memoryarchitect.ui.screens.analyticsdashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.core.analytics.RecordedEvent
import com.suman.memoryarchitect.feature.analyticsdashboard.AnalyticsDashboardUiState
import com.suman.memoryarchitect.feature.analyticsdashboard.AnalyticsDashboardViewModel
import com.suman.memoryarchitect.ui.components.AmbientBackground
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberParticleFieldState
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** Debug-build-only developer tool - see `SettingsScreen`'s debug-gated entry point, the only
 * place this is reachable from. Shows exactly what this device has actually sent through
 * [com.suman.memoryarchitect.core.analytics.AnalyticsLogger] this process, plus basic
 * Firebase/Crashlytics/Performance Monitoring configuration status - nothing here is fetched from
 * Firebase itself (the client SDKs are write-only for events/properties), it's a local mirror of
 * what was sent. */
@Composable
fun AnalyticsDashboardScreen(onBack: () -> Unit, viewModel: AnalyticsDashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val particles = rememberParticleFieldState()

    AmbientBackground(nearParticles = particles, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            DashboardHeader(onBack)
            LazyColumn(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { FirebaseStatusCard(state) }
                item { SessionInfoCard(state) }
                if (state.userProperties.isNotEmpty()) {
                    item { UserPropertiesCard(state.userProperties) }
                }
                item {
                    Text(
                        text = "Last ${state.recentEvents.size} events",
                        style = MaterialTheme.typography.labelLarge,
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(state.recentEvents) { event -> EventRow(event) }
            }
        }
    }
}

@Composable
private fun DashboardHeader(onBack: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .pressableScale(interactionSource)
                .background(MemoryArchitectColors.glassFill, CircleShape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MemoryArchitectColors.textPrimary)
        }
        Text(
            text = "Analytics Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MemoryArchitectColors.textPrimary,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun FirebaseStatusCard(state: AnalyticsDashboardUiState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusRow("Firebase", state.firebaseConfigured)
            StatusRow("Crashlytics", state.firebaseConfigured)
            PerformanceStatusRow(configured = state.firebaseConfigured, pluginActive = state.performancePluginActive)
            InfoRow("Installation ID", state.firebaseInstallationId)
        }
    }
}

@Composable
private fun SessionInfoCard(state: AnalyticsDashboardUiState) {
    val sessionDurationMs by produceState(initialValue = 0L, key1 = state.sessionStartedAtMs) {
        val startedAt = state.sessionStartedAtMs
        if (startedAt == null) {
            value = 0L
            return@produceState
        }
        while (true) {
            value = System.currentTimeMillis() - startedAt
            delay(1_000L)
        }
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoRow("Session duration", formatDuration(sessionDurationMs))
            InfoRow("Current mode", state.currentMode ?: "—")
            InfoRow("Current level", state.currentLevelNumber?.toString() ?: "—")
        }
    }
}

@Composable
private fun UserPropertiesCard(properties: Map<String, String?>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("User properties", style = MaterialTheme.typography.labelLarge, color = MemoryArchitectColors.textSecondary)
            properties.entries.sortedBy { it.key }.forEach { (key, value) -> InfoRow(key, value ?: "—") }
        }
    }
}

@Composable
private fun EventRow(event: RecordedEvent) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(event.name, style = MaterialTheme.typography.bodyMedium, color = MemoryArchitectColors.accentGold)
                Text(
                    remember(event.loggedAt) { TIME_FORMATTER.format(event.loggedAt.atZone(ZoneId.systemDefault())) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MemoryArchitectColors.textTertiary,
                )
            }
            if (event.params.isNotEmpty()) {
                Text(
                    event.params.entries.joinToString(", ") { (k, v) -> "$k=$v" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MemoryArchitectColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, configured: Boolean) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MemoryArchitectColors.textPrimary)
        Text(
            if (configured) "Configured" else "Not configured",
            style = MaterialTheme.typography.bodyMedium,
            color = if (configured) MemoryArchitectColors.accentSage else MemoryArchitectColors.textTertiary,
        )
    }
}

/** Performance Monitoring has a real third state - see FIREBASE_SETUP.md's "Known limitation"
 * section - so it gets its own row instead of [StatusRow]'s plain configured/not-configured. */
@Composable
private fun PerformanceStatusRow(configured: Boolean, pluginActive: Boolean) {
    val (text, color) = when {
        !configured -> "Not configured" to MemoryArchitectColors.textTertiary
        pluginActive -> "Configured" to MemoryArchitectColors.accentSage
        else -> "Custom traces only" to MemoryArchitectColors.accentGold
    }
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text("Performance Monitoring", style = MaterialTheme.typography.bodyMedium, color = MemoryArchitectColors.textPrimary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MemoryArchitectColors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MemoryArchitectColors.textPrimary)
    }
}

private fun formatDuration(ms: Long): String {
    val duration = Duration.ofMillis(ms)
    val minutes = duration.toMinutes()
    val seconds = duration.minusMinutes(minutes).seconds
    return "%d:%02d".format(minutes, seconds)
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
