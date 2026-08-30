package dev.pablocoding.contadorderasgueosdeacordes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.pablocoding.contadorderasgueosdeacordes.presentation.navigation.AppNavigation
import dev.pablocoding.contadorderasgueosdeacordes.ui.theme.ChordCounterTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChordCounterTheme {
                AppNavigation()
            }
        }
    }
}