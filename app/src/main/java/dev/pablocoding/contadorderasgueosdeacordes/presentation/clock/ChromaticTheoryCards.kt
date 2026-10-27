package dev.pablocoding.contadorderasgueosdeacordes.presentation.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.ChromaticWheelData
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.NoteNotationMode
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberGold
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberLight
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.EbonyDark
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.IvoryBone
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.OiledMahogany
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.RosewoodLight

@Composable
fun ChromaticTheoryCards(
    notationMode: NoteNotationMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // KEY CONCEPT Card (Luthier Tonewood framed card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AmberGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "KEY CONCEPT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberGold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "All natural notes (amber) have accidental notes (rosewood) in between...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryBone
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OiledMahogany.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberLight.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "EXCEPT E-F and B-C!\nThey are naturally adjacent: only 1 fret apart on the guitar neck, with no sharp or flat in between.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmberLight,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // Clock Motion & Direction Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "🔄 Fretboard Motion & Clock Direction",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = IvoryBone
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Clockwise Box
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "↷ Clockwise",
                                fontWeight = FontWeight.Bold,
                                color = AmberGold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Sharps (♯)\n• +1 Semitone\n• Up +1 fret (toward body)",
                                fontSize = 11.sp,
                                color = IvoryBone
                            )
                        }
                    }

                    // Counter-Clockwise Box
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "↶ Counter-Clockwise",
                                fontWeight = FontWeight.Bold,
                                color = RosewoodLight,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Flats (♭)\n• -1 Semitone\n• Down -1 fret (toward headstock)",
                                fontSize = 11.sp,
                                color = IvoryBone
                            )
                        }
                    }
                }
            }
        }

        // English Letters ↔ Solfège Quick Reference
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "📖 Letters ↔ Solfège Quick Reference",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = IvoryBone
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The English alphabet system starts at A (= La), while Solfège traditionally starts from Do (= C):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                val pairs = listOf(
                    "C" to "Do", "D" to "Re", "E" to "Mi", "F" to "Fa",
                    "G" to "Sol", "A" to "La", "B" to "Si"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pairs.forEach { (anglo, solfege) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = anglo,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AmberGold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = solfege,
                                    fontWeight = FontWeight.Medium,
                                    color = IvoryBone,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChromaticIntervalCalculator(
    calcRootPitchClass: Int,
    calcTargetPitchClass: Int,
    semitones: Int,
    intervalInfo: Pair<String, String>,
    notationMode: NoteNotationMode,
    onRootSelected: (Int) -> Unit,
    onTargetSelected: (Int) -> Unit,
    onPlayInterval: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📐 Fret & Interval Calculator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IvoryBone
            )
            Text(
                text = "Choose two notes to calculate the fret distance on guitar:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Selectors for Root and Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Root Picker
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Root Note (Tonic):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NoteDropdownChip(
                        currentPitchClass = calcRootPitchClass,
                        notationMode = notationMode,
                        onSelected = onRootSelected
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AmberGold,
                    modifier = Modifier.size(20.dp)
                )

                // Target Picker
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Target Note:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NoteDropdownChip(
                        currentPitchClass = calcTargetPitchClass,
                        notationMode = notationMode,
                        onSelected = onTargetSelected
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Result Display Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$semitones Frets on guitar (= $semitones semitones)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Interval: ${intervalInfo.first}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IvoryBone
                    )
                    Text(
                        text = intervalInfo.second,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteDropdownChip(
    currentPitchClass: Int,
    notationMode: NoteNotationMode,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val note = ChromaticWheelData.NOTES[currentPitchClass]

    Box {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (note.isNatural) AmberGold else OiledMahogany,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Box(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = note.compactDualName(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (note.isNatural) EbonyDark else IvoryBone
                )
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(EbonyDark)
        ) {
            ChromaticWheelData.NOTES.forEach { item ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = item.compactDualName(),
                            color = if (item.isNatural) AmberGold else IvoryBone
                        )
                    },
                    onClick = {
                        onSelected(item.pitchClass)
                        expanded = false
                    }
                )
            }
        }
    }
}
