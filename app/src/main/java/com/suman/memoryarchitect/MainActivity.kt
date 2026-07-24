package com.suman.memoryarchitect

import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.suman.memoryarchitect.ui.ConnectivityGate
import com.suman.memoryarchitect.ui.theme.MemoryArchitectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Smooths the handoff into Home's own staggeredReveal entrance instead of the default
        // instant swap once the first frame is drawn.
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.iconView.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .alpha(0f)
                .setDuration(SPLASH_EXIT_DURATION_MS)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { splashScreenViewProvider.remove() }
                .start()
        }

        setContent {
            MemoryArchitectTheme {
                ConnectivityGate()
            }
        }
    }

    private companion object {
        const val SPLASH_EXIT_DURATION_MS = 280L
    }
}
