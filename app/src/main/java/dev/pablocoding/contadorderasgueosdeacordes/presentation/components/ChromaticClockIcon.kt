package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Chromatic Clock icon: a circular wheel dial with radial note ticks and clock hands.
 */
val ChromaticClockIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "ChromaticClock",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Outer wheel circle
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2.5f)
            curveTo(17.25f, 2.5f, 21.5f, 6.75f, 21.5f, 12f)
            curveTo(21.5f, 17.25f, 17.25f, 21.5f, 12f, 21.5f)
            curveTo(6.75f, 21.5f, 2.5f, 17.25f, 2.5f, 12f)
            curveTo(2.5f, 6.75f, 6.75f, 2.5f, 12f, 2.5f)
            close()
        }
        // Center hub dot
        path(
            fill = SolidColor(Color.Black)
        ) {
            moveTo(12f, 10.5f)
            curveTo(12.83f, 10.5f, 13.5f, 11.17f, 13.5f, 12f)
            curveTo(13.5f, 12.83f, 12.83f, 13.5f, 12f, 13.5f)
            curveTo(11.17f, 13.5f, 10.5f, 12.83f, 10.5f, 12f)
            curveTo(10.5f, 11.17f, 11.17f, 10.5f, 12f, 10.5f)
            close()
        }
        // Hands: hour hand pointing to 1:00 (Slice 0 / C) and minute hand pointing to 12:00
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 12f)
            lineTo(12f, 6.5f)
            moveTo(12f, 12f)
            lineTo(15.5f, 9.5f)
        }
        // 4 cardinal mini-ticks
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 3.5f)
            lineTo(12f, 5f)
            moveTo(20.5f, 12f)
            lineTo(19f, 12f)
            moveTo(12f, 20.5f)
            lineTo(12f, 19f)
            moveTo(3.5f, 12f)
            lineTo(5f, 12f)
        }
    }.build()
}
