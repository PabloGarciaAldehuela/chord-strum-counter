package dev.pablocoding.contadorderasgueosdeacordes.presentation.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.NoteNotationMode
import dev.pablocoding.contadorderasgueosdeacordes.presentation.components.KeepScreenOn
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberGold
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.EbonyDark
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.IvoryBone
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.PhosphorBronze

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaticClockScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChromaticClockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    KeepScreenOn()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "THE NOTE CLOCK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = IvoryBone
                        )
                        Text(
                            text = "The Chromatic Circle of Music",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberGold
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = { viewModel.toggleAudio() }) {
                            Icon(
                                imageVector = if (uiState.isAudioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                                contentDescription = if (uiState.isAudioEnabled) "Mute tone" else "Enable tone",
                                tint = if (uiState.isAudioEnabled) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notation Switcher (Letters / Solfège / Both)
                NotationSelector(
                    currentMode = uiState.notationMode,
                    onModeSelected = { viewModel.setNotationMode(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // The Interactive Chromatic Wheel Canvas
                ChromaticClockWheel(
                    selectedPitchClass = uiState.selectedPitchClass,
                    notationMode = uiState.notationMode,
                    onNoteSelected = { pitchClass ->
                        viewModel.selectPitchClass(pitchClass, playAudio = true)
                    },
                    onCenterTapped = {
                        viewModel.playCurrentNote()
                    },
                    modifier = Modifier.fillMaxWidth(0.96f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs between Companion Learning Views
                SecondaryTabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = AmberGold,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(uiState.selectedTab),
                            color = AmberGold,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        text = {
                            Text(
                                text = "🎸 Fretboard",
                                fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == 0) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        text = {
                            Text(
                                text = "📐 Intervals",
                                fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == 1) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.setSelectedTab(2) },
                        text = {
                            Text(
                                text = "💡 Key Concept",
                                fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == 2) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Tab Content
                when (uiState.selectedTab) {
                    0 -> ChromaticFretboardMap(
                        selectedNote = uiState.selectedNote,
                        fretPositions = uiState.fretPositions,
                        notationMode = uiState.notationMode,
                        walkStringIndex = uiState.walkStringIndex,
                        walkFret = uiState.walkFret,
                        onWalkStringChanged = { viewModel.setWalkString(it) },
                        onWalkFretChanged = { viewModel.setWalkFret(it) }
                    )
                    1 -> ChromaticIntervalCalculator(
                        calcRootPitchClass = uiState.calcRootPitchClass,
                        calcTargetPitchClass = uiState.calcTargetPitchClass,
                        semitones = uiState.semitoneDistance,
                        intervalInfo = uiState.intervalInfo,
                        notationMode = uiState.notationMode,
                        onRootSelected = { viewModel.setCalcRoot(it) },
                        onTargetSelected = { viewModel.setCalcTarget(it) },
                        onPlayInterval = { viewModel.playPitch(uiState.calcTargetPitchClass) }
                    )
                    2 -> ChromaticTheoryCards(
                        notationMode = uiState.notationMode
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun NotationSelector(
    currentMode: NoteNotationMode,
    onModeSelected: (NoteNotationMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PhosphorBronze.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NoteNotationMode.entries.forEach { mode ->
                val isSelected = (mode == currentMode)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) AmberGold else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.clickable { onModeSelected(mode) }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) EbonyDark else IvoryBone
                        )
                    }
                }
            }
        }
    }
}
