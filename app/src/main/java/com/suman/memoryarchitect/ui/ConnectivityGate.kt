package com.suman.memoryarchitect.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.MainViewModel
import com.suman.memoryarchitect.feature.auth.SignInGateViewModel
import com.suman.memoryarchitect.ui.navigation.MemoryArchitectNavHost
import com.suman.memoryarchitect.ui.screens.auth.SignInGateScreen
import com.suman.memoryarchitect.ui.screens.nointernet.NoInternetScreen
import com.suman.memoryarchitect.ui.screens.splash.SplashScreen
import com.suman.memoryarchitect.ui.screens.splash.SplashTiming
import kotlinx.coroutines.delay

/**
 * Replaces the old `MainActivity` push/pop of `NoInternetFragment` as a real back-stack node.
 * The real [MemoryArchitectNavHost] stays permanently mounted underneath; a full-screen overlay
 * covers it whenever offline, and the back button backgrounds the app (never dismisses the gate
 * or touches the real back stack) while the overlay is visible — same behavior as before, just
 * expressed as a Compose overlay instead of a navigation destination.
 *
 * The Google Sign-In gate ([SignInGateScreen]) is a second overlay, same mechanism, shown whenever
 * online but not yet verified - every player must sign in with Google before reaching any gameplay
 * screen. Checked as `isOnline && !isVerified` rather than independently of connectivity: Google
 * Sign-In cannot succeed offline anyway, so the "No Internet" overlay above already covers that
 * case, and the two overlays never need to compete for which one shows.
 *
 * [SplashScreen] is a third overlay, topmost and shown unconditionally for exactly one cold start -
 * it covers whichever of the states above happens to be true underneath (Home, No Internet, or the
 * Sign-In gate) while those initialize normally in the background, then fades away to reveal
 * whichever one is actually correct once its own entrance animation finishes. [rememberSaveable]
 * (not [androidx.compose.runtime.remember]) so a configuration change mid-splash doesn't replay it,
 * while a genuine process restart correctly does.
 */
@Composable
fun ConnectivityGate(viewModel: MainViewModel = hiltViewModel(), signInGateViewModel: SignInGateViewModel = hiltViewModel()) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isVerified by signInGateViewModel.isVerified.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    var showSplash by rememberSaveable { mutableStateOf(true) }

    MemoryArchitectNavHost(modifier = Modifier.fillMaxSize())

    AnimatedVisibility(visible = !isOnline, enter = fadeIn(), exit = fadeOut()) {
        BackHandler(enabled = true) { activity?.moveTaskToBack(true) }
        NoInternetScreen()
    }

    AnimatedVisibility(visible = isOnline && signInGateViewModel.canSignIn && !isVerified, enter = fadeIn(), exit = fadeOut()) {
        BackHandler(enabled = true) { activity?.moveTaskToBack(true) }
        SignInGateScreen(viewModel = signInGateViewModel)
    }

    AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(SplashTiming.EXIT_FADE_DURATION_MS.toInt())),
    ) {
        SplashScreen()
    }

    if (showSplash) {
        LaunchedEffect(Unit) {
            delay(SplashTiming.ENTRANCE_DURATION_MS + SplashTiming.HOLD_DURATION_MS)
            showSplash = false
        }
    }
}
