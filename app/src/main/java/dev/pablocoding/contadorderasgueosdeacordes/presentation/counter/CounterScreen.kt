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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        ),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "View History",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Text(
                        text = "Chord Transitions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!uiState.isRunning && !uiState.isFinished) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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

                Spacer(modifier = Modifier.height(14.dp))

                if (!uiState.isRunning && !uiState.isFinished) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⏱",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "Duration: ${formatDuration(uiState.durationSeconds)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!uiState.isFinished) {
                    if (uiState.isRunning) {
                        ListeningIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = viewModel::onStop,
                            modifier = Modifier
                                .height(50.dp)
                                .fillMaxWidth(0.65f),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("STOP SESSION", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { tryStart() },
                            modifier = Modifier
                                .height(54.dp)
                                .fillMaxWidth(0.72f),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
                        ) {
                            Text("START PRACTICE 🎸", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
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

                Spacer(modifier = Modifier.height(20.dp))
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
                    lifetimeStrums = uiState.lifetimeStrums,
                    currentStreakDays = uiState.currentStreakDays,
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                chords.forEachIndexed { index, chordName ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { onChordClick(chordName) }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = chordName,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    if (index < chords.size - 1) {
                        Text(
                            text = "➔",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 4.dp else 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
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
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Metronome",
                                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
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
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isPlaying,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
                        FilledTonalButton(
                            onClick = { onBpmStep(-5) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("-5", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalIconButton(
                            onClick = { onBpmStep(-1) },
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease 1 BPM", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$bpm",
                            fontSize = 30.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FilledTonalIconButton(
                            onClick = { onBpmStep(1) },
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase 1 BPM", modifier = Modifier.size(16.dp))
                        }

                        FilledTonalButton(
                            onClick = { onBpmStep(5) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+5", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slider
                    Slider(
                        value = bpm.toFloat(),
                        onValueChange = { onBpmChange(it.toInt()) },
                        valueRange = 40f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("40 (Largo)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("240 (Presto)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
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
                        else -> MaterialTheme.colorScheme.outlineVariant
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
}

// ── Listening indicator ────────────────────────────────────────────────────────

@Composable
private fun ListeningIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
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

// ── Timer ring (Material 3 Expressive Acoustic Rosette) ──────────────────────

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

    val infiniteGlow = rememberInfiniteTransition(label = "ring_glow")
    val glowAlpha by infiniteGlow.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val glowColor = MaterialTheme.colorScheme.primary
    val inlayColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val beadCoreColor = dev.pablocoding.contadorderasgueosdeacordes.ui.theme.IvoryBone

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(268.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val strokeWidthPx = 12.dp.toPx()
            val innerPadding = strokeWidthPx / 2
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            val arcTopLeft = Offset(innerPadding, innerPadding)
            val centerOffset = Offset(size.width / 2, size.height / 2)
            val radius = (size.width - strokeWidthPx) / 2
            val innerInlayRadius = radius - 14.dp.toPx()
            val soundholeRadius = innerInlayRadius - 8.dp.toPx()

            // 1. Acoustic Soundhole Depth (radial background)
            if (soundholeRadius > 0) {
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            trackColor.copy(alpha = 0.35f),
                            trackColor.copy(alpha = 0.05f)
                        ),
                        center = centerOffset,
                        radius = soundholeRadius
                    ),
                    radius = soundholeRadius,
                    center = centerOffset
                )
            }

            // 2. Background Track (clean continuous circular ring)
            drawCircle(
                color = trackColor,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeWidthPx)
            )

            // 3. Inner Rosette Inlay Dashed Ring (from Luthier's Resonance design)
            if (innerInlayRadius > 0) {
                drawCircle(
                    color = inlayColor,
                    radius = innerInlayRadius,
                    center = centerOffset,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), 0f)
                    )
                )
            }

            // 4. Ambient Strum Glow Aura (pulsing when active)
            if (isRunning) {
                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha),
                    radius = radius + (strokeWidthPx / 2) + 2.dp.toPx(),
                    center = centerOffset,
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // 5. Active Countdown Progress Arc with Leading Indicator Bead
            if (isRunning && animatedProgress > 0f) {
                val clampedProgress = animatedProgress.coerceIn(0f, 1f)
                val sweep = clampedProgress * 360f

                if (sweep >= 359.5f) {
                    drawCircle(
                        color = progressColor,
                        radius = radius,
                        center = centerOffset,
                        style = Stroke(width = strokeWidthPx)
                    )
                } else {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )

                    // Calculate position of the leading bead at the arc's head
                    val angleRad = Math.toRadians((-90.0 + sweep))
                    val beadX = (centerOffset.x + radius * Math.cos(angleRad)).toFloat()
                    val beadY = (centerOffset.y + radius * Math.sin(angleRad)).toFloat()
                    val beadCenter = Offset(beadX, beadY)

                    // Outer bead glow
                    drawCircle(
                        color = glowColor.copy(alpha = 0.5f),
                        radius = 8.dp.toPx(),
                        center = beadCenter
                    )
                    // Inner solid bead
                    drawCircle(
                        color = beadCoreColor,
                        radius = 4.5.dp.toPx(),
                        center = beadCenter
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(counterScale)
        ) {
            Text(
                text = "$transitionCount",
                fontSize = 78.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 78.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isRunning)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                else
                    androidx.compose.ui.graphics.Color.Transparent,
                border = if (isRunning)
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                else null
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = if (isRunning) 10.dp else 0.dp,
                        vertical = if (isRunning) 3.dp else 0.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isRunning) {
                        Text(
                            text = "⏱",
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatTimeMMSS(remainingSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "TRANSITIONS",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Finished overlay ──────────────────────────────────────────────────────────

@Composable
private fun FinishedOverlay(
    count: Int,
    chords: List<String>,
    isPersonalBest: Boolean,
    lifetimeStrums: Long,
    currentStreakDays: Int,
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
            Text(
                text = "Time's up! 🎸",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Show chord progression
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                chords.forEachIndexed { index, chordName ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = chordName,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    if (index < chords.size - 1) {
                        Text(
                            text = "➔",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "$count",
                fontSize = 88.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 88.sp
            )
            Text(
                text = "chord transitions",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isPersonalBest) {
                Surface(
                    color = dev.pablocoding.contadorderasgueosdeacordes.ui.theme.VintageEmerald,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "🏆 Personal Best!",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Lifetime Stats & Streak Milestone Badge
            if (lifetimeStrums > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎸 Total: %,d strums".format(lifetimeStrums),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentStreakDays > 0) {
                            Text(
                                text = "·",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "🔥 %d %s streak".format(currentStreakDays, if (currentStreakDays == 1) "day" else "days"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("TRY AGAIN 🔁", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }

            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text("VIEW HISTORY 📊", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
                val isSelected = currentDuration == seconds
                FilledTonalButton(
                    onClick = {
                        durationSlider = seconds.toFloat()
                        onDurationSelected(seconds)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = if (isSelected)
                        ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    else ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) { Text(label, maxLines = 1, fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text("Custom: ${formatDuration(durationSlider.toInt())}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Slider(
            value = durationSlider,
            onValueChange = { durationSlider = it },
            onValueChangeFinished = { onDurationSelected(durationSlider.toInt()) },
            valueRange = 15f..300f,
            steps = 56,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        SliderLabels("15s", "5 min")

        Spacer(modifier = Modifier.height(22.dp))

        // ── Mic Sensitivity ──
        SectionLabel("🎙  Mic Sensitivity")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                sensitivitySlider < 0.33f -> "Low — only loud strums"
                sensitivitySlider < 0.67f -> "Medium — normal acoustic strumming"
                else                      -> "High — catches quiet strums"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = sensitivitySlider,
            onValueChange = { sensitivitySlider = it },
            onValueChangeFinished = { onSensitivityChange(sensitivitySlider) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        SliderLabels("Loud only", "Very quiet")

        Spacer(modifier = Modifier.height(22.dp))

        // ── Debounce ──
        SectionLabel("⚡  Minimum Gap Between Strums")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${debounceSlider.toInt()} ms",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = debounceSlider,
            onValueChange = { debounceSlider = it },
            onValueChangeFinished = { onDebounceChange(debounceSlider.toInt()) },
            valueRange = 100f..800f,
            steps = 13,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        SliderLabels("100ms (fast)", "800ms (slow)")

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SliderLabels(start: String, end: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(end,   style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

