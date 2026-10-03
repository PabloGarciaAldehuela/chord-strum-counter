package dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pablocoding.contadorderasgueosdeacordes.domain.model.tuner.TunerReading
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.IvoryBone
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.VintageEmerald

private const val TRACE_SPAN_MS = 2500L
private const val TRACE_GAP_MS = 150L
private const val TRACE_CAPACITY = 256
private val BUBBLE_SIZE = 56.dp
private val EDGE_MARGIN = 30.dp
private val GRID_ROW_SPACING = 32.dp

/** Ring buffer of recent (time, position, colour band) points, mutated outside composition. */
private class TraceHistory {
    val times = LongArray(TRACE_CAPACITY)
    val positions = FloatArray(TRACE_CAPACITY)
    val levels = arrayOfNulls<DeviationLevel>(TRACE_CAPACITY)
    var head = 0
    var count = 0

    fun add(timeMs: Long, position: Float, level: DeviationLevel) {
        times[head] = timeMs
        positions[head] = position
        levels[head] = level
        head = (head + 1) % TRACE_CAPACITY
        if (count < TRACE_CAPACITY) count++
    }

    /** Index of the i-th newest point (0 = newest). */
    fun indexOf(i: Int) = (head - 1 - i + TRACE_CAPACITY) % TRACE_CAPACITY
}

/**
 * Pitch display: a scrolling history line over a fret-like grid, flowing down
 * toward the headstock, with a cents bubble and a "Tune up / Tune down" pill at its tip.
 * ♭ is on the left (flat), ♯ on the right (sharp).
 */
@Composable
fun TunerPitchTrace(
    reading: TunerReading,
    toleranceCents: Double,
    inTuneEventCount: Int,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val history = remember { TraceHistory() }
    var frameTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) withFrameMillis { frameTimeMs = it }
    }

    val cents = reading.cents
    val hasLivePitch = reading.hasPitch && !reading.isHolding && cents != null
    LaunchedEffect(reading) {
        if (hasLivePitch) {
            history.add(
                // Same monotonic base as the Choreographer frame time used for drawing.
                timeMs = System.nanoTime() / 1_000_000,
                position = TunerFormatting.displayPosition(cents!!),
                level = TunerFormatting.deviationLevel(cents, toleranceCents)
            )
        }
    }

    val level = cents?.let { TunerFormatting.deviationLevel(it, toleranceCents) }
    val levelColor by animateColorAsState(
        targetValue = colorFor(level, scheme.primary, scheme.error, scheme.outline),
        label = "deviation_color"
    )
    val bubblePosition by animateFloatAsState(
        targetValue = if (reading.hasPitch && cents != null) TunerFormatting.displayPosition(cents) else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "bubble_position"
    )

    // Pop the bubble on every new in-tune event (but not when re-entering the screen).
    var handledInTuneEvents by rememberSaveable { mutableIntStateOf(inTuneEventCount) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(inTuneEventCount) {
        if (inTuneEventCount > handledInTuneEvents) {
            handledInTuneEvents = inTuneEventCount
            pop.snapTo(1.25f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    val gridColor = scheme.outlineVariant
    val centerLineColor = scheme.outline
    val toleranceBand = VintageEmerald.copy(alpha = 0.12f)
    val farColor = scheme.error
    val closeColor = scheme.primary

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val now = frameTimeMs
            val margin = EDGE_MARGIN.toPx()
            val centerX = size.width / 2
            val halfWidth = size.width / 2 - margin
            val traceTop = BUBBLE_SIZE.toPx() / 2 + 4.dp.toPx()
            val traceHeight = size.height - traceTop
            fun xFor(position: Float) = centerX + position * halfWidth

            // In-tune band.
            val bandLeft = xFor(TunerFormatting.displayPosition(-toleranceCents))
            val bandRight = xFor(TunerFormatting.displayPosition(toleranceCents))
            drawRect(toleranceBand, topLeft = Offset(bandLeft, traceTop), size = androidx.compose.ui.geometry.Size(bandRight - bandLeft, traceHeight))

            // Vertical "fret" lines every 10 cents, denser near the centre thanks to the curve.
            for (c in -50..50 step 10) {
                val x = xFor(TunerFormatting.displayPosition(c.toDouble()))
                val isCenter = c == 0
                drawLine(
                    color = if (isCenter) centerLineColor.copy(alpha = 0.6f) else gridColor.copy(alpha = 0.45f),
                    start = Offset(x, traceTop),
                    end = Offset(x, size.height),
                    strokeWidth = if (isCenter) 1.5.dp.toPx() else 1.dp.toPx()
                )
            }

            // Horizontal rows scroll downward with the history.
            val rowSpacing = GRID_ROW_SPACING.toPx()
            val pxPerMs = traceHeight / TRACE_SPAN_MS
            val scroll = (now * pxPerMs) % rowSpacing
            var y = traceTop + scroll
            while (y < size.height) {
                drawLine(gridColor.copy(alpha = 0.22f), Offset(margin, y), Offset(size.width - margin, y), 1.dp.toPx())
                y += rowSpacing
            }

            // The trace itself: newest point at the top (under the bubble), older points below.
            var previous: Offset? = null
            var previousTime = 0L
            for (i in 0 until history.count) {
                val index = history.indexOf(i)
                val time = history.times[index]
                val age = now - time
                if (age > TRACE_SPAN_MS) break
                val point = Offset(xFor(history.positions[index]), traceTop + age.coerceAtLeast(0) * pxPerMs)
                val prev = previous
                if (prev != null && previousTime - time <= TRACE_GAP_MS) {
                    val alpha = (1f - age.toFloat() / TRACE_SPAN_MS).coerceIn(0f, 1f)
                    drawLine(
                        color = colorFor(history.levels[index], closeColor, farColor, centerLineColor).copy(alpha = alpha),
                        start = prev,
                        end = point,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                previous = point
                previousTime = time
            }
        }

        Text(
            text = "♭",
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp)
        )
        Text(
            text = "♯",
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp)
        )

        BubbleRow(
            position = bubblePosition,
            modifier = Modifier.fillMaxSize()
        ) {
            CentsBubble(
                reading = reading,
                color = levelColor,
                modifier = Modifier.scale(pop.value)
            )
            if (reading.hasPitch && cents != null) {
                val direction = TunerFormatting.direction(cents, reading.isInTune)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (reading.isInTune) VintageEmerald.copy(alpha = 0.2f) else scheme.surfaceVariant,
                    border = BorderStroke(1.dp, levelColor.copy(alpha = 0.6f)),
                    modifier = Modifier.alpha(if (reading.isHolding) 0.6f else 1f)
                ) {
                    Text(
                        text = direction.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun colorFor(level: DeviationLevel?, close: Color, far: Color, idle: Color): Color = when (level) {
    DeviationLevel.IN_TUNE -> VintageEmerald
    DeviationLevel.CLOSE -> close
    DeviationLevel.FAR -> far
    null -> idle
}

/**
 * Places the bubble (first child) horizontally at [position] in [-1, 1] and the pill (optional
 * second child) beside it, flipping to the other side when it would run off the edge.
 */
@Composable
private fun BubbleRow(
    position: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val placeables = measurables.map { it.measure(loose) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val bubble = placeables.firstOrNull() ?: return@layout
            val margin = EDGE_MARGIN.roundToPx()
            val halfWidth = constraints.maxWidth / 2 - margin
            val bubbleX = (constraints.maxWidth / 2 + position * halfWidth - bubble.width / 2f).toInt()
            val bubbleY = 4.dp.roundToPx()
            bubble.place(bubbleX, bubbleY)

            placeables.getOrNull(1)?.let { pill ->
                val gap = 8.dp.roundToPx()
                val rightX = bubbleX + bubble.width + gap
                val x = if (rightX + pill.width <= constraints.maxWidth) rightX else bubbleX - gap - pill.width
                pill.place(x, bubbleY + (bubble.height - pill.height) / 2)
            }
        }
    }
}

@Composable
private fun CentsBubble(
    reading: TunerReading,
    color: Color,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val cents = reading.cents
    Surface(
        shape = CircleShape,
        color = if (reading.isInTune) VintageEmerald else scheme.surface,
        border = BorderStroke(2.5.dp, color),
        shadowElevation = if (reading.isInTune) 8.dp else 2.dp,
        modifier = modifier
            .size(BUBBLE_SIZE)
            .alpha(if (reading.isHolding || !reading.hasPitch) 0.6f else 1f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                reading.isInTune -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = IvoryBone,
                    modifier = Modifier.size(30.dp)
                )
                reading.hasPitch && cents != null -> Text(
                    text = TunerFormatting.centsLabel(cents),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = scheme.onSurface
                )
                else -> Text(
                    text = "–",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}
