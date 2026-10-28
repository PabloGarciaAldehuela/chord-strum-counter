package dev.pablocoding.contadorderasgueosdeacordes.presentation.clock

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.ChromaticNote
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.ChromaticWheelData
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.theory.NoteNotationMode
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberGold
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberLight
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.EbonyDark
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.IvoryBone
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.OiledMahogany
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.PhosphorBronze
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.RosewoodLight
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Tonewood palette for the Chromatic Wheel:
// Natural notes = Golden Amber Varnish (Spruce/Maple soundboard tone)
// Altered notes = Rich Oiled Rosewood & Mahogany (Dark acoustic fingerboard tone)
private val AccidentalWood = OiledMahogany
private val AccidentalWoodSelected = RosewoodLight
private val NaturalVarnish = AmberGold
private val NaturalVarnishSelected = AmberLight
private val WheelDividerColor = EbonyDark
private val GlueBracketColor = IvoryBone

@Composable
fun ChromaticClockWheel(
    selectedPitchClass: Int,
    notationMode: NoteNotationMode,
    onNoteSelected: (Int) -> Unit,
    onCenterTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedNote = ChromaticWheelData.NOTES[selectedPitchClass.coerceIn(0, 11)]
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val naturalTextColor = Color(0xFF221100).toArgb() // High-contrast dark tone on golden amber
    val accidentalTextColor = IvoryBone.toArgb()        // Polished bone white on dark rosewood
    val selectedBorderColor = IvoryBone.toArgb()

    val paintMain = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val paintSub = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    val paintBracket = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = GlueBracketColor.toArgb()
        }
    }

    val paintIndicator = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = PhosphorBronze.toArgb()
        }
    }

    val paintHubPrimary = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    val paintHubSecondary = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    val paintHubPlay = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = AmberGold.toArgb()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp)
            .semantics {
                contentDescription = "The Note Clock. Selected note: ${selectedNote.nameLetters} (${selectedNote.nameSolfege}). Tap notes on wheel to change, or tap center to play acoustic tone."
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        val maxR = size.width * 0.44f
                        val minR = size.width * 0.22f

                        if (r < minR) {
                            onCenterTapped()
                        } else if (r <= maxR) {
                            val angleRad = atan2(dy, dx)
                            val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                            var deltaFromTop = angleDeg - (-90f)
                            if (deltaFromTop < 0f) deltaFromTop += 360f
                            val sliceIndex = (deltaFromTop / 30f).toInt().coerceIn(0, 11)
                            onNoteSelected(sliceIndex)
                        }
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerRadius = size.width * 0.42f
            val innerRadius = size.width * 0.23f
            val hubRadius = innerRadius - 4.dp.toPx()

            // 1. Draw outer direction arrows and English labels
            drawDirectionIndicators(cx, cy, outerRadius, paintIndicator)

            // 2. Draw 12 Slices
            for (i in 0 until 12) {
                val note = ChromaticWheelData.NOTES[i]
                val isSelected = (i == selectedPitchClass)

                val startAngle = -90f + i * 30f
                val sweepAngle = 30f
                val midAngleRad = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())

                val sliceOuterR = if (isSelected) outerRadius + 5.dp.toPx() else outerRadius
                val sliceInnerR = innerRadius

                val sliceColor = when {
                    note.isNatural && isSelected -> NaturalVarnishSelected
                    note.isNatural -> NaturalVarnish
                    isSelected -> AccidentalWoodSelected
                    else -> AccidentalWood
                }

                // Draw arc wedge
                drawWedge(
                    cx = cx,
                    cy = cy,
                    innerR = sliceInnerR,
                    outerR = sliceOuterR,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    fillColor = sliceColor,
                    strokeColor = WheelDividerColor,
                    strokeWidth = 2.dp.toPx()
                )

                // If selected, draw luminous ivory bone highlight outline
                if (isSelected) {
                    drawWedge(
                        cx = cx,
                        cy = cy,
                        innerR = sliceInnerR,
                        outerR = sliceOuterR,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        fillColor = Color.Transparent,
                        strokeColor = Color(selectedBorderColor),
                        strokeWidth = 3.5f.dp.toPx()
                    )
                }

                // Draw Note Text
                val textRadius = (sliceInnerR + sliceOuterR) / 2f
                val tx = cx + (textRadius * cos(midAngleRad)).toFloat()
                val ty = cy + (textRadius * sin(midAngleRad)).toFloat()

                val textColor = if (note.isNatural) naturalTextColor else accidentalTextColor
                paintMain.color = textColor
                paintSub.color = textColor

                drawSliceText(
                    drawContext = drawContext.canvas.nativeCanvas,
                    note = note,
                    mode = notationMode,
                    x = tx,
                    y = ty,
                    paintMain = paintMain,
                    paintSub = paintSub,
                    outerRadius = outerRadius
                )
            }

            // 3. Draw English "Adjacent!" Callout Brackets for B-C and E-F
            drawGlueCallout(
                cx = cx,
                cy = cy,
                radius = outerRadius + 10.dp.toPx(),
                angleDeg = -90f, // Top boundary between B (11) and C (0)
                label = "B & C adjacent (1 fret)",
                paint = paintBracket,
                isTop = true
            )

            drawGlueCallout(
                cx = cx,
                cy = cy,
                radius = outerRadius + 10.dp.toPx(),
                angleDeg = 90f, // Bottom boundary between E (4) and F (5)
                label = "E & F adjacent (1 fret)",
                paint = paintBracket,
                isTop = false
            )

            // 4. Center Hub (Circle)
            drawCircle(
                color = WheelDividerColor,
                radius = hubRadius + 2.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = surfaceColor,
                radius = hubRadius,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = PhosphorBronze.copy(alpha = 0.5f),
                radius = hubRadius,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )

            // Center Hub Content (Selected Note & Tap to play)
            val selectedNote = ChromaticWheelData.NOTES[selectedPitchClass.coerceIn(0, 11)]
            drawCenterHubText(
                drawContext = drawContext.canvas.nativeCanvas,
                note = selectedNote,
                mode = notationMode,
                cx = cx,
                cy = cy,
                hubRadius = hubRadius,
                textColor = onSurfaceColor.toArgb(),
                paintPrimary = paintHubPrimary,
                paintSecondary = paintHubSecondary,
                paintPlay = paintHubPlay
            )
        }
    }
}

/**
 * Draws an annular sector (pie slice with inner and outer radius).
 */
private fun DrawScope.drawWedge(
    cx: Float,
    cy: Float,
    innerR: Float,
    outerR: Float,
    startAngle: Float,
    sweepAngle: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float
) {
    val path = Path().apply {
        val startRad = Math.toRadians(startAngle.toDouble())
        val endRad = Math.toRadians((startAngle + sweepAngle).toDouble())

        val x0 = cx + (innerR * cos(startRad)).toFloat()
        val y0 = cy + (innerR * sin(startRad)).toFloat()

        moveTo(x0, y0)
        lineTo(
            cx + (outerR * cos(startRad)).toFloat(),
            cy + (outerR * sin(startRad)).toFloat()
        )

        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                cx - outerR, cy - outerR, cx + outerR, cy + outerR
            ),
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngle,
            forceMoveTo = false
        )

        lineTo(
            cx + (innerR * cos(endRad)).toFloat(),
            cy + (innerR * sin(endRad)).toFloat()
        )

        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                cx - innerR, cy - innerR, cx + innerR, cy + innerR
            ),
            startAngleDegrees = startAngle + sweepAngle,
            sweepAngleDegrees = -sweepAngle,
            forceMoveTo = false
        )
        close()
    }

    if (fillColor != Color.Transparent) {
        drawPath(path, color = fillColor)
    }
    if (strokeWidth > 0f) {
        drawPath(
            path,
            color = strokeColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws note text inside a slice, adapting to LETTERS, SOLFEGE, or DUAL mode.
 */
private fun drawSliceText(
    drawContext: android.graphics.Canvas,
    note: ChromaticNote,
    mode: NoteNotationMode,
    x: Float,
    y: Float,
    paintMain: Paint,
    paintSub: Paint,
    outerRadius: Float
) {
    val scale = outerRadius / 150f
    val mainSize = if (note.isNatural) 15f * scale else 11.5f * scale
    val subSize = if (note.isNatural) 11.5f * scale else 9.5f * scale

    paintMain.textSize = mainSize
    paintSub.textSize = subSize

    when (mode) {
        NoteNotationMode.LETTERS -> {
            val yOffset = (paintMain.descent() + paintMain.ascent()) / 2f
            drawContext.drawText(note.nameLetters, x, y - yOffset, paintMain)
        }
        NoteNotationMode.SOLFEGE -> {
            val yOffset = (paintMain.descent() + paintMain.ascent()) / 2f
            drawContext.drawText(note.nameSolfege, x, y - yOffset, paintMain)
        }
        NoteNotationMode.DUAL -> {
            if (note.isNatural) {
                drawContext.drawText(note.nameLetters, x, y - 2f * scale, paintMain)
                drawContext.drawText("(${note.nameSolfege})", x, y + 12f * scale, paintSub)
            } else {
                drawContext.drawText(note.nameLetters, x, y - 2.5f * scale, paintMain)
                drawContext.drawText("(${note.nameSolfege})", x, y + 10f * scale, paintSub)
            }
        }
    }
}

/**
 * Visual bracket callout for the natural half-step adjacent points: B-C (top) and E-F (bottom).
 */
private fun DrawScope.drawGlueCallout(
    cx: Float,
    cy: Float,
    radius: Float,
    angleDeg: Float,
    label: String,
    paint: Paint,
    isTop: Boolean
) {
    val angleRad = Math.toRadians(angleDeg.toDouble())
    val bx = cx + (radius * cos(angleRad)).toFloat()
    val by = cy + (radius * sin(angleRad)).toFloat()

    val scale = radius / 170f
    paint.textSize = 10f * scale

    val yPos = if (isTop) by - 4.dp.toPx() else by + 12.dp.toPx()
    drawContext.canvas.nativeCanvas.drawText(label, bx, yPos, paint)

    val bracketSpan = 18f
    val startAngle = angleDeg - bracketSpan / 2f
    drawArc(
        color = GlueBracketColor.copy(alpha = 0.85f),
        startAngle = startAngle,
        sweepAngle = bracketSpan,
        useCenter = false,
        topLeft = Offset(cx - (radius - 2.dp.toPx()), cy - (radius - 2.dp.toPx())),
        size = Size((radius - 2.dp.toPx()) * 2f, (radius - 2.dp.toPx()) * 2f),
        style = Stroke(width = 1.8f.dp.toPx(), cap = StrokeCap.Round)
    )
}

/**
 * Directional indicators for Sharps (+1 fret / semitone) and Flats (-1 fret / semitone) in English.
 */
private fun DrawScope.drawDirectionIndicators(
    cx: Float,
    cy: Float,
    radius: Float,
    paintText: Paint
) {
    val indicatorRadius = radius + 22.dp.toPx()
    val stroke = Stroke(width = 1.5f.dp.toPx(), cap = StrokeCap.Round)
    val color = PhosphorBronze.copy(alpha = 0.7f)

    // Right side: Clockwise arrow (+1 Fret / Sharps #)
    drawArc(
        color = color,
        startAngle = -45f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - indicatorRadius, cy - indicatorRadius),
        size = Size(indicatorRadius * 2f, indicatorRadius * 2f),
        style = stroke
    )

    // Left side: Counter-Clockwise arrow (-1 Fret / Flats b)
    drawArc(
        color = color,
        startAngle = 135f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - indicatorRadius, cy - indicatorRadius),
        size = Size(indicatorRadius * 2f, indicatorRadius * 2f),
        style = stroke
    )

    val scale = radius / 170f
    paintText.textSize = 9.5f * scale

    // Right label: +1 Fret (♯)
    drawContext.canvas.nativeCanvas.drawText(
        "+1 Fret (♯)",
        cx + indicatorRadius + 2.dp.toPx(),
        cy + 4.dp.toPx(),
        paintText
    )

    // Left label: -1 Fret (♭)
    drawContext.canvas.nativeCanvas.drawText(
        "-1 Fret (♭)",
        cx - indicatorRadius - 2.dp.toPx(),
        cy + 4.dp.toPx(),
        paintText
    )
}

/**
 * Center hub displaying active note details and English "Play Tone" prompt.
 */
private fun drawCenterHubText(
    drawContext: android.graphics.Canvas,
    note: ChromaticNote,
    mode: NoteNotationMode,
    cx: Float,
    cy: Float,
    hubRadius: Float,
    textColor: Int,
    paintPrimary: Paint,
    paintSecondary: Paint,
    paintPlay: Paint
) {
    val scale = hubRadius / 70f
    paintPrimary.textSize = 21f * scale
    paintPrimary.color = textColor

    paintSecondary.textSize = 11.5f * scale
    paintSecondary.color = textColor
    paintSecondary.alpha = 190

    paintPlay.textSize = 10f * scale

    when (mode) {
        NoteNotationMode.LETTERS -> {
            drawContext.drawText(note.nameLetters, cx, cy - 4f * scale, paintPrimary)
            drawContext.drawText("🔊 Play Tone", cx, cy + 16f * scale, paintPlay)
        }
        NoteNotationMode.SOLFEGE -> {
            drawContext.drawText(note.nameSolfege, cx, cy - 4f * scale, paintPrimary)
            drawContext.drawText("🔊 Play Tone", cx, cy + 16f * scale, paintPlay)
        }
        NoteNotationMode.DUAL -> {
            drawContext.drawText(note.nameLetters, cx, cy - 10f * scale, paintPrimary)
            drawContext.drawText(note.nameSolfege, cx, cy + 5f * scale, paintSecondary)
            drawContext.drawText("🔊 Play Tone", cx, cy + 20f * scale, paintPlay)
        }
    }
}
