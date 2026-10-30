package dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.pablocoding.contadorderasgueosdeacordes.domain.audio.PitchMath
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.NoteNaming
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.Tuning
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberGold
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.AmberLight
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.EbonyDark
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.IvoryBone
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.OiledMahogany
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.PhosphorBronze
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.RosewoodLight
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.StringSilver
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.VintageEmerald
import kotlin.math.PI
import kotlin.math.sin

private val BODY_WIDTH = 132.dp
private val MAX_PEG_SIZE = 46.dp
private val KEY_LENGTH = 20.dp
private val PEG_GAP = 4.dp

/** Vertical positions of the three tuner posts, as fractions of the nut height. */
private val POST_ROWS = listOf(0.20f, 0.46f, 0.72f)
private const val NUT_FRACTION = 0.80f

private val BrassDark = Color(0xFF8A5A00)
private val FretboardWood = Color(0xFF2B1A14)
private val STRING_WIDTHS_DP = listOf(2.6f, 2.2f, 1.9f, 1.5f, 1.2f, 1.0f)

/**
 * A 3+3 acoustic headstock drawn in Canvas (mahogany face, ivory nut, brass tuner posts), with a
 * tappable note peg beside each tuner key. Strings 6–4 sit on the left and 3–1 on the right, as
 * seen when looking at the guitar; left-handed mode mirrors everything.
 */
@Composable
fun TunerHeadstock(
    tuning: Tuning,
    naming: NoteNaming,
    activeStringIndex: Int?,
    isActiveInTune: Boolean,
    isSounding: Boolean,
    tunedStrings: Set<Int>,
    signalLevel: Double,
    leftHanded: Boolean,
    onStringTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 230.dp
) {
    val stringCount = 6
    val enabled = !tuning.isChromatic
    val activeColor by animateColorAsState(
        targetValue = if (isActiveInTune) VintageEmerald else MaterialTheme.colorScheme.primary,
        label = "active_string_color"
    )
    val vibration = rememberInfiniteTransition(label = "string_vibration")
    val phase by vibration.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(110, easing = LinearEasing), RepeatMode.Restart),
        label = "string_phase"
    )
    // Gentle, level-dependent shimmer; capped so loud plucks don't look cartoonish.
    val amplitudeDp = if (isSounding) (signalLevel * 40.0).coerceIn(0.6, 2.6).toFloat() else 0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        val centerX = maxWidth / 2
        val nutY = height * NUT_FRACTION
        // Pegs shrink on short headstocks so neighbouring rows never touch.
        val pegSize = minOf(MAX_PEG_SIZE, nutY * (POST_ROWS[1] - POST_ROWS[0]) - 4.dp)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val mirror = if (leftHanded) -1f else 1f
            scale(scaleX = mirror, scaleY = 1f, pivot = Offset(size.width / 2, size.height / 2)) {
                drawHeadstock(
                    nutY = nutY.toPx(),
                    activeIndex = activeStringIndex,
                    activeColor = activeColor,
                    phase = phase,
                    amplitude = amplitudeDp.dp.toPx()
                )
            }
        }

        for (index in 0 until stringCount) {
            val (onLeft, row) = pegPosition(index, leftHanded)
            val pegCenterX = if (onLeft) {
                centerX - BODY_WIDTH / 2 - KEY_LENGTH - PEG_GAP - pegSize / 2
            } else {
                centerX + BODY_WIDTH / 2 + KEY_LENGTH + PEG_GAP + pegSize / 2
            }
            val pegCenterY = nutY * POST_ROWS[row]
            val midi = tuning.strings.getOrNull(index)
            StringPeg(
                label = midi?.let { PitchMath.noteName(it, tuning.preferFlats, naming) } ?: "",
                description = midi?.let {
                    val note = TunerFormatting.noteLabel(it, tuning.preferFlats, naming)
                    val tuned = if (index in tunedStrings) ", tuned" else ""
                    "String ${tuning.stringNumber(index)}, ${note.name} ${note.octave}$tuned. " +
                        "Double tap to hear it and lock the tuner to this string."
                } ?: "",
                isActive = enabled && index == activeStringIndex,
                isInTune = enabled && index == activeStringIndex && isActiveInTune,
                isTuned = enabled && index in tunedStrings,
                enabled = enabled && midi != null,
                onClick = { onStringTap(index) },
                diameter = pegSize,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = pegCenterX - pegSize / 2, y = pegCenterY - pegSize / 2)
            )
        }
    }
}

/** Left column holds strings 6, 5, 4 (bottom → top); right column holds 3, 2, 1 (top → bottom). */
private fun pegPosition(index: Int, leftHanded: Boolean): Pair<Boolean, Int> {
    val lowSide = index <= 2
    val row = if (lowSide) 2 - index else index - 3
    return (lowSide != leftHanded) to row
}

private fun DrawScope.drawHeadstock(
    nutY: Float,
    activeIndex: Int?,
    activeColor: Color,
    phase: Float,
    amplitude: Float
) {
    val bodyWidth = BODY_WIDTH.toPx()
    val left = size.width / 2 - bodyWidth / 2
    val top = 6.dp.toPx()
    val bodyHeight = nutY - top
    fun p(u: Float, v: Float) = Offset(left + u * bodyWidth, top + v * bodyHeight)

    // Tuner keys (drawn first so the wood overlaps their roots).
    val keyLength = KEY_LENGTH.toPx() + 6.dp.toPx()
    val keyHeight = 9.dp.toPx()
    POST_ROWS.forEach { row ->
        val y = nutY * row
        listOf(left - KEY_LENGTH.toPx(), left + bodyWidth - 6.dp.toPx()).forEach { x ->
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(AmberLight, AmberGold, BrassDark), startY = y - keyHeight / 2, endY = y + keyHeight / 2),
                topLeft = Offset(x, y - keyHeight / 2),
                size = Size(keyLength, keyHeight),
                cornerRadius = CornerRadius(keyHeight / 2)
            )
        }
    }

    // Fretboard below the nut, fading into the background.
    drawRect(
        brush = Brush.verticalGradient(
            listOf(FretboardWood, FretboardWood.copy(alpha = 0f)),
            startY = nutY,
            endY = size.height
        ),
        topLeft = p(0.30f, 1f),
        size = Size(bodyWidth * 0.40f, size.height - nutY)
    )

    val body = Path().apply {
        moveTo(p(0.30f, 1f).x, p(0.30f, 1f).y)
        cubicTo(p(0.30f, 0.92f).x, p(0.30f, 0.92f).y, p(0.08f, 0.88f).x, p(0.08f, 0.88f).y, p(0.06f, 0.78f).x, p(0.06f, 0.78f).y)
        lineTo(p(0.04f, 0.16f).x, p(0.04f, 0.16f).y)
        cubicTo(p(0.03f, 0.05f).x, p(0.03f, 0.05f).y, p(0.14f, 0f).x, p(0.14f, 0f).y, p(0.28f, 0f).x, p(0.28f, 0f).y)
        cubicTo(p(0.40f, 0f).x, p(0.40f, 0f).y, p(0.44f, 0.07f).x, p(0.44f, 0.07f).y, p(0.5f, 0.07f).x, p(0.5f, 0.07f).y)
        cubicTo(p(0.56f, 0.07f).x, p(0.56f, 0.07f).y, p(0.60f, 0f).x, p(0.60f, 0f).y, p(0.72f, 0f).x, p(0.72f, 0f).y)
        cubicTo(p(0.86f, 0f).x, p(0.86f, 0f).y, p(0.97f, 0.05f).x, p(0.97f, 0.05f).y, p(0.96f, 0.16f).x, p(0.96f, 0.16f).y)
        lineTo(p(0.94f, 0.78f).x, p(0.94f, 0.78f).y)
        cubicTo(p(0.92f, 0.88f).x, p(0.92f, 0.88f).y, p(0.70f, 0.92f).x, p(0.70f, 0.92f).y, p(0.70f, 1f).x, p(0.70f, 1f).y)
        close()
    }
    drawPath(body, Brush.horizontalGradient(listOf(OiledMahogany, RosewoodLight, OiledMahogany), startX = left, endX = left + bodyWidth))
    // Subtle grain.
    for (i in 1..5) {
        val u = 0.12f + i * 0.13f
        drawLine(
            color = EbonyDark.copy(alpha = 0.10f),
            start = p(u, 0.10f),
            end = p(u + 0.02f, 0.85f),
            strokeWidth = 1.dp.toPx()
        )
    }
    // Binding.
    drawPath(body, AmberGold.copy(alpha = 0.35f), style = Stroke(width = 1.5.dp.toPx()))

    // Truss-rod cover: a small bell, subtle against the wood.
    val trussTopLeft = p(0.44f, 0.84f)
    val trussSize = Size(bodyWidth * 0.12f, bodyHeight * 0.10f)
    drawRoundRect(EbonyDark.copy(alpha = 0.45f), trussTopLeft, trussSize, CornerRadius(bodyWidth * 0.06f))
    drawRoundRect(AmberGold.copy(alpha = 0.25f), trussTopLeft, trussSize, CornerRadius(bodyWidth * 0.06f), style = Stroke(width = 1.dp.toPx()))

    // Nut.
    val nutHalf = bodyWidth * 0.21f
    drawRect(
        color = IvoryBone,
        topLeft = Offset(size.width / 2 - nutHalf, nutY - 2.dp.toPx()),
        size = Size(nutHalf * 2, 5.dp.toPx())
    )

    // Strings: post → nut → down the fretboard.
    val stringSpread = nutHalf * 0.82f
    val step = stringSpread * 2 / 5
    val postX = bodyWidth * 0.27f
    for (index in 0 until 6) {
        val lowSide = index <= 2
        val row = if (lowSide) 2 - index else index - 3
        val post = Offset(size.width / 2 + (if (lowSide) -postX else postX), nutY * POST_ROWS[row])
        val nut = Offset(size.width / 2 - stringSpread + index * step, nutY)
        val isActive = index == activeIndex
        val color = when {
            isActive -> activeColor
            index <= 2 -> PhosphorBronze
            else -> StringSilver
        }
        val width = STRING_WIDTHS_DP[index].dp.toPx() * if (isActive) 1.3f else 1f
        drawLine(color.copy(alpha = 0.9f), post, nut, width, cap = StrokeCap.Round)

        if (isActive && amplitude > 0f) {
            val path = Path()
            val segments = 24
            val length = size.height - nutY
            for (s in 0..segments) {
                val t = s / segments.toFloat()
                // Standing wave anchored at the nut.
                val dx = amplitude * sin(PI.toFloat() * t) * sin(phase)
                val point = Offset(nut.x + dx, nutY + t * length)
                if (s == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
        } else {
            drawLine(
                brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.1f)), startY = nutY, endY = size.height),
                start = nut,
                end = Offset(nut.x, size.height),
                strokeWidth = width
            )
        }

        // Brass post on top of the string wrap.
        val postRadius = 7.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                listOf(AmberLight, AmberGold, BrassDark),
                center = post - Offset(postRadius * 0.3f, postRadius * 0.3f),
                radius = postRadius * 1.4f
            ),
            radius = postRadius,
            center = post
        )
        drawCircle(EbonyDark, radius = postRadius * 0.35f, center = post)
    }
}

@Composable
private fun StringPeg(
    label: String,
    description: String,
    isActive: Boolean,
    isInTune: Boolean,
    isTuned: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    diameter: Dp,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = when {
            isInTune -> VintageEmerald
            isActive -> scheme.primaryContainer
            isTuned -> VintageEmerald.copy(alpha = 0.22f)
            else -> scheme.surfaceVariant
        },
        label = "peg_container"
    )
    val borderColor = when {
        isInTune || isTuned -> VintageEmerald
        isActive -> scheme.primary
        else -> scheme.outlineVariant
    }
    val textColor = when {
        isInTune -> IvoryBone
        isActive -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    val glow = if (isActive) borderColor else Color.Transparent

    Box(modifier = modifier.size(diameter)) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = container,
            border = BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (glow != Color.Transparent) {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent)),
                            radius = size.minDimension * 0.85f
                        )
                    }
                }
                .semantics {
                    contentDescription = description
                    role = Role.Button
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    // Sized to the circle (not the user's font scale) so long solfège names fit;
                    // the full name is exposed to TalkBack through the content description.
                    fontSize = with(LocalDensity.current) {
                        (diameter * when {
                            label.length <= 2 -> 0.40f
                            label.length == 3 -> 0.32f
                            else -> 0.25f
                        }).toSp()
                    },
                    maxLines = 1
                )
            }
        }
        if (isTuned && !isInTune) {
            Surface(
                shape = CircleShape,
                color = VintageEmerald,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = IvoryBone,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
