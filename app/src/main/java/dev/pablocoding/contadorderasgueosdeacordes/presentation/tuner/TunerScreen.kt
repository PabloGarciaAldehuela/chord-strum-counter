package dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerError
import dev.pablocoding.contadorderasgueosdeacordes.presentation.components.KeepScreenOn
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.VintageEmerald
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val ANNOUNCE_INTERVAL_MS = 1500L
private const val LOCK_HINT_CENTS = 100.0
private const val PLACEMENT_TIP_DELAY_MS = 5000L

@Composable
fun TunerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TunerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showTuningSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // ── Microphone permission ──
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRequested = true
        viewModel.onPermissionResult(granted)
    }
    fun hasMicPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    // Re-checked on every return to the screen: the user may grant or revoke access in Settings.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        val granted = hasMicPermission()
        viewModel.onPermissionResult(granted)
        if (!granted && !permissionRequested) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    val permanentlyDenied = permissionRequested && !uiState.hasMicPermission &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
    val onRequestPermission = {
        if (permanentlyDenied) {
            context.startActivity(
                Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Screen stays on while tuning ──
    KeepScreenOn()

    // ── Haptic on every newly tuned string (not replayed after rotation) ──
    var handledInTuneEvents by rememberSaveable { mutableIntStateOf(uiState.inTuneEventCount) }
    LaunchedEffect(uiState.inTuneEventCount) {
        if (uiState.inTuneEventCount > handledInTuneEvents) {
            handledInTuneEvents = uiState.inTuneEventCount
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(uiState.showMetronomePausedMessage) {
        if (uiState.showMetronomePausedMessage) {
            viewModel.onMetronomeMessageShown()
            snackbarHostState.showSnackbar("Metronome paused while tuning")
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background),
                        radius = 1200f
                    )
                )
        ) {
            // Short screens (landscape, split screen) scroll with a fixed-height trace instead.
            val compact = maxHeight < 600.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TunerTopBar(
                    isAutoMode = uiState.isAutoMode,
                    autoModeEnabled = !uiState.tuning.isChromatic,
                    onBack = onNavigateBack,
                    onAutoModeChange = viewModel::onAutoModeChange
                )

                DiagnosticsRecordingBanner(visible = uiState.isDiagnosticsRecording)

                ErrorBanner(
                    error = uiState.error,
                    onRetry = viewModel::onRetry,
                    onDismiss = viewModel::onDismissError
                )

                TuningChip(
                    uiState = uiState,
                    onClick = { showTuningSheet = true }
                )

                Spacer(modifier = Modifier.height(8.dp))
                NoteHeader(uiState = uiState)

                val traceModifier = if (compact) Modifier.height(220.dp) else Modifier.weight(1f)
                Box(
                    modifier = traceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    if (uiState.hasMicPermission) {
                        TunerPitchTrace(
                            reading = uiState.reading,
                            toleranceCents = uiState.settings.tolerance.cents,
                            inTuneEventCount = uiState.inTuneEventCount,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PermissionCard(
                            permanentlyDenied = permanentlyDenied,
                            onRequest = onRequestPermission,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    AllTunedBanner(
                        visible = uiState.allStringsTuned,
                        onBackToPractice = onNavigateBack,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                TunerHeadstock(
                    tuning = uiState.tuning,
                    naming = uiState.settings.noteNaming,
                    activeStringIndex = uiState.activeStringIndex,
                    isActiveInTune = uiState.reading.isInTune,
                    isSounding = uiState.reading.hasPitch && !uiState.reading.isHolding,
                    tunedStrings = uiState.tunedStrings,
                    signalLevel = uiState.reading.signalLevel,
                    leftHanded = uiState.settings.leftHanded,
                    onStringTap = viewModel::onStringTapped,
                    height = if (compact) 190.dp else 230.dp
                )

                BottomBar(
                    uiState = uiState,
                    onOpenSettings = { showSettingsSheet = true }
                )
            }
        }
    }

    if (showTuningSheet) {
        TuningSelectionSheet(
            tunings = viewModel.tunings,
            selectedId = uiState.tuning.id,
            naming = uiState.settings.noteNaming,
            onSelect = viewModel::onSelectTuning,
            onDismiss = { showTuningSheet = false }
        )
    }

    if (showSettingsSheet) {
        TunerSettingsSheet(
            settings = uiState.settings,
            onReferencePitchChange = viewModel::onReferencePitchChange,
            onToleranceChange = viewModel::onToleranceChange,
            onReadingModeChange = viewModel::onReadingModeChange,
            onNoteNamingChange = viewModel::onNoteNamingChange,
            onInTuneSoundChange = viewModel::onInTuneSoundChange,
            onLeftHandedChange = viewModel::onLeftHandedChange,
            onDismiss = { showSettingsSheet = false }
        )
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun TunerTopBar(
    isAutoMode: Boolean,
    autoModeEnabled: Boolean,
    onBack: () -> Unit,
    onAutoModeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(44.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = "Tuner",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "AUTO",
                style = MaterialTheme.typography.labelMedium,
                color = if (isAutoMode && autoModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Switch(
                checked = isAutoMode && autoModeEnabled,
                onCheckedChange = onAutoModeChange,
                enabled = autoModeEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Auto string detection"
                }
            )
        }
    }
}

// ── Tuning chip ────────────────────────────────────────────────────────────────

@Composable
private fun TuningChip(uiState: TunerUiState, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceVariant,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .semantics { contentDescription = "Tuning: ${uiState.tuning.name}. Double tap to change." }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.tuning.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Text(
                    text = tuningNotesSummary(uiState.tuning, uiState.settings.noteNaming),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = scheme.onSurfaceVariant
            )
        }
    }
}

// ── Big note + hints ───────────────────────────────────────────────────────────

@Composable
private fun NoteHeader(uiState: TunerUiState) {
    val scheme = MaterialTheme.colorScheme
    val reading = uiState.reading
    val tuning = uiState.tuning
    val naming = uiState.settings.noteNaming
    val target = reading.targetMidi
        ?: uiState.lockedStringIndex?.let { tuning.strings.getOrNull(it) }

    // TalkBack: announce at most every 1.5 s, but immediately when a string reaches pitch.
    val latestAnnouncement by rememberUpdatedState(TunerFormatting.accessibilityDescription(reading, tuning, naming))
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            announcement = latestAnnouncement
            delay(ANNOUNCE_INTERVAL_MS)
        }
    }
    LaunchedEffect(reading.isInTune) { announcement = latestAnnouncement }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            }
    ) {
        if (target != null && (reading.hasPitch || uiState.lockedStringIndex != null)) {
            val note = TunerFormatting.noteLabel(target, tuning.preferFlats, naming)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = note.name,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        reading.isInTune -> VintageEmerald
                        reading.hasPitch -> scheme.onBackground
                        else -> scheme.onSurfaceVariant
                    }
                )
                Text(
                    text = note.octave.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
                )
            }
            val frequency = reading.frequencyHz
            val detected = reading.detectedMidi
            val caption = when {
                frequency == null -> "Pluck the string"
                detected != null && detected != target -> {
                    val heard = TunerFormatting.noteLabel(detected, tuning.preferFlats, naming)
                    "Hearing ${heard.name}${heard.octave} · ${TunerFormatting.frequencyLabel(frequency)}"
                }
                else -> TunerFormatting.frequencyLabel(frequency)
            }
            Text(text = caption, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        } else {
            ListeningPrompt(level = reading.signalLevel)
        }

        // Quiet instruments (e.g. an unplugged electric): if nothing has been heard since the
        // screen opened, suggest better phone placement after a few seconds.
        var heardAnything by rememberSaveable { mutableStateOf(false) }
        var showPlacementTip by remember { mutableStateOf(false) }
        LaunchedEffect(reading.hasPitch, uiState.hasMicPermission) {
            if (reading.hasPitch) heardAnything = true
            showPlacementTip = false
            if (!heardAnything && !reading.hasPitch && uiState.hasMicPermission) {
                delay(PLACEMENT_TIP_DELAY_MS)
                showPlacementTip = true
            }
        }

        val cents = reading.cents
        val hint = when {
            reading.isInputSilenced -> "Another app is using the microphone"
            reading.showNoiseHint -> "Too noisy — pluck one string at a time, close to the phone"
            showPlacementTip && !heardAnything ->
                "Not hearing your guitar? Hold the phone close to the strings, or near the amp if you're plugged in."
            uiState.isAutoMode && !tuning.isChromatic && cents != null && abs(cents) > LOCK_HINT_CENTS ->
                "Far from any string — tap a string below to lock it"
            else -> null
        }
        if (hint != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

/** Mic icon with a halo that follows the input level, so the user can see they're being heard. */
@Composable
private fun ListeningPrompt(level: Double) {
    val transition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
        label = "mic_scale"
    )
    val levelFraction by animateFloatAsState(
        targetValue = TunerFormatting.levelFraction(level),
        animationSpec = tween(120),
        label = "input_level"
    )
    val haloColor = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(32.dp)
                .drawBehind {
                    drawCircle(
                        color = haloColor.copy(alpha = 0.10f + 0.25f * levelFraction),
                        radius = size.minDimension * (0.55f + 0.6f * levelFraction)
                    )
                }
                .scale(micScale)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "🎸  Pluck a string",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Banners & cards ────────────────────────────────────────────────────────────

/**
 * Developer builds only: unmistakable reminder that the microphone is being recorded to disk for
 * diagnostics (enabled over adb). Never shown in release builds.
 */
@Composable
private fun DiagnosticsRecordingBanner(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val pulse = rememberInfiniteTransition(label = "rec_pulse")
        val dotAlpha by pulse.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
            label = "rec_dot"
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .semantics { contentDescription = "Diagnostics recording is on. Microphone audio is being saved." }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .graphicsLayer { alpha = dotAlpha }
                        .background(MaterialTheme.colorScheme.onError, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REC · Diagnostics recording on — microphone audio is being saved",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(error: TunerError?, onRetry: () -> Unit, onDismiss: () -> Unit) {
    // Permission problems are handled by the in-place permission card instead.
    val message = when (error) {
        TunerError.MicrophoneUnavailable -> "The microphone is unavailable or in use by another app."
        TunerError.RecordingFailed -> "Audio recording stopped unexpectedly."
        TunerError.PracticeSessionActive -> "Stop your practice session to use the tuner."
        TunerError.MicrophonePermissionDenied, null -> null
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                val canRetry = error != TunerError.PracticeSessionActive
                TextButton(
                    onClick = if (canRetry) onRetry else onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Text(if (canRetry) "Retry" else "Dismiss")
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(permanentlyDenied: Boolean, onRequest: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Microphone access needed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (permanentlyDenied) {
                    "The tuner listens to your strings. Enable the microphone for this app in Settings."
                } else {
                    "The tuner listens to your strings to tell you how far each one is from pitch."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (permanentlyDenied) "Open Settings" else "Allow microphone", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AllTunedBanner(visible: Boolean, onBackToPractice: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = VintageEmerald.copy(alpha = 0.18f)),
            border = BorderStroke(1.dp, VintageEmerald),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎸 Your guitar is in tune!",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onBackToPractice) {
                    Text("Back to practice", color = VintageEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Bottom bar ─────────────────────────────────────────────────────────────────

@Composable
private fun BottomBar(uiState: TunerUiState, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when {
                uiState.tuning.isChromatic -> "Chromatic mode"
                uiState.isAutoMode -> "${uiState.tunedStrings.size} / ${uiState.tuning.strings.size} strings tuned"
                else -> "Manual · tap a string to switch"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "A4 = ${uiState.settings.referencePitchHz} Hz",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
