package com.suman.memoryarchitect.ui.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.auth.GoogleLinkError
import com.suman.memoryarchitect.core.auth.signInWithGoogle
import com.suman.memoryarchitect.feature.auth.SignInGateViewModel
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.PrimaryButton
import com.suman.memoryarchitect.ui.components.warmGlowOverlay
import com.suman.memoryarchitect.ui.components.warmGradientBackground
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import kotlinx.coroutines.launch

/** The mandatory, non-dismissible "sign in to play" overlay - same visual shape as
 * [com.suman.memoryarchitect.ui.screens.nointernet.NoInternetScreen], and shown by
 * [com.suman.memoryarchitect.ui.ConnectivityGate] the exact same way (a full-screen overlay on top
 * of the permanently-mounted nav host, with the back button backgrounding the app rather than
 * dismissing it). There is deliberately no dismiss affordance anywhere on this screen - tapping the
 * sign-in button again *is* the retry, for every failure mode (offline, no Google account on the
 * device, the player cancelling the picker). */
@Composable
fun SignInGateScreen(viewModel: SignInGateViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLinking by viewModel.isLinking.collectAsStateWithLifecycle()
    val linkError by viewModel.linkError.collectAsStateWithLifecycle()

    Box(modifier = Modifier.warmGradientBackground().warmGlowOverlay().fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard(modifier = Modifier.padding(32.dp)) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.sign_in_gate_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MemoryArchitectColors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.sign_in_gate_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MemoryArchitectColors.textSecondary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                )
                if (linkError != null) {
                    Text(
                        text = stringResource(
                            when (linkError) {
                                GoogleLinkError.ALREADY_LINKED_ELSEWHERE -> R.string.settings_account_link_error_already_linked
                                GoogleLinkError.NO_GOOGLE_ACCOUNT -> R.string.settings_account_link_error_no_google_account
                                else -> R.string.settings_account_link_error_unknown
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MemoryArchitectColors.danger,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                }
                if (isLinking) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentGold)
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.settings_account_sign_in_google),
                        onClick = {
                            scope.launch {
                                signInWithGoogle(
                                    context = context,
                                    onIdToken = viewModel::linkWithGoogle,
                                    onFailure = viewModel::reportSignInPickerFailure,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
