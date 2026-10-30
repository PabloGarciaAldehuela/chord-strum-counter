package dev.pablocoding.contadorderasgueosdeacordes.presentation.clock

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.ChromaticNote
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.ChromaticWheelData
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.GuitarFretPosition
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.NoteNotationMode
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberGold
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberLight
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.EbonyDark
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.IvoryBone
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.OiledMahogany
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.PhosphorBronze

@Composable
fun ChromaticFretboardMap(
    selectedNote: ChromaticNote,
    fretPositions: List<GuitarFretPosition>,
    notationMode: NoteNotationMode,
    walkStringIndex: Int,
    walkFret: Int,
    onWalkStringChanged: (Int) -> Unit,
    onWalkFretChanged: (Int) -> Unit,
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
                text = "🎸 Fretboard Positions (Frets 0 to 12)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IvoryBone
            )
            Text(
                text = "Where to find ${selectedNote.compactDualName()} across all 6 strings:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Fretboard Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(EbonyDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                FretboardCanvas(
                    fretPositions = fretPositions,
                    selectedNote = selectedNote
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Single String Fret Walk Section
            Text(
                text = "🚶 Single-String Fret Walk (1 Fret = 1 Step on the Clock)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AmberGold
            )
            Text(
                text = "Move the slider to see how each fret advances one note around the clock:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // String Selector Chips (6th string to 1st string)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChromaticWheelData.GUITAR_STRINGS.forEachIndexed { index, (_, label) ->
                    val isSelected = (index == walkStringIndex)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onWalkStringChanged(index) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AmberGold else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, PhosphorBronze.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) EbonyDark else IvoryBone
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Walk Status Banner
            val currentWalkPitch = ChromaticWheelData.pitchClassAt(walkStringIndex, walkFret)
            val currentWalkNote = ChromaticWheelData.NOTES[currentWalkPitch]
            val isGlued = currentWalkNote.isGluedWithNext && walkFret < 12

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fret $walkFret: ${currentWalkNote.displayName(notationMode)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberLight
                    )
                    if (isGlued) {
                        Text(
                            text = "💡 Next note is adjacent! (1 fret / no accidental)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = IvoryBone
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (currentWalkNote.isNatural) AmberGold else OiledMahogany,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (currentWalkNote.isNatural) "Nat" else "Acc",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentWalkNote.isNatural) EbonyDark else IvoryBone
                        )
                    }
                }
            }

            // Fret Slider (0 to 12)
            Slider(
                value = walkFret.toFloat(),
                onValueChange = { onWalkFretChanged(it.toInt()) },
                valueRange = 0f..12f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = AmberGold,
                    activeTrackColor = AmberGold,
                    inactiveTrackColor = PhosphorBronze.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 (Open)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("3", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("5", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("7", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("9", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("12 (Oct)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FretboardCanvas(
    fretPositions: List<GuitarFretPosition>,
    selectedNote: ChromaticNote
) {
    Canvas(modifier = Modifier.fillMaxWidth()) {
        val totalFrets = 12
        val stringCount = 6

        val leftPad = 28.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 14.dp.toPx()
        val bottomPad = 20.dp.toPx()

        val fretboardWidth = size.width - leftPad - rightPad
        val fretboardHeight = size.height - topPad - bottomPad

        val fretSpacing = fretboardWidth / totalFrets
        val stringSpacing = fretboardHeight / (stringCount - 1)

        // Draw Nut (Fret 0 divider)
        drawLine(
            color = IvoryBone,
            start = Offset(leftPad, topPad),
            end = Offset(leftPad, topPad + fretboardHeight),
            strokeWidth = 4.dp.toPx()
        )

        // Draw Frets (1 to 12)
        for (f in 1..totalFrets) {
            val fx = leftPad + f * fretSpacing
            drawLine(
                color = PhosphorBronze.copy(alpha = if (f in listOf(3, 5, 7, 9, 12)) 0.7f else 0.4f),
                start = Offset(fx, topPad),
                end = Offset(fx, topPad + fretboardHeight),
                strokeWidth = if (f == 12) 2.5f.dp.toPx() else 1.2f.dp.toPx()
            )
        }

        // Draw Inlay Dots at frets 3, 5, 7, 9, and double dot at 12
        val dotColor = IvoryBone.copy(alpha = 0.25f)
        val centerY = topPad + fretboardHeight / 2f
        for (f in listOf(3, 5, 7, 9)) {
            val dotX = leftPad + (f - 0.5f) * fretSpacing
            drawCircle(dotColor, radius = 3.dp.toPx(), center = Offset(dotX, centerY))
        }
        val dot12X = leftPad + 11.5f * fretSpacing
        drawCircle(dotColor, radius = 2.5f.dp.toPx(), center = Offset(dot12X, centerY - 12.dp.toPx()))
        drawCircle(dotColor, radius = 2.5f.dp.toPx(), center = Offset(dot12X, centerY + 12.dp.toPx()))

        // Draw Strings (from top: 1st string High E to bottom: 6th string Low E)
        for (s in 0 until stringCount) {
            val sy = topPad + s * stringSpacing
            val thickness = 1f.dp.toPx() + (s * 0.45f).dp.toPx()
            drawLine(
                color = PhosphorBronze.copy(alpha = 0.65f),
                start = Offset(leftPad, sy),
                end = Offset(leftPad + fretboardWidth, sy),
                strokeWidth = thickness
            )
        }

        // Draw Active Note Markers on Frets
        for (pos in fretPositions) {
            val sy = topPad + (pos.stringNumber - 1) * stringSpacing

            for (fret in pos.frets) {
                val dotX = if (fret == 0) {
                    leftPad - 12.dp.toPx()
                } else {
                    leftPad + (fret - 0.5f) * fretSpacing
                }

                drawCircle(
                    color = if (selectedNote.isNatural) AmberGold else OiledMahogany,
                    radius = if (fret == 0) 5.dp.toPx() else 6.5f.dp.toPx(),
                    center = Offset(dotX, sy)
                )
                drawCircle(
                    color = IvoryBone,
                    radius = if (fret == 0) 5.dp.toPx() else 6.5f.dp.toPx(),
                    center = Offset(dotX, sy),
                    style = Stroke(width = 1.5f.dp.toPx())
                )
            }
        }
    }
}
