package dev.pablocoding.contadorderasgueosdeacordes.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = FretboardBlack,
    primaryContainer = RosewoodMid,
    onPrimaryContainer = AmberLight,
    secondary = RosewoodLight,
    onSecondary = StringSilver,
    background = FretboardBlack,
    onBackground = StringSilver,
    surface = SurfaceDark,
    onSurface = StringSilver,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = StringSilver,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = RosewoodLightTheme,
    onPrimary = SurfaceLight,
    primaryContainer = SurfaceVariantLight,
    onPrimaryContainer = RosewoodLightTheme,
    secondary = AmberLightTheme,
    onSecondary = OnSurfaceLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceLight
)

@Composable
fun ChordCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}