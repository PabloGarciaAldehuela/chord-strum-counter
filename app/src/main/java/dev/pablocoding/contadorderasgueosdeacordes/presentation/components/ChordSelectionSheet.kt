package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.ChordLibrary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChordSelectionSheet(
    selectedChords: List<String>,
    onChordsSelected: (List<String>) -> Unit,
    onViewChordDiagram: (Chord) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentSelection by remember { mutableStateOf(selectedChords.toMutableList()) }

    val presets = listOf(
        "A ⇄ D" to listOf("A", "D"),
        "A ⇄ D ⇄ E" to listOf("A", "D", "E"),
        "C ⇄ G ⇄ Am" to listOf("C", "G", "Am"),
        "Em ⇄ Am" to listOf("Em", "Am")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
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

            Spacer(modifier = Modifier.height(16.dp))

            // Current Practice Progression Preview
            Text(
                text = "Current Progression: ${currentSelection.joinToString(" ➔ ")}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Presets
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (label, chords) ->
                    FilledTonalButton(
                        onClick = { currentSelection = chords.toMutableList() }
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // All Chords List
            Text(
                text = "Chord Library (Tap chip to select, tap ℹ to view diagram)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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

                    FilterChip(
                        selected = isSelected,
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
                        label = { Text(chord.name, fontWeight = FontWeight.Bold) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                        } else null,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "View diagram",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onViewChordDiagram(chord) }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onChordsSelected(currentSelection)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm Selection (${currentSelection.size} Chords)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
