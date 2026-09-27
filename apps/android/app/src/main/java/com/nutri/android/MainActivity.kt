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
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.NutriTheme
import com.nutri.android.feature.home.HomeCurrentSheet
import com.nutri.android.feature.home.HomeNavEvent
import com.nutri.android.feature.home.HomeScreen
import com.nutri.android.feature.home.HomeViewModel
import com.nutri.android.feature.onboarding.CeilingScreen
import com.nutri.android.feature.onboarding.EatScreen
import com.nutri.android.feature.onboarding.OnboardingViewModel
import com.nutri.android.feature.splash.SplashScreen
import com.nutri.android.feature.splash.SplashViewModel
import com.nutri.android.feature.t2.T2Screen
import com.nutri.android.feature.t2.T2ViewModel
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
    val nav = rememberNavController()
    val startDestination: Any = when (captureScreen) {
        "o1" -> RouteCeiling
        "o2" -> RouteEat
        "t2", "t2q" -> RouteT2
        "t0", "t0d2", "t0fds", "t1", "t1load", "t3quero", "t3tenho", "t3ideia" -> RouteHome
        else -> RouteSplash
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(LocalPalette.current.bg)
            .semantics { testTagsAsResourceId = true },
    ) {
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable<RouteSplash> {
                val vm: SplashViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()
                val isCapture = captureScreen == "splash"
                SplashScreen(
                    capture = isCapture,
                    onDone = {
                        val destination: Any = if (ui.onboardingDone) RouteHome else RouteCeiling
                        nav.navigate(destination) {
                            popUpTo<RouteSplash> { inclusive = true }
                        }
                    },
                )
            }
            composable<RouteCeiling> {
                val vm: OnboardingViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()
                CeilingScreen(
                    ui = ui,
                    onMode = vm::setCeilingMode,
                    onSame = vm::setSameField,
                    onWeekday = vm::setWeekdayField,
                    onWeekend = vm::setWeekendField,
                    onDay = vm::setDayField,
                    onContinue = {
                        nav.navigate(RouteEat)
                    },
                )
            }
            composable<RouteEat> {
                val vm: OnboardingViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()
                EatScreen(
                    ui = ui,
                    onEat = vm::setEat,
                    onPct = vm::setPct,
                    onStart = {
                        vm.completeOnboarding {
                            nav.navigate(RouteHome) {
                                popUpTo<RouteCeiling> { inclusive = true }
                            }
                        }
                    },
                )
            }
            composable<RouteHome> {
                val vm: HomeViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    vm.navEvents.collect { event ->
                        when (event) {
                            is HomeNavEvent.NavigateToT2 -> {
                                nav.navigate(RouteT2)
                            }
                        }
                    }
                }

                HomeScreen(
                    ui = ui,
                    onLog = vm::openLog,
                    onFit = vm::openFit,
                    onRemoveChip = {
                        val chip = ui.chips.firstOrNull()?.window ?: ui.window
                        vm.removeChip(chip)
                    },
                )
                HomeCurrentSheet(
                    ui = ui,
                    onClose = vm::closeSheet,
                    onText = vm::setLogText,
                    onPhoto = vm::setPhoto,
                    onSubmit = vm::submitLog,
                    onMode = vm::setFitMode,
                    onFitText = vm::setFitText,
                    onFit = vm::primaryFitAction,
                    onAlreadyAte = vm::alreadyAte,
                    onSelectDish = vm::selectFitDish,
                )
            }
            composable<RouteT2> {
                val vm: T2ViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()
                T2Screen(
                    ui = ui,
                    onYes = { vm.onYes { nav.popBackStack() } },
                    onRevise = { vm.onRevise { nav.popBackStack() } },
                    onDiscard = { vm.onDiscard { nav.popBackStack() } },
                    onConfirm = { vm.confirm { nav.popBackStack() } },
                    onUndo = { nav.popBackStack() },
                )
            }
        }
    }
}
