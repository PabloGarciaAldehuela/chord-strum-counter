package dev.pablocoding.contadorderasgueosdeacordes.presentation.counter

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord
import dev.pablocoding.contadorderasgueosdeacordes.presentation.components.ChordDiagramSheet
import dev.pablocoding.contadorderasgueosdeacordes.presentation.components.ChordSelectionSheet
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    onNavigateToHistory: () -> Unit,
    viewModel: CounterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChordSelectionSheet by remember { mutableStateOf(false) }
    var viewingChordDiagram by remember { mutableStateOf<Chord?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Counter pulse animation — fires on every new strum detected
    var counterScale by remember { mutableFloatStateOf(1f) }
    val animatedCounterScale by animateFloatAsState(
        targetValue = counterScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "counter_scale"
    )
    LaunchedEffect(uiState.transitionCount) {
        if (uiState.isRunning && uiState.transitionCount > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            counterScale = 1.35f
            delay(120)
            counterScale = 1f
        }
    }

    // Screen always-on while running
    DisposableEffect(uiState.isRunning) {
        val window = (context as Activity).window
        if (uiState.isRunning) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onStart()
        else showPermissionDialog = true
    }

    fun tryStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.onStart()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View History",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (!uiState.isRunning && !uiState.isFinished) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Chord Transitions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // ── Selected Practice Chords Row ──
                ChordProgressionBar(
                    chords = uiState.selectedChords,
                    isRunning = uiState.isRunning,
                    onChordClick = { chordName ->
                        viewingChordDiagram = viewModel.getChord(chordName)
                    },
                    onEditClick = { showChordSelectionSheet = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                TimerRing(
                    progress = if (uiState.durationSeconds > 0)
                        uiState.remainingSeconds.toFloat() / uiState.durationSeconds.toFloat()
                    else 1f,
                    transitionCount = uiState.transitionCount,
                    counterScale = animatedCounterScale,
                    remainingSeconds = uiState.remainingSeconds,
                    isRunning = uiState.isRunning
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!uiState.isRunning && !uiState.isFinished) {
                    Text(
                        text = "Duration: ${formatDuration(uiState.durationSeconds)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!uiState.isFinished) {
                    if (uiState.isRunning) {
                        ListeningIndicator()
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = viewModel::onStop,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Stop") }
                    } else {
                        Button(
                            onClick = { tryStart() },
                            modifier = Modifier
                                .height(52.dp)
                                .fillMaxWidth(0.65f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Start", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Elegant Bottom Metronome Section ──
                MetronomeBottomCard(
                    isPlaying = uiState.isMetronomePlaying,
                    bpm = uiState.metronomeBpm,
                    beat = uiState.metronomeBeat,
                    tempoName = uiState.metronomeTempoName,
                    onToggle = { enabled ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onToggleMetronome(enabled)
                    },
                    onBpmChange = { newBpm -> viewModel.onMetronomeBpmChange(newBpm) },
                    onBpmStep = { delta ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onMetronomeBpmStep(delta)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Finished overlay
            AnimatedVisibility(
                visible = uiState.isFinished,
                enter = fadeIn(tween(400)) + scaleIn(tween(400)),
                exit = fadeOut()
            ) {
                FinishedOverlay(
                    count = uiState.transitionCount,
                    chords = uiState.selectedChords,
                    isPersonalBest = uiState.isPersonalBest,
                    onTryAgain = { tryStart() },
                    onViewHistory = onNavigateToHistory
                )
            }
        }
    }

    // Chord Selection Sheet
    if (showChordSelectionSheet) {
        ChordSelectionSheet(
            selectedChords = uiState.selectedChords,
            onChordsSelected = { viewModel.onChordsChange(it) },
            onViewChordDiagram = { chord -> viewingChordDiagram = chord },
            onDismiss = { showChordSelectionSheet = false }
        )
    }

    // Chord Diagram Viewer
    viewingChordDiagram?.let { chord ->
        ChordDiagramSheet(
            chord = chord,
            onDismiss = { viewingChordDiagram = null }
        )
    }

    // Settings sheet
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SettingsContent(
                currentDuration = uiState.durationSeconds,
                currentSensitivity = uiState.sensitivity,
                currentDebounceMs = uiState.debounceMs,
                onDurationSelected = { viewModel.onDurationChange(it) },
                onSensitivityChange = { viewModel.onSensitivityChange(it) },
                onDebounceChange = { viewModel.onDebounceChange(it) },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Microphone Required") },
            text = {
                Text("This app listens for your guitar strums to count chord transitions automatically. Please grant microphone access in Settings.")
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("OK") }
            }
        )
    }
}

// ── Chord Progression Bar ───────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChordProgressionBar(
    chords: List<String>,
    isRunning: Boolean,
    onChordClick: (String) -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                chords.forEachIndexed { index, chordName ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { onChordClick(chordName) }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = chordName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (index < chords.size - 1) {
                        Text(
                            text = "➔",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!isRunning) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Chords",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Elegant Metronome Card ───────────────────────────────────────────────────

@Composable
private fun MetronomeBottomCard(
    isPlaying: Boolean,
    bpm: Int,
    beat: Int,
    tempoName: String,
    onToggle: (Boolean) -> Unit,
    onBpmChange: (Int) -> Unit,
    onBpmStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 4.dp else 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon + Title/Tempo + Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Metronome",
                                tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Metronome",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$bpm BPM · $tempoName",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Switch(
                    checked = isPlaying,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Visual beat dots
            if (isPlaying) {
                Spacer(modifier = Modifier.height(14.dp))
                BeatVisualizer(currentBeat = beat, beatsPerMeasure = 4)
            }

            // Expandable Controls (or always visible when active)
            AnimatedVisibility(
                visible = isPlaying || isExpanded,
                enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Quick steppers & BPM Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { onBpmStep(-5) },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text("-5", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalIconButton(
                            onClick = { onBpmStep(-1) },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease 1 BPM", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$bpm",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FilledTonalIconButton(
                            onClick = { onBpmStep(1) },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase 1 BPM", modifier = Modifier.size(16.dp))
                        }

                        FilledTonalIconButton(
                            onClick = { onBpmStep(5) },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text("+5", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { onBpmChange(it.toInt()) },
                        valueRange = 40f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("40 (Largo)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("240 (Presto)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// ── 4-Beat Visualizer ────────────────────────────────────────────────────────

@Composable
private fun BeatVisualizer(
    currentBeat: Int,
    beatsPerMeasure: Int = 4
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..beatsPerMeasure) {
            val isActive = (i == currentBeat)
            val isAccent = (i == 1)

            val animatedScale by animateFloatAsState(
                targetValue = if (isActive) (if (isAccent) 1.45f else 1.25f) else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                label = "beat_dot_scale_$i"
            )

            val dotColor by animateColorAsState(
                targetValue = when {
                    isActive && isAccent -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.surface
                },
                animationSpec = tween(100),
                label = "beat_dot_color_$i"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(if (isAccent) 14.dp else 11.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

// ── Listening indicator ────────────────────────────────────────────────────────

@Composable
private fun ListeningIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Listening",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(32.dp)
                .scale(micScale)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "🎸  Listening for strums…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Timer ring ─────────────────────────────────────────────────────────────────

@Composable
private fun TimerRing(
    progress: Float,
    transitionCount: Int,
    counterScale: Float,
    remainingSeconds: Int,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "timer_progress"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(220.dp)
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 10.dp
        )
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = if (isRunning) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            strokeWidth = 10.dp
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(counterScale)
        ) {
            Text(
                text = "$transitionCount",
                fontSize = 68.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 68.sp
            )
            Text(
                text = if (isRunning) formatTimeMMSS(remainingSeconds) else "transitions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Finished overlay ──────────────────────────────────────────────────────────

@Composable
private fun FinishedOverlay(
    count: Int,
    chords: List<String>,
    isPersonalBest: Boolean,
    onTryAgain: () -> Unit,
    onViewHistory: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Time's up! 🎸", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            // Show chord progression
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                chords.forEachIndexed { index, chordName ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = chordName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (index < chords.size - 1) {
                        Text("➔", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
            }

            Text("$count", fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, lineHeight = 88.sp)
            Text("chord transitions", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))

            if (isPersonalBest) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50), modifier = Modifier.padding(top = 2.dp)) {
                    Text("🏆 Personal Best!", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onViewHistory, modifier = Modifier.fillMaxWidth()) {
                Text("View History")
            }
        }
    }
}

// ── Settings bottom sheet ──────────────────────────────────────────────────────

@Composable
private fun SettingsContent(
    currentDuration: Int,
    currentSensitivity: Float,
    currentDebounceMs: Int,
    onDurationSelected: (Int) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onDebounceChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val durationPresets = listOf(30 to "30s", 60 to "1 min", 90 to "90s", 120 to "2 min")
    var durationSlider   by remember { mutableFloatStateOf(currentDuration.toFloat()) }
    var sensitivitySlider by remember { mutableFloatStateOf(currentSensitivity) }
    var debounceSlider   by remember { mutableFloatStateOf(currentDebounceMs.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Duration ──
        SectionLabel("⏱  Practice Duration")
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            durationPresets.forEach { (seconds, label) ->
                FilledTonalButton(
                    onClick = {
                        durationSlider = seconds.toFloat()
                        onDurationSelected(seconds)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (currentDuration == seconds)
                        ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    else ButtonDefaults.filledTonalButtonColors()
                ) { Text(label, maxLines = 1) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Custom: ${formatDuration(durationSlider.toInt())}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = durationSlider,
            onValueChange = { durationSlider = it },
            onValueChangeFinished = { onDurationSelected(durationSlider.toInt()) },
            valueRange = 15f..300f,
            steps = 56,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        SliderLabels("15s", "5 min")

        Spacer(modifier = Modifier.height(20.dp))

        // ── Mic Sensitivity ──
        SectionLabel("🎙  Mic Sensitivity")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                sensitivitySlider < 0.33f -> "Low — only loud strums"
                sensitivitySlider < 0.67f -> "Medium — normal strumming"
                else                      -> "High — catches quiet strums"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Slider(
            value = sensitivitySlider,
            onValueChange = { sensitivitySlider = it },
            onValueChangeFinished = { onSensitivityChange(sensitivitySlider) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        SliderLabels("Loud only", "Very quiet")

        Spacer(modifier = Modifier.height(20.dp))

        // ── Debounce ──
        SectionLabel("⚡  Min. Gap Between Strums")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${debounceSlider.toInt()} ms",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = debounceSlider,
            onValueChange = { debounceSlider = it },
            onValueChangeFinished = { onDebounceChange(debounceSlider.toInt()) },
            valueRange = 100f..800f,
            steps = 13,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
        )
        SliderLabels("100ms (fast)", "800ms (slow)")

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SliderLabels(start: String, end: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(end,   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

// ── Formatters ─────────────────────────────────────────────────────────────────

private fun formatDuration(seconds: Int): String = when {
    seconds < 60      -> "${seconds}s"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else              -> "${seconds / 60}m ${seconds % 60}s"
}

private fun formatTimeMMSS(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return if (m > 0) "%d:%02d".format(m, s) else "%ds".format(s)
}
