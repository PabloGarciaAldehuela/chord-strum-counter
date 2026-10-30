package dev.pablocoding.contadorderasgueosdeacordes.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Tuning fork with two small vibration arcs; tinted by `Icon` like any Material icon. */
val TuningForkIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TuningFork",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8.5f, 2.5f)
            lineTo(8.5f, 10f)
            curveTo(8.5f, 12.6f, 10f, 14.2f, 12f, 14.2f)
            curveTo(14f, 14.2f, 15.5f, 12.6f, 15.5f, 10f)
            lineTo(15.5f, 2.5f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 14.2f)
            lineTo(12f, 21.5f)
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(5f, 4f)
            curveTo(4f, 5.3f, 4f, 6.7f, 5f, 8f)
            moveTo(19f, 4f)
            curveTo(20f, 5.3f, 20f, 6.7f, 19f, 8f)
        }
    }.build()
}
