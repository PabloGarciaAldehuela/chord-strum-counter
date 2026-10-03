package dev.pablocoding.contadorderasgueosdeacordes.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.pablocoding.contadorderasgueosdeacordes.presentation.counter.CounterScreen
import dev.pablocoding.contadorderasgueosdeacordes.presentation.history.HistoryScreen
import dev.pablocoding.contadorderasgueosdeacordes.presentation.tuner.TunerScreen

import dev.pablocoding.contadorderasgueosdeacordes.presentation.splash.SplashScreen

private const val ROUTE_SPLASH = "splash"
private const val ROUTE_COUNTER = "counter"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_TUNER = "tuner"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_SPLASH
    ) {
        composable(ROUTE_SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(ROUTE_COUNTER) {
                        popUpTo(ROUTE_SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_COUNTER) {
            CounterScreen(
                onNavigateToHistory = { navController.navigate(ROUTE_HISTORY) },
                onNavigateToTuner = { navController.navigate(ROUTE_TUNER) { launchSingleTop = true } }
            )
        }
        composable(ROUTE_HISTORY) {
            HistoryScreen(
                onNavigateBack = dropUnlessResumed { navController.popBackStack() }
            )
        }
        composable(ROUTE_TUNER) {
            TunerScreen(
                onNavigateBack = dropUnlessResumed { navController.popBackStack() }
            )
        }
    }
}
