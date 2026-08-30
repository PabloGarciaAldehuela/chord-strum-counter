package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord

@Composable
fun ChordDiagram(
    chord: Chord,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val fretboardColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = chord.name,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = primaryColor
        )
        Text(
            text = chord.fullName,
            style = MaterialTheme.typography.bodyMedium,
            color = onSurfaceColor.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 220.dp, height = 240.dp)
                .background(fretboardColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 180.dp, height = 200.dp)) {
                drawGuitarFretboard(
                    chord = chord,
                    primaryColor = primaryColor,
                    stringColor = onSurfaceColor.copy(alpha = 0.6f),
                    nutColor = onSurfaceColor
                )
            }
        }
    }
}

private fun DrawScope.drawGuitarFretboard(
    chord: Chord,
    primaryColor: Color,
    stringColor: Color,
    nutColor: Color
) {
    val numStrings = 6
    val numFrets = 4

    val topPadding = 28f
    val bottomPadding = 18f
    val leftPadding = 20f
    val rightPadding = 20f

    val gridWidth = size.width - leftPadding - rightPadding
    val gridHeight = size.height - topPadding - bottomPadding

    val stringSpacing = gridWidth / (numStrings - 1)
    val fretSpacing = gridHeight / numFrets

    // 1. Draw Nut (or base fret line)
    val isNut = (chord.baseFret == 1)
    if (isNut) {
        drawLine(
            color = nutColor,
            start = Offset(leftPadding - 2f, topPadding),
            end = Offset(size.width - rightPadding + 2f, topPadding),
            strokeWidth = 6f
        )
    } else {
        drawLine(
            color = nutColor.copy(alpha = 0.5f),
            start = Offset(leftPadding, topPadding),
            end = Offset(size.width - rightPadding, topPadding),
            strokeWidth = 2f
        )
    }

    // 2. Draw Fret lines (horizontal)
    for (f in 1..numFrets) {
        val y = topPadding + (f * fretSpacing)
        drawLine(
            color = stringColor.copy(alpha = 0.4f),
            start = Offset(leftPadding, y),
            end = Offset(size.width - rightPadding, y),
            strokeWidth = 2f
        )
    }

    // 3. Draw Strings (vertical) - varying gauges from 6th (thick) to 1st (thin)
    for (s in 0 until numStrings) {
        val x = leftPadding + (s * stringSpacing)
        val strokeWidth = 1.2f + (5 - s) * 0.7f // String 6 is thicker than String 1

        drawLine(
            color = stringColor,
            start = Offset(x, topPadding),
            end = Offset(x, topPadding + gridHeight),
            strokeWidth = strokeWidth
        )
    }

    // 4. Draw Open / Muted string markers and Finger Dots
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 24f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val stringLabelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 20f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    val stringNames = listOf("E", "A", "D", "G", "B", "e")

    for (s in 0 until numStrings) {
        val x = leftPadding + (s * stringSpacing)
        val fret = chord.frets.getOrElse(s) { 0 }
        val finger = chord.fingers.getOrElse(s) { 0 }

        // Top markers ('O' / 'X')
        when (fret) {
            -1 -> {
                // Muted string 'X'
                drawContext.canvas.nativeCanvas.drawText("✕", x, topPadding - 10f, paint.apply { color = android.graphics.Color.RED })
            }
            0 -> {
                // Open string 'O'
                drawCircle(
                    color = primaryColor,
                    radius = 5.5f,
                    center = Offset(x, topPadding - 12f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                )
            }
            else -> {
                // Fret finger dot
                val relFret = fret - chord.baseFret + 1
                if (relFret in 1..numFrets) {
                    val dotY = topPadding + ((relFret - 0.5f) * fretSpacing)
                    val dotRadius = 14f

                    drawCircle(
                        color = primaryColor,
                        radius = dotRadius,
                        center = Offset(x, dotY)
                    )

                    if (finger > 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "$finger",
                            x,
                            dotY + 8f,
                            paint.apply { color = android.graphics.Color.BLACK }
                        )
                    }
                }
            }
        }

        // String name at the bottom
        drawContext.canvas.nativeCanvas.drawText(
            stringNames[s],
            x,
            topPadding + gridHeight + 20f,
            stringLabelPaint
        )
    }
}
