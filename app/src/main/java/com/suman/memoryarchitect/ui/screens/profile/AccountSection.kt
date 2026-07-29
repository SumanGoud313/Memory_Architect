package com.suman.memoryarchitect.ui.screens.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.suman.memoryarchitect.BuildConfig
import com.suman.memoryarchitect.R
import com.suman.memoryarchitect.core.auth.GoogleLinkError
import com.suman.memoryarchitect.core.auth.signInWithGoogle
import com.suman.memoryarchitect.core.feedback.ui.rememberFeedback
import com.suman.memoryarchitect.domain.model.AvatarCatalog
import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.feature.profile.AccountViewModel
import com.suman.memoryarchitect.ui.components.GlassCard
import com.suman.memoryarchitect.ui.components.pressableScale
import com.suman.memoryarchitect.ui.components.rememberHapticsTick
import com.suman.memoryarchitect.ui.theme.CosmeticVisualCatalog
import com.suman.memoryarchitect.ui.theme.MemoryArchitectColors
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Profile's Account section - verification status + Google Sign-In upgrade, avatar, and optional
 * country, all backed by [AccountViewModel]. Split into its own file since this one section owns
 * three dialogs (avatar/country pickers + the link-error alert) on top of its own Credential
 * Manager side effect - keeping it here keeps `ProfileScreen.kt` itself scannable. */
@Composable
fun AccountSection(
    viewModel: AccountViewModel,
    equippedFrameId: CosmeticId? = null,
    equippedNameColorId: CosmeticId? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isVerified by viewModel.isVerified.collectAsStateWithLifecycle()
    val googleDisplayName by viewModel.googleDisplayName.collectAsStateWithLifecycle()
    val googlePhotoUrl by viewModel.googlePhotoUrl.collectAsStateWithLifecycle()
    val isLinking by viewModel.isLinking.collectAsStateWithLifecycle()
    val linkError by viewModel.linkError.collectAsStateWithLifecycle()
    val avatarId by viewModel.avatarId.collectAsStateWithLifecycle()
    val avatarUrl by viewModel.avatarUrl.collectAsStateWithLifecycle()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()
    val avatarUploadFailed by viewModel.avatarUploadFailed.collectAsStateWithLifecycle()
    val country by viewModel.country.collectAsStateWithLifecycle()

    var showAvatarPicker by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val jpegBytes = decodeAndDownscaleToJpeg(context, uri)
            if (jpegBytes != null) viewModel.uploadAvatar(jpegBytes)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccountStatusCard(
            isVerified = isVerified,
            displayName = googleDisplayName,
            photoUrl = googlePhotoUrl,
            isLinking = isLinking,
            equippedNameColorId = equippedNameColorId,
            onSignInClick = {
                scope.launch {
                    signInWithGoogle(
                        context = context,
                        onIdToken = viewModel::linkWithGoogle,
                        onFailure = viewModel::reportSignInPickerFailure,
                    )
                }
            },
        )
        AvatarRow(avatarId = avatarId, avatarUrl = avatarUrl, equippedFrameId = equippedFrameId, onClick = { showAvatarPicker = true })
        CountryRow(country = country, onClick = { showCountryPicker = true })
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            selectedAvatarId = avatarId,
            hasCustomPhoto = avatarUrl != null,
            isUploading = isUploadingAvatar,
            onSelect = { id -> viewModel.setAvatarId(id); showAvatarPicker = false },
            onUploadPhotoClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onDismiss = { showAvatarPicker = false },
        )
    }

    if (avatarUploadFailed) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAvatarUploadError,
            title = { Text(stringResource(R.string.settings_account_avatar)) },
            text = { Text(stringResource(R.string.settings_account_avatar_upload_error)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAvatarUploadError) { Text(stringResource(R.string.action_close)) }
            },
        )
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            onSelect = { code -> viewModel.setCountry(code); showCountryPicker = false },
            onDismiss = { showCountryPicker = false },
        )
    }

    if (linkError != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLinkError,
            title = { Text(stringResource(R.string.settings_account_sign_in_google)) },
            text = {
                Text(
                    stringResource(
                        when (linkError) {
                            GoogleLinkError.ALREADY_LINKED_ELSEWHERE -> R.string.settings_account_link_error_already_linked
                            GoogleLinkError.NO_GOOGLE_ACCOUNT -> R.string.settings_account_link_error_no_google_account
                            else -> R.string.settings_account_link_error_unknown
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissLinkError) { Text(stringResource(R.string.action_close)) }
            },
        )
    }
}

/** Downscales to a fixed [AVATAR_MAX_DIMENSION_PX] max dimension and re-encodes as JPEG before
 * this ever reaches [AccountViewModel.uploadAvatar] - keeps Storage cost bounded and removes most
 * large-file/EXIF-bloat abuse vectors regardless of what the source photo actually was. Runs on
 * [Dispatchers.IO] since both decoding and re-encoding a full-resolution photo are real,
 * non-trivial CPU/IO work that must never block Compose's main thread. Returns `null` (rather than
 * throwing) on any decode failure - a corrupt/unsupported file the OS picker still let through -
 * so the caller can simply no-op instead of needing its own try/catch. */
private suspend fun decodeAndDownscaleToJpeg(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
        val scale = AVATAR_MAX_DIMENSION_PX.toFloat() / maxOf(original.width, original.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * scale).toInt().coerceAtLeast(1), (original.height * scale).toInt().coerceAtLeast(1), true)
        } else {
            original
        }
        ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, output)
            output.toByteArray()
        }
    } catch (failure: Exception) {
        Log.w("AccountSection", "Failed to decode/downscale picked avatar image", failure)
        null
    }
}

private const val AVATAR_MAX_DIMENSION_PX = 256
private const val AVATAR_JPEG_QUALITY = 85

@Composable
private fun AccountStatusCard(
    isVerified: Boolean,
    displayName: String?,
    photoUrl: String?,
    isLinking: Boolean,
    equippedNameColorId: CosmeticId?,
    onSignInClick: () -> Unit,
) {
    // Name Color's whole point is to recolor the player's actual displayed name - this identity
    // text (Google display name, or "Playing anonymously") is the one persistent "name" shown
    // anywhere in the app, unlike the milestone Title text ProfileScreen.kt separately recolors.
    val nameColorBrush = equippedNameColorId?.let { Brush.linearGradient(CosmeticVisualCatalog.get(it).gradientColors) }
    // GlassCard itself now applies the equipped Premium Border automatically (see GlassCard.kt) -
    // no manual border logic needed here anymore, this card gets it for free like every other
    // GlassCard-based surface in the app.
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (isVerified && photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = if (isVerified) Icons.Filled.Verified else Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = if (isVerified) MemoryArchitectColors.accentGold else MemoryArchitectColors.textSecondary,
                    )
                }
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = if (isVerified) {
                            displayName ?: stringResource(R.string.settings_account_verified)
                        } else {
                            stringResource(R.string.settings_account_anonymous)
                        },
                        color = if (nameColorBrush != null) Color.Unspecified else MemoryArchitectColors.textPrimary,
                        style = if (nameColorBrush != null) {
                            MaterialTheme.typography.bodyLarge.copy(brush = nameColorBrush)
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                    )
                    if (!isVerified) {
                        Text(
                            text = stringResource(R.string.settings_account_anonymous_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MemoryArchitectColors.textSecondary,
                        )
                    } else if (displayName != null) {
                        Text(
                            text = stringResource(R.string.settings_account_verified),
                            style = MaterialTheme.typography.bodySmall,
                            color = MemoryArchitectColors.textSecondary,
                        )
                    }
                }
            }
            if (!isVerified && BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
                if (isLinking) {
                    CircularProgressIndicator(color = MemoryArchitectColors.accentGold, modifier = Modifier.size(20.dp))
                } else {
                    TextButton(onClick = onSignInClick) {
                        Text(stringResource(R.string.settings_account_sign_in_google), color = MemoryArchitectColors.accentGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarRow(avatarId: String, avatarUrl: String?, equippedFrameId: CosmeticId?, onClick: () -> Unit) {
    val tick = rememberHapticsTick()
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .pressableScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { tick(); onClick() },
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                if (equippedFrameId != null) {
                    AvatarFrameGlow(frameId = equippedFrameId)
                }
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(36.dp).background(MemoryArchitectColors.glassFill, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = AvatarCatalog.glyphFor(avatarId), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_account_avatar),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MemoryArchitectColors.textPrimary,
                )
                if (avatarUrl != null) {
                    Text(
                        text = stringResource(R.string.settings_account_avatar_custom_photo_set),
                        style = MaterialTheme.typography.bodySmall,
                        color = MemoryArchitectColors.textSecondary,
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MemoryArchitectColors.textTertiary)
        }
    }
}

@Composable
private fun CountryRow(country: String?, onClick: () -> Unit) {
    val tick = rememberHapticsTick()
    val interactionSource = remember { MutableInteractionSource() }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .pressableScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) { tick(); onClick() },
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Public, contentDescription = null, tint = MemoryArchitectColors.accentGold)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_account_country),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MemoryArchitectColors.textPrimary,
                )
                Text(
                    text = country?.let { Locale.Builder().setRegion(it).build().displayCountry } ?: stringResource(R.string.settings_account_country_not_set),
                    style = MaterialTheme.typography.bodySmall,
                    color = MemoryArchitectColors.textSecondary,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MemoryArchitectColors.textTertiary)
        }
    }
}

@Composable
private fun AvatarPickerDialog(
    selectedAvatarId: String,
    hasCustomPhoto: Boolean,
    isUploading: Boolean,
    onSelect: (String) -> Unit,
    onUploadPhotoClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val feedback = rememberFeedback()
    LaunchedEffect(Unit) { feedback.onDialogOpen() }
    AlertDialog(
        onDismissRequest = { feedback.onDialogClose(); onDismiss() },
        title = { Text(stringResource(R.string.settings_account_avatar_dialog_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isUploading, onClick = onUploadPhotoClick).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (hasCustomPhoto) R.string.settings_account_avatar_change_photo else R.string.settings_account_avatar_upload_photo,
                        ),
                        color = MemoryArchitectColors.accentGold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (isUploading) {
                        CircularProgressIndicator(color = MemoryArchitectColors.accentGold, modifier = Modifier.size(18.dp))
                    }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(220.dp)) {
                    items(AvatarCatalog.options, key = { it.id }) { option ->
                        val isSelected = option.id == selectedAvatarId
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .size(48.dp)
                                .background(
                                    if (isSelected) MemoryArchitectColors.accentGold.copy(alpha = 0.24f) else MemoryArchitectColors.glassFill,
                                    CircleShape,
                                )
                                .clickable { onSelect(option.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = option.glyph, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { feedback.onDialogClose(); onDismiss() }) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun CountryPickerDialog(onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    val feedback = rememberFeedback()
    LaunchedEffect(Unit) { feedback.onDialogOpen() }
    val countries = remember {
        Locale.getISOCountries()
            .map { code -> code to Locale.Builder().setRegion(code).build().displayCountry }
            .sortedBy { it.second }
    }
    AlertDialog(
        onDismissRequest = { feedback.onDialogClose(); onDismiss() },
        title = { Text(stringResource(R.string.settings_account_country_dialog_title)) },
        text = {
            LazyColumn(modifier = Modifier.height(320.dp)) {
                item {
                    Text(
                        text = stringResource(R.string.settings_account_country_clear),
                        color = MemoryArchitectColors.textSecondary,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(vertical = 10.dp),
                    )
                }
                items(countries, key = { it.first }) { (code, name) ->
                    Text(
                        text = name,
                        color = MemoryArchitectColors.textPrimary,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(code) }.padding(vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { feedback.onDialogClose(); onDismiss() }) { Text(stringResource(R.string.action_close)) }
        },
    )
}

/** Decorative ring drawn just outside [AvatarRow]'s 36dp avatar glyph when an Avatar Frame cosmetic
 * is equipped - same Canvas-arc technique as Profile's `AvatarFrameRing`, scaled down to frame this
 * much smaller circle instead. */
@Composable
private fun AvatarFrameGlow(frameId: CosmeticId, modifier: Modifier = Modifier) {
    val spec = CosmeticVisualCatalog.get(frameId)
    Canvas(modifier = modifier.size(46.dp)) {
        val strokeWidth = size.minDimension * 0.09f
        val inset = strokeWidth / 2f
        drawArc(
            brush = Brush.sweepGradient(spec.gradientColors),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
