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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.Chord
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.IvoryBone
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.PhosphorBronze

@Composable
fun ChordDiagram(
    chord: Chord,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onPrimaryArgb = MaterialTheme.colorScheme.onPrimary.toArgb()
    val errorArgb = MaterialTheme.colorScheme.error.toArgb()
    val labelArgb = onSurfaceVariantColor.toArgb()

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = chord.name,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = primaryColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = chord.fullName,
            style = MaterialTheme.typography.bodyMedium,
            color = onSurfaceVariantColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 240.dp, height = 250.dp)
                .background(surfaceColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 200.dp, height = 210.dp)) {
                drawGuitarFretboard(
                    chord = chord,
                    primaryColor = primaryColor,
                    stringColor = PhosphorBronze,
                    nutColor = IvoryBone,
                    fretColor = PhosphorBronze.copy(alpha = 0.5f),
                    onPrimaryArgb = onPrimaryArgb,
                    errorArgb = errorArgb,
                    labelArgb = labelArgb
                )
            }
        }
    }
}

private fun DrawScope.drawGuitarFretboard(
    chord: Chord,
    primaryColor: Color,
    stringColor: Color,
    nutColor: Color,
    fretColor: Color,
    onPrimaryArgb: Int,
    errorArgb: Int,
    labelArgb: Int
) {
    val numStrings = 6
    val numFrets = 4

    val topPadding = 28.dp.toPx()
    val bottomPadding = 18.dp.toPx()
    val leftPadding = 20.dp.toPx()
    val rightPadding = 20.dp.toPx()

    val gridWidth = size.width - leftPadding - rightPadding
    val gridHeight = size.height - topPadding - bottomPadding

    val stringSpacing = gridWidth / (numStrings - 1)
    val fretSpacing = gridHeight / numFrets

    // 1. Draw Nut (or base fret line)
    val isNut = (chord.baseFret == 1)
    if (isNut) {
        drawLine(
            color = nutColor,
            start = Offset(leftPadding - 2.dp.toPx(), topPadding),
            end = Offset(size.width - rightPadding + 2.dp.toPx(), topPadding),
            strokeWidth = 3.5.dp.toPx()
        )
    } else {
        drawLine(
            color = nutColor.copy(alpha = 0.5f),
            start = Offset(leftPadding, topPadding),
            end = Offset(size.width - rightPadding, topPadding),
            strokeWidth = 1.5.dp.toPx()
        )
    }

    // 2. Draw Fret lines (horizontal)
    for (f in 1..numFrets) {
        val y = topPadding + (f * fretSpacing)
        drawLine(
            color = fretColor,
            start = Offset(leftPadding, y),
            end = Offset(size.width - rightPadding, y),
            strokeWidth = 1.5.dp.toPx()
        )
    }

    // 3. Draw Strings (vertical) - varying gauges from 6th (thick) to 1st (thin)
    for (s in 0 until numStrings) {
        val x = leftPadding + (s * stringSpacing)
        val strokeWidth = (0.8f + (5 - s) * 0.4f).dp.toPx()

        drawLine(
            color = stringColor,
            start = Offset(x, topPadding),
            end = Offset(x, topPadding + gridHeight),
            strokeWidth = strokeWidth
        )
    }

    // 4. Draw Open / Muted string markers and Finger Dots
    val markerTextSize = 13.sp.toPx()
    val labelTextSize = 11.sp.toPx()
    val dotRadius = 9.dp.toPx()
    val openCircleRadius = 4.dp.toPx()

    val paint = android.graphics.Paint().apply {
        textSize = markerTextSize
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val stringLabelPaint = android.graphics.Paint().apply {
        color = labelArgb
        textSize = labelTextSize
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
                paint.color = errorArgb
                drawContext.canvas.nativeCanvas.drawText("✕", x, topPadding - 8.dp.toPx(), paint)
            }
            0 -> {
                // Open string 'O'
                drawCircle(
                    color = primaryColor,
                    radius = openCircleRadius,
                    center = Offset(x, topPadding - 9.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8.dp.toPx())
                )
            }
            else -> {
                // Fret finger dot
                val relFret = fret - chord.baseFret + 1
                if (relFret in 1..numFrets) {
                    val dotY = topPadding + ((relFret - 0.5f) * fretSpacing)

                    drawCircle(
                        color = primaryColor,
                        radius = dotRadius,
                        center = Offset(x, dotY)
                    )

                    if (finger > 0) {
                        paint.color = onPrimaryArgb
                        drawContext.canvas.nativeCanvas.drawText(
                            "$finger",
                            x,
                            dotY + 4.5.dp.toPx(),
                            paint
                        )
                    }
                }
            }
        }

        // String name at the bottom
        drawContext.canvas.nativeCanvas.drawText(
            stringNames[s],
            x,
            topPadding + gridHeight + 14.dp.toPx(),
            stringLabelPaint
        )
    }
}
