package com.nutri.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nutri.android.ui.CeilingScreen
import com.nutri.android.ui.CurrentSheet
import com.nutri.android.ui.DayViewModel
import com.nutri.android.ui.EatScreen
import com.nutri.android.ui.HomeScreen
import com.nutri.android.ui.LocalPalette
import com.nutri.android.ui.NutriTheme
import com.nutri.android.ui.SplashScreen
import com.nutri.android.ui.Stage
import com.nutri.android.ui.T2Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable data object RouteSplash
@Serializable data object RouteCeiling
@Serializable data object RouteEat
@Serializable data object RouteHome
@Serializable data object RouteT2

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent?.getStringExtra("nutri_tela")
        setContent {
            NutriTheme {
                App(screen)
            }
        }
    }
}

@Composable
private fun App(captureScreen: String?) {
    val vm: DayViewModel = hiltViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    LaunchedEffect(captureScreen) {
        if (!captureScreen.isNullOrBlank()) vm.openCapture(captureScreen)
    }
    LaunchedEffect(ui.stage) {
        val route = when (ui.stage) {
            Stage.SPLASH -> RouteSplash
            Stage.O1 -> RouteCeiling
            Stage.O2 -> RouteEat
            Stage.HOME -> RouteHome
            Stage.T2 -> RouteT2
        }
        nav.navigate(route) {
            popUpTo(nav.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(LocalPalette.current.bg)
            .semantics { testTagsAsResourceId = true },
    ) {
        NavHost(navController = nav, startDestination = RouteSplash, modifier = Modifier.fillMaxSize()) {
            composable<RouteSplash> { SplashScreen(ui, onDone = vm::leaveSplash) }
            composable<RouteCeiling> {
                CeilingScreen(
                    ui = ui,
                    onMode = vm::ceilingMode,
                    onSame = vm::sameField,
                    onWeekday = vm::weekdayField,
                    onWeekend = vm::weekendField,
                    onDay = vm::dayField,
                    onContinue = vm::continueO1,
                )
            }
            composable<RouteEat> {
                EatScreen(ui, onEat = vm::eat, onPct = vm::pct, onStart = vm::enter)
            }
            composable<RouteHome> {
                HomeScreen(
                    ui = ui,
                    onLog = vm::openLog,
                    onFit = vm::openFit,
                    onRemoveChip = { vm.removeChip(ui.chips.firstOrNull()?.window ?: ui.window) },
                )
                CurrentSheet(
                    ui = ui,
                    onClose = vm::close,
                    onText = vm::text,
                    onPhoto = vm::photo,
                    onSubmit = vm::submitLog,
                    onMode = vm::fitMode,
                    onFitText = vm::fitText,
                    onFit = vm::requestFit,
                    onAlreadyAte = vm::alreadyAte,
                )
            }
            composable<RouteT2> {
                T2Screen(
                    ui = ui,
                    onAnswer = vm::t2Answer,
                    onConfirm = vm::confirm,
                    onUndo = vm::undo,
                )
            }
        }
    }
}
