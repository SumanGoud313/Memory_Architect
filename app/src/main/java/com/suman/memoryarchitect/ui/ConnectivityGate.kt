package com.suman.memoryarchitect.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.MainViewModel
import com.suman.memoryarchitect.ui.navigation.MemoryArchitectNavHost
import com.suman.memoryarchitect.ui.screens.nointernet.NoInternetScreen

/**
 * Replaces the old `MainActivity` push/pop of `NoInternetFragment` as a real back-stack node.
 * The real [MemoryArchitectNavHost] stays permanently mounted underneath; a full-screen overlay
 * covers it whenever offline, and the back button backgrounds the app (never dismisses the gate
 * or touches the real back stack) while the overlay is visible — same behavior as before, just
 * expressed as a Compose overlay instead of a navigation destination.
 */
@Composable
fun ConnectivityGate(viewModel: MainViewModel = hiltViewModel()) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    MemoryArchitectNavHost(modifier = Modifier.fillMaxSize())

    AnimatedVisibility(visible = !isOnline, enter = fadeIn(), exit = fadeOut()) {
        BackHandler(enabled = true) { activity?.moveTaskToBack(true) }
        NoInternetScreen()
    }
}
