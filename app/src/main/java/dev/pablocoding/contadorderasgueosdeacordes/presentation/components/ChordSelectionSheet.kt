package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.ChordLibrary

object ChordSelectionPresets {
    val defaultPresets: List<Pair<String, List<String>>> = listOf(
        "A ⇄ D" to listOf("A", "D"),
        "A ⇄ D ⇄ E" to listOf("A", "D", "E"),
        "C ⇄ G ⇄ Am" to listOf("C", "G", "Am"),
        "Em ⇄ Am" to listOf("Em", "Am"),
        "Am ⇄ Dm" to listOf("Am", "Dm"),
        "A ⇄ E" to listOf("A", "E"),
        "D ⇄ E" to listOf("D", "E"),
        "Am ⇄ E" to listOf("Am", "E"),
        "Em ⇄ D" to listOf("Em", "D"),
        "Am ⇄ Em" to listOf("Am", "Em"),
        "Am ⇄ C" to listOf("Am", "C"),
        "C ⇄ Em" to listOf("C", "Em"),
        "Big G ⇄ Cadd9" to listOf("Big G", "Cadd9"),
        "Em7 ⇄ Big G ⇄ Dadd11/F# ⇄ Cadd9" to listOf("Em7", "Big G", "Dadd11/F#", "Cadd9"),
        "G5 ⇄ Cadd9 ⇄ Dsus4" to listOf("G5", "Cadd9", "Dsus4")
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChordSelectionSheet(
    selectedChords: List<String>,
    onChordsSelected: (List<String>) -> Unit,
    onViewChordDiagram: (Chord) -> Unit,
    onDismiss: () -> Unit,
    presets: List<Pair<String, List<String>>> = ChordSelectionPresets.defaultPresets
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentSelection by remember { mutableStateOf(selectedChords.toMutableList()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎸 Select Practice Chords",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Current Practice Progression Preview Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Current: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    currentSelection.forEachIndexed { index, chordName ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = chordName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        if (index < currentSelection.size - 1) {
                            Text(
                                text = "➔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Presets
            Text(
                text = "QUICK PRESETS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (label, chords) ->
                    val isCurrentPreset = currentSelection == chords
                    FilledTonalButton(
                        onClick = { currentSelection = chords.toMutableList() },
                        shape = RoundedCornerShape(50),
                        colors = if (isCurrentPreset)
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        else
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                    ) {
                        Text(label, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // All Chords List
            Text(
                text = "CHORD LIBRARY (Tap to toggle • Long-press or tap ℹ for shape)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ChordLibrary.allChords.forEach { chord ->
                    val isSelected = currentSelection.contains(chord.name)

                    val containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = containerColor,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .combinedClickable(
                                    onClick = {
                                        if (isSelected) {
                                            if (currentSelection.size > 1) {
                                                currentSelection = currentSelection.filter { it != chord.name }.toMutableList()
                                            }
                                        } else {
                                            if (currentSelection.size < 6) {
                                                currentSelection = (currentSelection + chord.name).toMutableList()
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        onViewChordDiagram(chord)
                                    }
                                )
                                .padding(
                                    start = if (isSelected) 10.dp else 14.dp,
                                    end = 4.dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                )
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = chord.name,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = { onViewChordDiagram(chord) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View diagram for ${chord.name}",
                                    tint = if (isSelected) contentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onChordsSelected(currentSelection)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                val chordLabel = if (currentSelection.size == 1) "Chord" else "Chords"
                Text("Confirm Selection (${currentSelection.size} $chordLabel)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
