package com.nutri.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.push.PushHandler
import com.nutri.android.core.designsystem.DietaBotTheme
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
import com.nutri.android.feature.chat.rememberPhotoLaunchers
import com.nutri.android.feature.config.ConfigActions
import com.nutri.android.feature.config.ConfigScreen
import com.nutri.android.feature.config.ConfigViewModel
import com.nutri.android.core.telemetry.NoopTelemetry
import com.nutri.android.core.telemetry.Telemetry
import com.nutri.android.core.telemetry.TelemetryEvents
import com.nutri.android.flavor.FlavorConfigRows
import com.nutri.android.flavor.flavorDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var telemetry: Telemetry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent?.getStringExtra("nutri_tela")
        // A7 "Registrar": the notification restarts the task and asks for the Chat.
        val openChat = savedInstanceState == null && intent?.getStringExtra(EXTRA_OPEN) == OPEN_CHAT
        if (openChat) {
            intent?.getLongExtra(EXTRA_SLOT, -1)?.takeIf { it >= 0 }?.let { PushHandler.clear(this, it) }
            telemetry.event(TelemetryEvents.PUSH_ACTION, mapOf("action" to "open"))
        }
        setContent {
            DietaBotTheme {
                App(screen, openChat, telemetry)
            }
        }
    }

    companion object {
        const val EXTRA_OPEN = "nutri_open"
        const val OPEN_CHAT = "chat"
        const val EXTRA_SLOT = "nutri_slot"
    }
}

@Composable
private fun App(captureScreen: String?, openChat: Boolean = false, telemetry: Telemetry = NoopTelemetry) {
    val nav = rememberNavController()
    DisposableEffect(nav, telemetry) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val screen = screenName(destination.route) ?: return@OnDestinationChangedListener
            telemetry.breadcrumb("screen $screen")
            telemetry.event(TelemetryEvents.SCREEN_VIEW, mapOf("screen" to screen))
        }
        nav.addOnDestinationChangedListener(listener)
        onDispose { nav.removeOnDestinationChangedListener(listener) }
    }
    var chatPending by rememberSaveable { mutableStateOf(openChat) }
    val onboardingStart: Any = when (captureScreen) {
        "o2" -> RouteEat
        "o3" -> RouteSlots
        "o4" -> RouteMacros
        else -> RouteCeiling
    }
    val startDestination: Any = when (captureScreen) {
        "o1", "o2", "o3", "o4" -> RouteOnboarding
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
                        onMode = vm::setSlotMode,
                        onCopy = vm::copyPreviousSlots,
                        onConfirmMode = vm::confirmSlotMode,
                        onCancelMode = vm::cancelSlotMode,
                        onBack = { vm.previousSlotGroup { nav.popBackStack() } },
                        onContinue = { vm.nextSlotGroup { nav.navigate(RouteMacros) } },
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
                NotificationPermissionOnce()
                LaunchedEffect(chatPending) {
                    if (chatPending) {
                        chatPending = false
                        nav.navigate(RouteChat)
                    }
                }
                HomePanelScreen(
                    ui = ui,
                    onSkip = vm::skip,
                    onConfig = { nav.navigate(RouteConfig) },
                    onChat = { nav.navigate(RouteChat) },
                    onWorkoutOpen = vm::openWorkout,
                    onWorkoutChange = vm::setWorkout,
                    onWorkoutSave = vm::saveWorkout,
                    onWorkoutCancel = vm::closeWorkout,
                )
            }
            composable<RouteChat> {
                val vm: ChatViewModel = hiltViewModel()
                val ui by vm.uiState.collectAsStateWithLifecycle()
                val photo = rememberPhotoLaunchers(vm)
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
                    onReplaceConfirm = vm::confirmReplace,
                    onReplaceElsewhere = vm::replaceElsewhere,
                    onReplaceCancel = vm::cancelReplace,
                    onPhoto = vm::openPhotoSheet,
                    onCamera = photo.camera,
                    onGallery = photo.gallery,
                    onPhotoSheetClose = vm::closePhotoSheet,
                    onNoticeShown = vm::dismissNotice,
                    onRemoveAttachment = vm::removeAttachment,
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
                        onSlotMode = vm::setSlotMode,
                        onConfirmSlotMode = vm::confirmSlotMode,
                        onCancelSlotMode = vm::cancelSlotMode,
                        onCopySlots = vm::copyPreviousSlots,
                        onPreviousSlots = vm::previousSlotGroup,
                        onOpenSlotGroup = vm::openSlotGroup,
                        onWorkout = vm::setWorkout,
                    )
                }
                ConfigScreen(ui, actions) { FlavorConfigRows(nav) }
            }
            // ADR-019: dev-only tool routes (A23). prod adds none.
            flavorDestinations(nav)
        }
    }
}

/** ADR-012 screen id of a typed route ("com.nutri.android.RouteCeiling" -> "o1"); null for nav graphs. */
internal fun screenName(route: String?): String? = when (route?.substringAfterLast('.')?.substringBefore('?')) {
    "RouteSplash" -> "splash"
    "RouteCeiling" -> "o1"
    "RouteEat" -> "o2"
    "RouteSlots" -> "o3"
    "RouteMacros" -> "o4"
    "RouteHome" -> "home"
    "RouteChat" -> "chat"
    "RouteConfig" -> "cfg"
    else -> null
}

/** One OnboardingViewModel for O1..O4, scoped to the onboarding graph. */
@Composable
private fun onboardingViewModel(nav: NavHostController, entry: NavBackStackEntry): OnboardingViewModel {
    val parent = remember(entry) { nav.getBackStackEntry<RouteOnboarding>() }
    return hiltViewModel(parent)
}

/**
 * A7: POST_NOTIFICATIONS (Android 13+) asked once, from the Home, after onboarding. The system
 * dialog is the only UI; a denial is kept and never asked again from here.
 */
@Composable
private fun NotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nutri_push", android.content.Context.MODE_PRIVATE) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ASKED, true).apply()
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted && !prefs.getBoolean(KEY_NOTIFICATIONS_ASKED, false)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private const val KEY_NOTIFICATIONS_ASKED = "notifications_asked"
