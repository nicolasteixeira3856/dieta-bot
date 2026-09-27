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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.NutriTheme
import com.nutri.android.feature.home.HomePanelScreen
import com.nutri.android.feature.home.HomePanelViewModel
import com.nutri.android.feature.onboarding.CeilingScreen
import com.nutri.android.feature.onboarding.EatScreen
import com.nutri.android.feature.onboarding.MacrosScreen
import com.nutri.android.feature.onboarding.OnboardingViewModel
import com.nutri.android.feature.onboarding.SlotsScreen
import com.nutri.android.feature.splash.SplashScreen
import com.nutri.android.feature.splash.SplashViewModel
import com.nutri.android.feature.chat.ChatScreen
import com.nutri.android.feature.chat.ChatViewModel
import com.nutri.android.feature.config.ConfigActions
import com.nutri.android.feature.config.ConfigScreen
import com.nutri.android.feature.config.ConfigViewModel
import com.nutri.android.feature.t2.T2Screen
import com.nutri.android.feature.t2.T2ViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable data object RouteSplash
@Serializable data object RouteOnboarding
@Serializable data object RouteCeiling
@Serializable data object RouteEat
@Serializable data object RouteSlots
@Serializable data object RouteMacros
@Serializable data object RouteHome
@Serializable data object RouteChat
@Serializable data object RouteConfig
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
    val onboardingStart: Any = when (captureScreen) {
        "o2" -> RouteEat
        "o3" -> RouteSlots
        "o4" -> RouteMacros
        else -> RouteCeiling
    }
    val startDestination: Any = when (captureScreen) {
        "o1", "o2", "o3", "o4" -> RouteOnboarding
        "t2", "t2q" -> RouteT2
        "home0", "home1", "homeX" -> RouteHome
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
                        val destination: Any = if (ui.onboardingDone) RouteHome else RouteOnboarding
                        nav.navigate(destination) {
                            popUpTo<RouteSplash> { inclusive = true }
                        }
                    },
                )
            }
            navigation<RouteOnboarding>(startDestination = onboardingStart) {
                composable<RouteCeiling> { entry ->
                    val vm = onboardingViewModel(nav, entry)
                    val ui by vm.uiState.collectAsStateWithLifecycle()
                    CeilingScreen(
                        ui = ui,
                        onSex = vm::setSex,
                        onAge = vm::setAge,
                        onHeight = vm::setHeight,
                        onWeight = vm::setWeight,
                        onMode = vm::setCeilingMode,
                        onSame = vm::setSameField,
                        onWeekday = vm::setWeekdayField,
                        onWeekend = vm::setWeekendField,
                        onDay = vm::setDayField,
                        onContinue = { nav.navigate(RouteEat) },
                    )
                }
                composable<RouteEat> { entry ->
                    val vm = onboardingViewModel(nav, entry)
                    val ui by vm.uiState.collectAsStateWithLifecycle()
                    EatScreen(
                        ui = ui,
                        onEat = vm::setEat,
                        onPct = vm::setPct,
                        onBack = { nav.popBackStack() },
                        onContinue = { nav.navigate(RouteSlots) },
                    )
                }
                composable<RouteSlots> { entry ->
                    val vm = onboardingViewModel(nav, entry)
                    val ui by vm.uiState.collectAsStateWithLifecycle()
                    SlotsScreen(
                        ui = ui,
                        onCount = vm::setSlotCount,
                        onName = vm::setSlotName,
                        onTime = vm::setSlotTime,
                        onBack = { nav.popBackStack() },
                        onContinue = { nav.navigate(RouteMacros) },
                    )
                }
                composable<RouteMacros> { entry ->
                    val vm = onboardingViewModel(nav, entry)
                    val ui by vm.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(Unit) { vm.enterMacros() }
                    MacrosScreen(
                        ui = ui,
                        onProtein = vm::setProtein,
                        onCarb = vm::setCarb,
                        onFat = vm::setFat,
                        onBack = { nav.popBackStack() },
                        onFinish = {
                            vm.completeOnboarding {
                                nav.navigate(RouteHome) {
                                    popUpTo<RouteOnboarding> { inclusive = true }
                                }
                            }
                        },
                    )
                }
            }
            composable<RouteHome> {
                val vm: HomePanelViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()
                HomePanelScreen(
                    ui = ui,
                    onSkip = vm::skip,
                    onConfig = { nav.navigate(RouteConfig) },
                    onChat = { nav.navigate(RouteChat) },
                )
            }
            composable<RouteChat> {
                val vm: ChatViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()
                ChatScreen(
                    ui = ui,
                    onBack = { nav.popBackStack() },
                    onComposer = vm::setComposer,
                    onSend = vm::send,
                    onRetry = vm::retry,
                    onRecord = vm::record,
                    onSwap = vm::openSheet,
                    onSheetSelect = vm::selectInSheet,
                    onSheetConfirm = vm::confirmSheet,
                    onSheetClose = vm::closeSheet,
                    onAskSkip = vm::askSkip,
                    onSkipConfirm = vm::confirmSkip,
                    onSkipCancel = vm::cancelSkip,
                )
            }
            composable<RouteConfig> {
                val vm: ConfigViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()
                val actions = remember(vm) {
                    ConfigActions(
                        onBack = { nav.popBackStack() },
                        onOpen = vm::open,
                        onClose = vm::close,
                        onSave = vm::save,
                        onConfirmWipe = vm::confirmWipe,
                        onCancelWipe = vm::cancelWipe,
                        onCeilingMode = vm::setCeilingMode,
                        onSame = vm::setSame,
                        onWeekday = vm::setWeekday,
                        onWeekend = vm::setWeekend,
                        onDay = vm::setDay,
                        onEat = vm::setEat,
                        onPct = vm::setPct,
                        onProtein = vm::setProtein,
                        onCarb = vm::setCarb,
                        onFat = vm::setFat,
                        onSlotCount = vm::setSlotCount,
                        onSlotName = vm::setSlotName,
                        onSlotTime = vm::setSlotTime,
                        onWorkout = vm::setWorkout,
                    )
                }
                ConfigScreen(ui, actions)
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

/** One OnboardingViewModel for O1..O4, scoped to the onboarding graph. */
@Composable
private fun onboardingViewModel(nav: NavHostController, entry: NavBackStackEntry): OnboardingViewModel {
    val parent = remember(entry) { nav.getBackStackEntry<RouteOnboarding>() }
    return hiltViewModel(parent)
}
