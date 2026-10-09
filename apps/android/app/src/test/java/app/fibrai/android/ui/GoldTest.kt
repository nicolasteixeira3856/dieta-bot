package app.fibrai.android.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertWithMessage
import app.fibrai.android.core.database.DaySnapshot
import app.fibrai.android.core.database.MealSlot
import app.fibrai.android.feature.config.ConfigActions
import app.fibrai.android.feature.config.ConfigMapper
import app.fibrai.android.feature.config.ConfigScreen
import app.fibrai.android.core.designsystem.aero.Aero
import app.fibrai.android.core.designsystem.aero.AeroTheme
import app.fibrai.android.feature.chat.ChatFixtures
import app.fibrai.android.feature.chat.ChatScreen
import app.fibrai.android.feature.chat.ChatUiState
import app.fibrai.android.feature.chat.PhotoPreviews
import app.fibrai.android.feature.home.HomeFixtures
import app.fibrai.android.feature.home.HomePanelMapper
import app.fibrai.android.feature.home.HomePanelScreen
import app.fibrai.android.feature.onboarding.CeilingScreen
import app.fibrai.android.feature.onboarding.EatScreen
import app.fibrai.android.feature.onboarding.MacrosScreen
import app.fibrai.android.feature.onboarding.OnboardingUiState
import app.fibrai.android.feature.onboarding.SlotDraft
import app.fibrai.android.feature.onboarding.OnboardingSlotsScreen
import app.fibrai.android.feature.splash.SplashScreen
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every screen of the inventory (docs/qa/README.md § Golds, GoldInventory) with the state shown in its gold
 * and diffs it against the Figma frame docs/qa/figma/{theme}/<id>.png (ADR-031). JVM renders and diff masks land in
 * build/outputs/gold/. docs/qa/android/current/ is reserved for emulator screencaps (tools/diff-gold.mjs).
 *
 * Gold geometry (390 dp @ 2x): the bare Figma frame, no status or navigation bar, rendered at the frame height
 * (the qualifiers of each test). The bottom 40 dp are left out of the diff.
 *
 * Gate: both images are blurred (3 box passes, ~sigma 3 px) before the per-pixel compare, so
 * Chrome-vs-Skia glyph rasterisation is ignored (AGENTS: ignore font raster) while layout shifts,
 * sizes and colours still count. The raw per-pixel share is printed for reference only.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Plain Application: the real one starts PushSync on a Room flow that outlives each test and leaks
// "Illegal connection pointer" into the next one (flaky UncaughtExceptionsBeforeTest).
@Config(application = Application::class)
class GoldTest {
    @get:Rule
    val compose = createComposeRule()

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun splash_dark() = check("splash", dark = true) { SplashScreen(capture = true, onDone = {}) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun splash_light() = check("splash", dark = false) { SplashScreen(capture = true, onDone = {}) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h979dp-xhdpi")
    fun o1_dark() = check("o1", dark = true) { O1() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h979dp-xhdpi")
    fun o1_light() = check("o1", dark = false) { O1() }

    /** A31: before the profile (A41: Figma frame, 937 dp). */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h937dp-xhdpi")
    fun o1e_dark() = check("o1e", dark = true) { O1E() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h937dp-xhdpi")
    fun o1e_light() = check("o1e", dark = false) { O1E() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o2_dark() = check("o2", dark = true) { O2() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o2_light() = check("o2", dark = false) { O2() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1268dp-xhdpi")
    fun o3_dark() = check("o3", dark = true) { O3() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1268dp-xhdpi")
    fun o3_light() = check("o3", dark = false) { O3() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1270dp-xhdpi")
    fun o3s_dark() = check("o3s", dark = true) { O3S() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1270dp-xhdpi")
    fun o3s_light() = check("o3s", dark = false) { O3S() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h902dp-xhdpi")
    fun cfgS_dark() = check("cfgS", dark = true) { CfgS() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h902dp-xhdpi")
    fun cfgS_light() = check("cfgS", dark = false) { CfgS() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h865dp-xhdpi")
    fun o4_dark() = check("o4", dark = true) { O4() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h865dp-xhdpi")
    fun o4_light() = check("o4", dark = false) { O4() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1258dp-xhdpi")
    fun home0_dark() = check("home0", dark = true) { Home(HomeFixtures.home0) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1258dp-xhdpi")
    fun home0_light() = check("home0", dark = false) { Home(HomeFixtures.home0) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1414dp-xhdpi")
    fun home1_dark() = check("home1", dark = true) { Home(HomeFixtures.home1Workout) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1414dp-xhdpi")
    fun home1_light() = check("home1", dark = false) { Home(HomeFixtures.home1Workout) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1516dp-xhdpi")
    fun homeX_dark() = check("homeX", dark = true) { Home(HomeFixtures.homeXWorkout) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1516dp-xhdpi")
    fun homeX_light() = check("homeX", dark = false) { Home(HomeFixtures.homeXWorkout) }

    /** A22 homeW: "Treino de hoje" sheet over the blurred home1 (A40: Figma frame, 1414 dp, gated whole). */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1414dp-xhdpi")
    fun homeW_dark() = check("homeW", dark = true) { HomeW() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1414dp-xhdpi")
    fun homeW_light() = check("homeW", dark = false) { HomeW() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chat0_dark() = check("chat0", dark = true) { Chat(ChatFixtures.chat0) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chat0_light() = check("chat0", dark = false) { Chat(ChatFixtures.chat0) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatL_dark() = check("chatL", dark = true) { Chat(ChatFixtures.chatL) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatL_light() = check("chatL", dark = false) { Chat(ChatFixtures.chatL) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatE_dark() = check("chatE", dark = true) { Chat(ChatFixtures.chatE) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatE_light() = check("chatE", dark = false) { Chat(ChatFixtures.chatE) }

    /** A30: second question before the estimate, Forçar estimativa (A42: Figma frame, gated whole). */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatQ_dark() = check("chatQ", dark = true) { Chat(ChatFixtures.chatQ) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatQ_light() = check("chatQ", dark = false) { Chat(ChatFixtures.chatQ) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatT_dark() = check("chatT", dark = true) { Chat(ChatFixtures.chatT) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatT_light() = check("chatT", dark = false) { Chat(ChatFixtures.chatT) }

    /** A34: Substituir inside the conversation. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatU_dark() = check("chatU", dark = true) { Chat(ChatFixtures.chatU) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatU_light() = check("chatU", dark = false) { Chat(ChatFixtures.chatU) }

    /** A59 (D19): a record and a skip in one message (1050 dp frame), and the skip of a meal with a record. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1050dp-xhdpi")
    fun chatSK_dark() = check("chatSK", dark = true) { Chat(ChatFixtures.chatSK) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1050dp-xhdpi")
    fun chatSK_light() = check("chatSK", dark = false) { Chat(ChatFixtures.chatSK) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatSD_dark() = check("chatSD", dark = true) { Chat(ChatFixtures.chatSD) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatSD_light() = check("chatSD", dark = false) { Chat(ChatFixtures.chatSD) }

    /** A47 (D9): addition to a meal with a record (930 dp frame), its meal picker and a revision. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h930dp-xhdpi")
    fun chatI_dark() = check("chatI", dark = true) { Chat(ChatFixtures.chatI) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h930dp-xhdpi")
    fun chatI_light() = check("chatI", dark = false) { Chat(ChatFixtures.chatI) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatTI_dark() = check("chatTI", dark = true) { Chat(ChatFixtures.chatTI) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatTI_light() = check("chatTI", dark = false) { Chat(ChatFixtures.chatTI) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatIC_dark() = check("chatIC", dark = true) { Chat(ChatFixtures.chatIC) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatIC_light() = check("chatIC", dark = false) { Chat(ChatFixtures.chatIC) }

    /** A34: receipts (chatF photo, chatG, chatD undone + restored). A43: Figma frames, gated whole. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h961dp-xhdpi")
    fun chatF_dark() = check("chatF", dark = true, reportOnly = true, regions = CHAT_F_BOXES) { Chat(ChatFixtures.chatF) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h961dp-xhdpi")
    fun chatF_light() = check("chatF", dark = false, reportOnly = true, regions = CHAT_F_BOXES) { Chat(ChatFixtures.chatF) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h942dp-xhdpi")
    fun chatD_dark() = check("chatD", dark = true) { Chat(ChatFixtures.chatD) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h942dp-xhdpi")
    fun chatD_light() = check("chatD", dark = false) { Chat(ChatFixtures.chatD) }

    /** A19: photo attached in the composer. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatA_dark() = check("chatA", dark = true) { Chat(ChatFixtures.chatA) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatA_light() = check("chatA", dark = false) { Chat(ChatFixtures.chatA) }

    /** A25: text over 2000 characters. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatX_dark() = check("chatX", dark = true) { Chat(ChatFixtures.chatX) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatX_light() = check("chatX", dark = false) { Chat(ChatFixtures.chatX) }

    /** A42 chatP: the Home skip confirmation (Dialog/Confirm) over the blurred empty day. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatP_dark() = check("chatP", dark = true) { HomePanelScreen(HomePanelMapper.map(HomeFixtures.home0, LocalDate.parse("2026-09-25")), {}, {}, {}, initialSkip = 3) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatP_light() = check("chatP", dark = false) { HomePanelScreen(HomePanelMapper.map(HomeFixtures.home0, LocalDate.parse("2026-09-25")), {}, {}, {}, initialSkip = 3) }

    /** A29: plan with the projected day and Registrar assim. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h930dp-xhdpi")
    fun chatR_dark() = check("chatR", dark = true) { Chat(ChatFixtures.chatR) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h930dp-xhdpi")
    fun chatR_light() = check("chatR", dark = false) { Chat(ChatFixtures.chatR) }

    /** A29: Memória atualizada and the origin chips. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h927dp-xhdpi")
    fun chatM_dark() = check("chatM", dark = true) { Chat(ChatFixtures.chatM) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h927dp-xhdpi")
    fun chatM_light() = check("chatM", dark = false) { Chat(ChatFixtures.chatM) }

    /** A29: the routine suggestion on an empty day. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatS_dark() = check("chatS", dark = true) { Chat(ChatFixtures.chatS) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatS_light() = check("chatS", dark = false) { Chat(ChatFixtures.chatS) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h902dp-xhdpi")
    fun chatG_dark() = check("chatG", dark = true) { Chat(ChatFixtures.chatG) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h902dp-xhdpi")
    fun chatG_light() = check("chatG", dark = false) { Chat(ChatFixtures.chatG) }

    /** chatCP, chatCC (D20, A61 part B): two messages selected; the copy confirmation of Android 12 and earlier. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h902dp-xhdpi")
    fun chatCP_dark() = check("chatCP", dark = true) { Chat(ChatFixtures.chatCP) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h902dp-xhdpi")
    fun chatCP_light() = check("chatCP", dark = false) { Chat(ChatFixtures.chatCP) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h948dp-xhdpi")
    fun chatCC_dark() = check("chatCC", dark = true) { Chat(ChatFixtures.chatCC) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h948dp-xhdpi")
    fun chatCC_light() = check("chatCC", dark = false) { Chat(ChatFixtures.chatCC) }

    /** cfg gold is a full-page capture (1047 dp) ending on the Dados block (D15). */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1104dp-xhdpi")
    fun cfg_dark() = check("cfg", dark = true) { Cfg() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1104dp-xhdpi")
    fun cfg_light() = check("cfg", dark = false) { Cfg() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun cfgR_dark() = check("cfgR", dark = true) { Cfg(reset = true) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun cfgR_light() = check("cfgR", dark = false) { Cfg(reset = true) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun wipe_dark() = check("wipe", dark = true) { Cfg(wipe = true) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun wipe_light() = check("wipe", dark = false) { Cfg(wipe = true) }

    /** chatRL (D18, A60 part D): the reserved plan. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h898dp-xhdpi")
    fun chatRL_dark() = check("chatRL", dark = true) { Chat(ChatFixtures.chatRL) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h898dp-xhdpi")
    fun chatRL_light() = check("chatRL", dark = false) { Chat(ChatFixtures.chatRL) }

    /** chatRK (D17, A60 part C): the recipe with its table and steps. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1067dp-xhdpi")
    fun chatRK_dark() = check("chatRK", dark = true, reportOnly = true, regions = CHAT_RK_BOXES) { Chat(ChatFixtures.chatRK) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1067dp-xhdpi")
    fun chatRK_light() = check("chatRK", dark = false, reportOnly = true, regions = CHAT_RK_BOXES) { Chat(ChatFixtures.chatRK) }

    /** homeP (D18, A60 part D): the dinner reserved on the timeline. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1496dp-xhdpi")
    fun homeP_dark() = check("homeP", dark = true) { Home(HOME_P_DAY) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1496dp-xhdpi")
    fun homeP_light() = check("homeP", dark = false) { Home(HOME_P_DAY) }

    /** chatRB (D12, A60 part A). */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1002dp-xhdpi")
    fun chatRB_dark() = check("chatRB", dark = true) { Chat(ChatFixtures.chatRB) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1002dp-xhdpi")
    fun chatRB_light() = check("chatRB", dark = false) { Chat(ChatFixtures.chatRB) }

    /** o5 (D16, A60 part B): the tone, Seco preselected. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o5_dark() = check("o5", dark = true) { O5() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o5_light() = check("o5", dark = false) { O5() }

    /** cfgT (D16): the Tom da Tali sheet over the blurred Config. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun cfgT_dark() = check("cfgT", dark = true) { CfgT() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun cfgT_light() = check("cfgT", dark = false) { CfgT() }

    /** homeC (D16): the day closure between the workout row and the timeline. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1632dp-xhdpi")
    fun homeC_dark() = check("homeC", dark = true) { HomeClosure(LocalDate.parse("2026-09-25"), listOf(DAY_CLOSURE)) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1632dp-xhdpi")
    fun homeC_light() = check("homeC", dark = false) { HomeClosure(LocalDate.parse("2026-09-25"), listOf(DAY_CLOSURE)) }

    /** homeK (D16): the week closure above the day closure, Sunday 4 de outubro. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1834dp-xhdpi")
    fun homeK_dark() = check("homeK", dark = true, reportOnly = true, regions = HOME_K_BOXES) { HomeClosure(LocalDate.parse("2026-10-04"), WEEK_CLOSURES) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1834dp-xhdpi")
    fun homeK_light() = check("homeK", dark = false, reportOnly = true, regions = HOME_K_BOXES) { HomeClosure(LocalDate.parse("2026-10-04"), WEEK_CLOSURES) }

    @Composable private fun O5() = app.fibrai.android.feature.onboarding.ToneScreen(GOLD_STATE, {}, {}, {})

    @Composable private fun CfgT() = ConfigScreen(
        ConfigMapper.map(CFG_DAY, LocalDate.parse("2026-09-25")).copy(
            editor = app.fibrai.android.feature.config.ConfigEditor.TONE,
            draft = ConfigMapper.draftOf(CFG_DAY),
        ),
        ConfigActions(),
    )

    @Composable private fun HomeClosure(today: LocalDate, closures: List<app.fibrai.android.core.database.ClosureEntity>) =
        HomePanelScreen(HomePanelMapper.map(HOME_C_DAY, today, closures = closures), {}, {}, {})

    @Composable private fun Cfg(wipe: Boolean = false, reset: Boolean = false) =
        ConfigScreen(ConfigMapper.map(CFG_DAY, LocalDate.parse("2026-09-25")).copy(wipeConfirm = wipe, resetConfirm = reset), ConfigActions())

    @Composable private fun Chat(ui: ChatUiState) =
        ChatScreen(ui, onBack = {}, onComposer = {}, onSend = {}, onRetry = {}, onSheetSelect = {}, onSheetConfirm = {}, onSheetClose = {})

    @Composable private fun Home(day: DaySnapshot) =
        HomePanelScreen(HomePanelMapper.map(day, LocalDate.parse("2026-09-25")), {}, {}, {})

    @Composable private fun HomeW() = HomePanelScreen(
        HomePanelMapper.map(HomeFixtures.home1Workout, LocalDate.parse("2026-09-25"), workoutDraft = "350"),
        {}, {}, {},
    )

    @Composable private fun O1() = CeilingScreen(GOLD_STATE, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {})

    @Composable private fun O1E() = CeilingScreen(OnboardingUiState(sex = "male"), {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {})

    @Composable private fun O2() = EatScreen(GOLD_STATE, {}, {}, {}, {})

    @Composable private fun O3() = OnboardingSlotsScreen(GOLD_STATE, {}, { _, _ -> }, { _, _ -> }, {}, {})

    @Composable private fun O3S() = OnboardingSlotsScreen(
        GOLD_STATE.copy(
            slots = listOf(SlotDraft(name = "Café da manhã", minutes = 570), SlotDraft(name = "Almoço", minutes = 810), SlotDraft(name = "Jantar", minutes = 1230)),
            slotSchedule = app.fibrai.android.feature.onboarding.SlotScheduleDraft(mode = "split", index = 1),
        ), {}, { _, _ -> }, { _, _ -> }, {}, {},
    )

    @Composable private fun CfgS() = ConfigScreen(ConfigMapper.map(
        CFG_DAY.copy(slotMode = "split", slots = CFG_DAY.slots.map { it.copy(days = 31) } +
            listOf(MealSlot(5, "Café da manhã", 570, 96), MealSlot(6, "Almoço", 810, 96), MealSlot(7, "Jantar", 1230, 96))),
        LocalDate.parse("2026-09-25"),
    ), ConfigActions())

    @Composable private fun O4() = MacrosScreen(GOLD_STATE, {}, {}, {}, {}, {})

    /** [navDp] = 0 for bottom sheets: they draw under the nav bar and pad themselves. */
    @org.junit.Before
    fun preloadPhotos() {
        PhotoPreviews.load(ChatFixtures.CHAT_F_PHOTO) ?: error("missing ${ChatFixtures.CHAT_F_PHOTO}")
        PhotoPreviews.load(ChatFixtures.CHAT_A_PHOTO) ?: error("missing ${ChatFixtures.CHAT_A_PHOTO}")
    }

    private fun check(
        id: String,
        dark: Boolean,
        /** Gold px box gated on its own (best vertical offset) when the whole screen is a conflict. */
        region: IntArray? = null,
        /** More boxes, each with its own best offset (A34: receipt and its buttons, whose gap the golds disagree on). */
        regions: List<IntArray> = emptyList(),
        /** Boxes printed, never asserted: the part of a gold that contradicts the others. */
        reportRegions: List<IntArray> = emptyList(),
        /** Gold to diff against when the state has none of its own. */
        goldId: String = id,
        reportOnly: Boolean = false,
        /** false: [region] is printed, not asserted (the gold contradicts the canonical one). */
        gateRegion: Boolean = true,
        screen: @Composable () -> Unit,
    ) {
        // Figma golds (ADR-031) are the bare frame: no status bar, no nav bar, the page at the frame height.
        assertWithMessage("$goldId in the inventory").that(GoldInventory.contains(goldId)).isTrue()
        compose.setContent {
            AeroTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(Aero.colors.bgPage)) { screen() }
            }
        }
        compose.waitForIdle()
        val app = compose.onRoot().captureToImage().asAndroidBitmap()
        val theme = if (dark) "dark" else "light"
        save(app, File(RENDER_OUT, "$theme/$id.png"))

        val goldFile = File(ROOT, "docs/qa/figma/$theme/$goldId.png")
        val gold = BitmapFactory.decodeFile(goldFile.path) ?: error("missing gold $goldFile")
        val raw = diff(app, gold)
        val appBlur = blur(app)
        val goldBlur = blur(gold)
        val blurred = diff(appBlur, goldBlur)
        val inkRatio = ink(appBlur).toDouble() / ink(goldBlur).coerceAtLeast(1)
        save(blurred.mask, File(DIFF_OUT, "$theme-$id.png"))
        println("GOLD_DIFF $theme/$id blurred ${"%.2f".format(blurred.percent)}% raw ${"%.2f".format(raw.percent)}% ink ${"%.2f".format(inkRatio)}")
        (listOfNotNull(region) + regions).forEachIndexed { i, box ->
            val part = regionDiff(appBlur, goldBlur, box)
            val name = if (i == 0) "region" else "region$i"
            println("GOLD_DIFF $theme/$id $name ${"%.2f".format(part)}%")
            if (gateRegion) assertWithMessage("$theme/$id $name differs from the gold (blurred)").that(part).isAtMost(MAX_DIFF_PERCENT)
        }
        reportRegions.forEachIndexed { i, box ->
            println("GOLD_DIFF $theme/$id report$i ${"%.2f".format(regionDiff(appBlur, goldBlur, box))}%")
        }
        if (reportOnly) return
        assertWithMessage("$theme/$id content ink vs gold").that(inkRatio).isIn(com.google.common.collect.Range.closed(0.8, 1.25))
        assertWithMessage("$theme/$id differs from the gold (blurred)").that(blurred.percent).isAtMost(MAX_DIFF_PERCENT)
    }

    private class Diff(val percent: Double, val mask: Bitmap)

    /** Share of compared pixels whose max channel delta exceeds [CHANNEL_TOLERANCE]. */
    private fun diff(app: Bitmap, gold: Bitmap): Diff {
        val w = minOf(app.width, gold.width)
        val h = minOf(app.height, gold.height)
        val from = 0
        val to = h - IGNORE_BOTTOM_DP * 2
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        var differ = 0
        var total = 0
        val a = IntArray(w)
        val g = IntArray(w)
        for (y in 0 until h) {
            app.getPixels(a, 0, w, 0, y, w, 1)
            gold.getPixels(g, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                if (y < from || y >= to) {
                    mask.setPixel(x, y, Color.DKGRAY)
                    continue
                }
                total++
                val d = max(
                    abs(Color.red(a[x]) - Color.red(g[x])),
                    max(abs(Color.green(a[x]) - Color.green(g[x])), abs(Color.blue(a[x]) - Color.blue(g[x]))),
                )
                if (d > CHANNEL_TOLERANCE) {
                    differ++
                    mask.setPixel(x, y, Color.RED)
                } else {
                    mask.setPixel(x, y, Color.argb(255, Color.red(g[x]) / 3, Color.green(g[x]) / 3, Color.blue(g[x]) / 3))
                }
            }
        }
        return Diff(100.0 * differ / total.coerceAtLeast(1), mask)
    }

    /** Blurred diff of the gold box [x0, y0, x1, y1] against the app, best vertical offset in ±64 dp. */
    private fun regionDiff(app: Bitmap, gold: Bitmap, box: IntArray): Double {
        val (x0, y0, x1, y1) = box.toList()
        var best = 100.0
        for (dy in -128..128 step 2) {
            var differ = 0
            var total = 0
            for (y in y0 until y1 step 2) {
                val ay = y - REGION_BIAS_PX + dy
                if (ay !in 0 until app.height) continue
                for (x in x0 until x1 step 2) {
                    total++
                    val a = app.getPixel(x, ay)
                    val g = gold.getPixel(x, y)
                    val d = max(abs(Color.red(a) - Color.red(g)), max(abs(Color.green(a) - Color.green(g)), abs(Color.blue(a) - Color.blue(g))))
                    if (d > CHANNEL_TOLERANCE) differ++
                }
            }
            if (total > 0) best = minOf(best, 100.0 * differ / total)
        }
        return best
    }

    /** Pixels off the page background (median colour) in the compared rows: catches blank captures. */
    private fun ink(img: Bitmap): Int {
        val from = 0
        val to = img.height - IGNORE_BOTTOM_DP * 2
        val sample = IntArray(0).toMutableList()
        for (y in from until to step 8) for (x in 0 until img.width step 8) sample += img.getPixel(x, y)
        val bg = intArrayOf(
            sample.map { Color.red(it) }.sorted()[sample.size / 2],
            sample.map { Color.green(it) }.sorted()[sample.size / 2],
            sample.map { Color.blue(it) }.sorted()[sample.size / 2],
        )
        var n = 0
        val row = IntArray(img.width)
        for (y in from until to) {
            img.getPixels(row, 0, img.width, 0, y, img.width, 1)
            for (c in row) {
                val d = max(abs(Color.red(c) - bg[0]), max(abs(Color.green(c) - bg[1]), abs(Color.blue(c) - bg[2])))
                if (d > CHANNEL_TOLERANCE) n++
            }
        }
        return n
    }

    /** Three horizontal+vertical box passes (radius 3): close to a Gaussian with sigma ~3 px. */
    private fun blur(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        var px = IntArray(w * h).also { src.getPixels(it, 0, w, 0, 0, w, h) }
        repeat(3) {
            px = boxPass(px, w, h, horizontal = true)
            px = boxPass(px, w, h, horizontal = false)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun boxPass(input: IntArray, w: Int, h: Int, horizontal: Boolean): IntArray {
        val r = BLUR_RADIUS
        val out = IntArray(input.size)
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        for (line in 0 until lines) {
            fun at(i: Int): Int {
                val c = i.coerceIn(0, len - 1)
                return if (horizontal) input[line * w + c] else input[c * w + line]
            }
            var sr = 0
            var sg = 0
            var sb = 0
            for (i in -r..r) {
                val c = at(i)
                sr += Color.red(c); sg += Color.green(c); sb += Color.blue(c)
            }
            val n = 2 * r + 1
            for (i in 0 until len) {
                val idx = if (horizontal) line * w + i else i * w + line
                out[idx] = Color.rgb(sr / n, sg / n, sb / n)
                val add = at(i + r + 1)
                val sub = at(i - r)
                sr += Color.red(add) - Color.red(sub)
                sg += Color.green(add) - Color.green(sub)
                sb += Color.blue(add) - Color.blue(sub)
            }
        }
        return out
    }

    private fun save(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    companion object {
        /** The bottom 40 dp of a frame are left out (the frame's last edge bubble and bottom margin). */
        private const val IGNORE_BOTTOM_DP = 40

        /** Region search starts 20 dp above the gold box (best offset in ±64 dp). */
        private const val REGION_BIAS_PX = 40
        private const val CHANNEL_TOLERANCE = 40
        private const val BLUR_RADIUS = 3
        private const val MAX_DIFF_PERCENT = 2.0

        /** homeK: the page down to the week card, then the day card and the timeline, each at its best offset (2 dp apart). */
        private val HOME_K_BOXES = listOf(intArrayOf(0, 0, 780, 1690), intArrayOf(0, 1690, 780, 3588))

        /**
         * chatF (Figma frame): the frame crops its sample photo differently from the app's centre crop of the fixture, so
         * the photo box is left out: header to the bubble top, the tag and caption row, and the bot answer to the end.
         */
        /**
         * chatRK: the app fits "finalize" on the third step's first line where the Figma text wraps it (font raster), so
         * everything below moves one line: the bubble down to the steps, and the total, the day panel and Registrar
         * assim, each at its best offset.
         */
        // Until A68: the D23 export added Salvar receita under Registrar assim, a button A68 delivers, so the second box
        // stops above the action rows; A68 restores it to the end of the frame.
        private val CHAT_RK_BOXES = listOf(intArrayOf(0, 0, 780, 1370), intArrayOf(0, 1450, 780, 1780))

        private val CHAT_F_BOXES = listOf(intArrayOf(0, 0, 780, 228), intArrayOf(0, 470, 780, 1922))

        private val ROOT = File("../../..")
        private val RENDER_OUT = File("build/outputs/gold/render")
        private val DIFF_OUT = File("build/outputs/gold/diff")

        /** State drawn in the cfg gold: 2000 kcal same, 0% eat-back, 150/200/67, 4 slots, no workout. */
        val CFG_DAY = HomeFixtures.day().copy(
            slots = listOf(
                MealSlot(1, "Café da manhã", 7 * 60 + 30),
                MealSlot(2, "Almoço", 12 * 60 + 30),
                MealSlot(3, "Lanche da tarde", 16 * 60),
                MealSlot(4, "Jantar", 20 * 60),
            ),
        )

        /** homeC / homeK: home1 with a 2000 kcal ceiling, 50% eat-back and 350 kcal of workout (Meta 2175). */
        val HOME_C_DAY = HomeFixtures.home1.copy(kcalSame = 2000, eat = "partial", pct = 50, workoutKcal = 350)

        /** homeP: the homeC day with the dinner reserved (D18). */
        val HOME_P_DAY = HOME_C_DAY.copy(
            planned = mapOf(4L to app.fibrai.android.domain.PlannedSlot("Omelete de forno: 3 ovos, 50 g de ricota e 1 fatia de pão integral", 360, 30, 20, 18)),
        )

        val DAY_CLOSURE = app.fibrai.android.core.database.ClosureEntity(
            key = "day:2026-09-25", period = "day", date = "2026-09-25", numbers = "{}",
            text = "1300 de 2175 kcal. Proteína: 76 de 150 g.", status = "text", createdAtEpochMs = 0,
        )

        private val WEEK_NUMBERS = app.fibrai.android.domain.ClosureWeek(
            days = listOf(2210, 1980, 2300, 0, 2240, 2390, 2300).mapIndexed { i, kcal ->
                app.fibrai.android.domain.ClosureWeekDay(
                    LocalDate.parse("2026-09-28").plusDays(i.toLong()).toString(), kcal, if (kcal > 0) 118 else 0, 200, 60, 2175, null, kcal > 0,
                )
            },
            overSlot = app.fibrai.android.domain.ClosureOverSlot("Jantar", 4),
        )

        val WEEK_CLOSURES = listOf(
            app.fibrai.android.core.database.ClosureEntity(
                key = "week:2026-09-28", period = "week", date = "2026-09-28",
                numbers = app.fibrai.android.domain.Closures.json.encodeToString(app.fibrai.android.domain.ClosureWeek.serializer(), WEEK_NUMBERS),
                text = "Semana: média de 2.237 kcal e 118 g de proteína por dia.", status = "text", createdAtEpochMs = 0,
            ),
            DAY_CLOSURE.copy(key = "day:2026-10-04", date = "2026-10-04"),
        )

        val GOLD_STATE = OnboardingUiState(
            sex = "male",
            ageField = "27",
            heightField = "180",
            weightField = "116",
            ceilingMode = "same",
            sameField = "2000",
            ceilingEdited = true,
            suggestedCeiling = 2160,
            eat = "zero",
            slots = listOf(
                SlotDraft(name = "Café da manhã", minutes = 7 * 60 + 30),
                SlotDraft(name = "Almoço", minutes = 12 * 60 + 30),
                SlotDraft(name = "Lanche", minutes = 16 * 60),
                SlotDraft(name = "Jantar", minutes = 20 * 60),
            ),
            proteinField = "150",
            carbField = "200",
            fatField = "67",
            day1Ceiling = 2000,
        )
    }
}
