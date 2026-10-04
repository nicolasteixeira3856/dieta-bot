package com.nutri.android.ui

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
import com.nutri.android.core.database.DaySnapshot
import com.nutri.android.core.database.MealSlot
import com.nutri.android.feature.config.ConfigActions
import com.nutri.android.feature.config.ConfigMapper
import com.nutri.android.feature.config.ConfigScreen
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.DietaBotTheme
import com.nutri.android.core.designsystem.aero.AeroTheme
import com.nutri.android.feature.chat.ChatFixtures
import com.nutri.android.feature.chat.ChatScreen
import com.nutri.android.feature.chat.ChatUiState
import com.nutri.android.feature.chat.PhotoPreviews
import com.nutri.android.feature.home.HomeFixtures
import com.nutri.android.feature.home.HomePanelMapper
import com.nutri.android.feature.home.HomePanelScreen
import com.nutri.android.feature.onboarding.CeilingScreen
import com.nutri.android.feature.onboarding.EatScreen
import com.nutri.android.feature.onboarding.MacrosScreen
import com.nutri.android.feature.onboarding.OnboardingUiState
import com.nutri.android.feature.onboarding.SlotDraft
import com.nutri.android.feature.onboarding.SlotsScreen
import com.nutri.android.feature.splash.SplashScreen
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
 * Renders splash + O1..O4 with the state shown in the gold and diffs them against the gold of the id's source in
 * the inventory of docs/qa/README.md: docs/qa/<stitch|figma>/{theme}/<id>.png (GoldInventory). JVM renders and diff masks land in build/outputs/stitch-gold/.
 * docs/qa/android/current/ is reserved for emulator screencaps (tools/diff-gold.mjs).
 *
 * Gold geometry (390 dp @ 2x): 844 dp phone centred in a 884 dp page (20 dp bands), except O3,
 * a full-page 1103 dp capture. Status bar (40 dp) and gesture nav (24 dp) are simulated as
 * insets. The top 40 dp and bottom 40 dp (clock, battery, home pill) are excluded from the diff
 * (AGENTS: ignore clock, battery, nav).
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
class StitchGoldTest {
    @get:Rule
    val compose = createComposeRule()

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun splash_dark() = check("splash", dark = true) { SplashScreen(capture = true, onDone = {}) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun splash_light() = check("splash", dark = false) { SplashScreen(capture = true, onDone = {}) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o1_dark() = check("o1", dark = true) { O1() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o1_light() = check("o1", dark = false) { O1() }

    /** A31: before the profile, a 925 dp full-page gold (ST8). */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h925dp-xhdpi")
    fun o1e_dark() = check("o1e", dark = true, fullPage = true) { O1E() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h925dp-xhdpi")
    fun o1e_light() = check("o1e", dark = false, fullPage = true) { O1E() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o2_dark() = check("o2", dark = true) { O2() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o2_light() = check("o2", dark = false) { O2() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1239dp-xhdpi")
    fun o3_dark() = check("o3", dark = true, fullPage = true) { O3() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1239dp-xhdpi")
    fun o3_light() = check("o3", dark = false, fullPage = true) { O3() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1224dp-xhdpi")
    fun o3s_dark() = check("o3s", dark = true, fullPage = true) { O3S() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1225dp-xhdpi")
    fun o3s_light() = check("o3s", dark = false, fullPage = true) { O3S() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h918dp-xhdpi")
    fun cfgS_dark() = check("cfgS", dark = true, fullPage = true, footerDp = IGNORE_BOTTOM_DP) { CfgS() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h884dp-xhdpi")
    fun cfgS_light() = check("cfgS", dark = false, fullPage = true, footerDp = IGNORE_BOTTOM_DP) { CfgS() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o4_dark() = check("o4", dark = true) { O4() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o4_light() = check("o4", dark = false) { O4() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1258dp-xhdpi")
    fun home0_dark() = check("home0", dark = true, fullPage = true) { Home(HomeFixtures.home0) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1258dp-xhdpi")
    fun home0_light() = check("home0", dark = false, fullPage = true) { Home(HomeFixtures.home0) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1414dp-xhdpi")
    fun home1_dark() = check("home1", dark = true, fullPage = true) { Home(HomeFixtures.home1Workout) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1414dp-xhdpi")
    fun home1_light() = check("home1", dark = false, fullPage = true) { Home(HomeFixtures.home1Workout) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1516dp-xhdpi")
    fun homeX_dark() = check("homeX", dark = true, fullPage = true) { Home(HomeFixtures.homeXWorkout) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1516dp-xhdpi")
    fun homeX_light() = check("homeX", dark = false, fullPage = true) { Home(HomeFixtures.homeXWorkout) }

    /** A22 homeW: "Treino de hoje" sheet over the blurred home1 (A40: Figma frame, 1414 dp, gated whole). */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h1414dp-xhdpi")
    fun homeW_dark() = check("homeW", dark = true, fullPage = true) { HomeW() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1414dp-xhdpi")
    fun homeW_light() = check("homeW", dark = false, fullPage = true) { HomeW() }

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

    /**
     * A30 (ST7): second question before the estimate, Forçar estimativa. Dark is gated whole. The light
     * gold draws the question bubbles tighter than dark (line ~22 dp vs 24.5 dp, icon gap 10 vs 12 dp,
     * ~3 dp less padding) from the same prompt: one component follows dark, so light gates only the
     * Forçar estimativa bar (FORCE_BAR) and reports the screen.
     */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatQ_dark() = check("chatQ", dark = true, region = FORCE_BAR) { Chat(ChatFixtures.chatQ) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatQ_light() = check("chatQ", dark = false, region = FORCE_BAR, reportOnly = true) { Chat(ChatFixtures.chatQ) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatT_dark() = check("chatT", dark = true, navDp = 0) { Chat(ChatFixtures.chatT) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatT_light() = check("chatT", dark = false, navDp = 0) { Chat(ChatFixtures.chatT) }

    /** A34 (ST9): Substituir inside the conversation. chatE generation: whole screen gated. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatU_dark() = check("chatU", dark = true) { Chat(ChatFixtures.chatU) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatU_light() = check("chatU", dark = false) { Chat(ChatFixtures.chatU) }

    /**
     * A34 (ST9): chatF, chatG and chatD are the receipt generation (other header, scrolled thread): the receipt
     * and its stacked actions are gated (RECEIPT_*). The light golds tint the receipt green where dark and the
     * tokens draw it on the card colour: light is reported only (ADR-027 rules 2 and 3).
     */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatF_dark() = check("chatF", dark = true, reportRegions = listOf(PHOTO_BUBBLE_DARK) + RECEIPT_F_DARK) { Chat(ChatFixtures.chatF) }

    /** chatF light: the photo bubble and the buttons are gated; the receipt (green, chip on the title row) is reported. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatF_light() = check(
        "chatF",
        dark = false,
        region = PHOTO_BUBBLE_LIGHT,
        regions = RECEIPT_F_LIGHT.drop(1),
        reportRegions = RECEIPT_F_LIGHT.take(1),
    ) { Chat(ChatFixtures.chatF) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatD_dark() = check("chatD", dark = true, regions = RECEIPT_D_DARK) { Chat(ChatFixtures.chatD) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatD_light() = check("chatD", dark = false, regions = RECEIPT_D_LIGHT, gateRegion = false) { Chat(ChatFixtures.chatD) }

    /**
     * A19: photo attached in the composer. chatA is a copy of chat0 (other header): only the composer is gated.
     * Light gold draws the composer on the page colour (chat0 generation) while the canonical chatE light uses
     * the card colour, so the light region is reported only.
     */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatA_dark() = check("chatA", dark = true, region = COMPOSER_ATTACHED) { Chat(ChatFixtures.chatA) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatA_light() = check("chatA", dark = false, region = COMPOSER_ATTACHED, gateRegion = false) { Chat(ChatFixtures.chatA) }

    /**
     * A25: text over 2000 characters. chatX is a copy of chat0 (other header): only the composer + error line is gated.
     * Like chatA, the light gold draws the composer on the page colour (chat0 generation): light region reported only.
     */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatX_dark() = check("chatX", dark = true, region = COMPOSER_TOO_LONG) { Chat(ChatFixtures.chatX) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatX_light() = check("chatX", dark = false, region = COMPOSER_TOO_LONG, gateRegion = false) { Chat(ChatFixtures.chatX) }

    /**
     * A29 (ST6): plan with the projected day and Registrar assim. The plan bubble and the bar are gated
     * (PLAN_BUBBLE); the whole screen is reported: its header, user bubble and composer mic carry the
     * chatE-generation delta (~1.3 points) on a text-dense screen.
     */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatR_dark() = check("chatR", dark = true, region = PLAN_BUBBLE, reportOnly = true) { Chat(ChatFixtures.chatR) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatR_light() = check("chatR", dark = false, region = PLAN_BUBBLE, reportOnly = true) { Chat(ChatFixtures.chatR) }

    /**
     * A29 (ST6): Memória atualizada + origin chips. chatE generation. The gold still draws Gravar | Trocar |
     * Pular, which A34 (ADR-028) replaced by Registrar: the thread above the actions slot is gated (CHAT_M_THREAD),
     * the screen is reported (ADR-027 rule 3).
     */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatM_dark() = check("chatM", dark = true, region = CHAT_M_THREAD, reportOnly = true) { Chat(ChatFixtures.chatM) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatM_light() = check("chatM", dark = false, region = CHAT_M_THREAD, reportOnly = true) { Chat(ChatFixtures.chatM) }

    /** A29 (ST6): chatS is a copy of chat0 (other header): only the routine card is gated (ROUTINE_CARD). */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatS_dark() = check("chatS", dark = true, region = ROUTINE_CARD) { Chat(ChatFixtures.chatS) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatS_light() = check("chatS", dark = false, region = ROUTINE_CARD) { Chat(ChatFixtures.chatS) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatG_dark() = check("chatG", dark = true, regions = RECEIPT_G_DARK) { Chat(ChatFixtures.chatG) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun chatG_light() = check("chatG", dark = false, regions = RECEIPT_G_LIGHT, gateRegion = false) { Chat(ChatFixtures.chatG) }

    /** cfg gold is a full-page capture (936 dp dark, 930 dp light) with the info note near the end. */
    @Test @Config(sdk = [34], qualifiers = "w390dp-h936dp-xhdpi")
    fun cfg_dark() = check("cfg", dark = true, fullPage = true, footerDp = IGNORE_BOTTOM_DP) { Cfg() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h930dp-xhdpi")
    fun cfg_light() = check("cfg", dark = false, fullPage = true, footerDp = IGNORE_BOTTOM_DP) { Cfg() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun wipe_dark() = check("wipe", dark = true) { Cfg(wipe = true) }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun wipe_light() = check("wipe", dark = false) { Cfg(wipe = true) }

    @Composable private fun Cfg(wipe: Boolean = false) =
        ConfigScreen(ConfigMapper.map(CFG_DAY, LocalDate.parse("2026-09-25")).copy(wipeConfirm = wipe), ConfigActions())

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

    @Composable private fun O3() = SlotsScreen(GOLD_STATE, {}, { _, _ -> }, { _, _ -> }, {}, {})

    @Composable private fun O3S() = SlotsScreen(
        GOLD_STATE.copy(
            slots = listOf(SlotDraft(name = "Café da manhã", minutes = 570), SlotDraft(name = "Almoço", minutes = 810), SlotDraft(name = "Jantar", minutes = 1230)),
            slotSchedule = com.nutri.android.feature.onboarding.SlotScheduleDraft(mode = "split", index = 1),
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
        fullPage: Boolean = false,
        navDp: Int = NAV_DP,
        footerDp: Int = if (fullPage) FOOTER_DP else IGNORE_BOTTOM_DP,
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
        val figma = GoldInventory.source(goldId) == "figma"
        val statusDp = if (figma) 0 else STATUS_DP
        compose.setContent {
            DietaBotTheme(darkTheme = dark) {
                AeroTheme(darkTheme = dark) {
                    Box(Modifier.fillMaxSize().background(LocalPalette.current.phone)) {
                        Box(Modifier.padding(top = statusDp.dp, bottom = if (figma) 0.dp else navDp.dp)) { screen() }
                    }
                }
            }
        }
        compose.waitForIdle()
        val app = compose.onRoot().captureToImage().asAndroidBitmap()
        val theme = if (dark) "dark" else "light"
        save(app, File(RENDER_OUT, "$theme/$id.png"))

        val goldFile = File(ROOT, "docs/qa/${GoldInventory.source(goldId)}/$theme/$goldId.png")
        val gold = BitmapFactory.decodeFile(goldFile.path) ?: error("missing gold $goldFile")
        val top = if (fullPage) 0 else BAND_PX
        val raw = diff(app, gold, top)
        val appBlur = blur(app)
        val goldBlur = blur(gold)
        val blurred = diff(appBlur, goldBlur, top, if (figma) IGNORE_BOTTOM_DP else footerDp, topDp = statusDp)
        val inkRatio = ink(appBlur, 0, figma).toDouble() / ink(goldBlur, top, figma).coerceAtLeast(1)
        save(blurred.mask, File(DIFF_OUT, "$theme-$id.png"))
        println("GOLD_DIFF $theme/$id blurred ${"%.2f".format(blurred.percent)}% raw ${"%.2f".format(raw.percent)}% ink ${"%.2f".format(inkRatio)}")
        (listOfNotNull(region) + regions).forEachIndexed { i, box ->
            val part = regionDiff(appBlur, goldBlur, box)
            val name = if (i == 0) "region" else "region$i"
            println("GOLD_DIFF $theme/$id $name ${"%.2f".format(part)}%")
            if (gateRegion) assertWithMessage("$theme/$id $name differs from Stitch gold (blurred)").that(part).isAtMost(MAX_DIFF_PERCENT)
        }
        reportRegions.forEachIndexed { i, box ->
            println("GOLD_DIFF $theme/$id report$i ${"%.2f".format(regionDiff(appBlur, goldBlur, box))}%")
        }
        if ((id in GOLD_CONFLICTS && !figma) || reportOnly) return // reported only, see GOLD_CONFLICTS
        assertWithMessage("$theme/$id content ink vs gold").that(inkRatio).isIn(com.google.common.collect.Range.closed(0.8, 1.25))
        assertWithMessage("$theme/$id differs from the gold (blurred)").that(blurred.percent).isAtMost(MAX_DIFF_PERCENT)
    }

    private class Diff(val percent: Double, val mask: Bitmap)

    /** Share of compared pixels whose max channel delta exceeds [CHANNEL_TOLERANCE]. */
    private fun diff(app: Bitmap, gold: Bitmap, goldTop: Int, footerDp: Int = IGNORE_BOTTOM_DP, topDp: Int = STATUS_DP): Diff {
        val w = minOf(app.width, gold.width)
        val h = minOf(app.height, gold.height - goldTop)
        val from = topDp * 2
        // Full-page golds: the fixed FAB/CTA sits at the page bottom without Android nav insets,
        // so the footer is left out (same as tools/diff-gold.mjs).
        val to = h - footerDp * 2
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        var differ = 0
        var total = 0
        val a = IntArray(w)
        val g = IntArray(w)
        for (y in 0 until h) {
            app.getPixels(a, 0, w, 0, y, w, 1)
            gold.getPixels(g, 0, w, 0, y + goldTop, w, 1)
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
                val ay = y - BAND_PX + dy
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
    private fun ink(img: Bitmap, top: Int, fullFrame: Boolean = false): Int {
        val from = if (fullFrame) 0 else STATUS_DP * 2
        val to = (if (fullFrame) img.height - top else PHONE_ROWS.coerceAtMost(img.height - top)) - IGNORE_BOTTOM_DP * 2
        val sample = IntArray(0).toMutableList()
        for (y in from until to step 8) for (x in 0 until img.width step 8) sample += img.getPixel(x, y + top)
        val bg = intArrayOf(
            sample.map { Color.red(it) }.sorted()[sample.size / 2],
            sample.map { Color.green(it) }.sorted()[sample.size / 2],
            sample.map { Color.blue(it) }.sorted()[sample.size / 2],
        )
        var n = 0
        val row = IntArray(img.width)
        for (y in from until to) {
            img.getPixels(row, 0, img.width, 0, y + top, img.width, 1)
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
        private const val STATUS_DP = 40
        private const val NAV_DP = 24

        /** Stitch draws an iOS home pill at ~808 dp; the CTA ends at 792 dp. */
        private const val IGNORE_BOTTOM_DP = 40
        private const val FOOTER_DP = 130
        private const val BAND_PX = 40
        private const val CHANNEL_TOLERANCE = 40
        private const val BLUR_RADIUS = 3
        private const val PHONE_ROWS = 1688
        private const val MAX_DIFF_PERCENT = 2.0

        /**
         * Golds whose layout contradicts the canonical one of their group (home1, chatE): each is a
         * separate Stitch generation. Reported, not gated, until the owner regenerates them.
         */
        private val GOLD_CONFLICTS = setOf(
            // Chat: chatE is canonical. chat0/chatL use another header (body alone: ~1.2-1.4%).
            // See docs/android/plans/completed/a5-chat.md.
            "chat0", "chatL",
            // chatF, chatG, chatD: the receipt generation (A34, ST9): only the receipt and its actions are gated.
            "chatF", "chatG", "chatD",
            // chatA is a copy of chat0 (same other header): only its composer is gated (COMPOSER_ATTACHED, A19).
            "chatA",
            // chatX too (A25): only its composer and the error line are gated (COMPOSER_TOO_LONG).
            "chatX",
            // chatS is a copy of chat0 too (A29): only the routine card is gated (ROUTINE_CARD).
            "chatS",
        )

        /**
         * A34: the receipt(s), then the stacked actions, in gold px (x0, y0, x1, y1), per gold. Two boxes: the
         * golds put the actions 16.5 (chatD), 24 (chatF) and 28 dp (chatG) under the receipt; the app uses 22.
         * The chatF dark gold draws the receipt and buttons 314 dp wide (335 dp in the other five): reported only,
         * like its photo bubble (PHOTO_BUBBLE_DARK).
         */
        /**
         * chatF (ST9): the photo bubble below the header, its top scrolled under it (A6), in gold px. The dark
         * ST9 gold re-crops the photo and bolds the caption (light and the app keep the A6 bubble): dark reported.
         */
        private val PHOTO_BUBBLE_DARK = intArrayOf(214, 224, 746, 504)
        private val PHOTO_BUBBLE_LIGHT = intArrayOf(214, 224, 746, 526)
        private val RECEIPT_G_DARK = listOf(intArrayOf(48, 994, 732, 1136), intArrayOf(48, 1180, 732, 1488))
        private val RECEIPT_G_LIGHT = listOf(intArrayOf(48, 974, 732, 1116), intArrayOf(48, 1152, 732, 1460))
        private val RECEIPT_F_DARK = listOf(intArrayOf(68, 1096, 714, 1238), intArrayOf(68, 1274, 714, 1478))
        private val RECEIPT_F_LIGHT = listOf(intArrayOf(48, 1118, 732, 1222), intArrayOf(48, 1258, 732, 1462))
        private val RECEIPT_D_DARK = listOf(intArrayOf(48, 886, 732, 1144), intArrayOf(48, 1164, 732, 1472))
        private val RECEIPT_D_LIGHT = listOf(intArrayOf(48, 868, 732, 1140), intArrayOf(48, 1160, 732, 1468))

        /** chatA composer with the attached thumbnail in gold px (x0, y0, x1, y1). */
        private val COMPOSER_ATTACHED = intArrayOf(32, 1388, 748, 1664)

        /** chatX composer (5 lines, red border) and "Texto muito longo" in gold px (x0, y0, x1, y1). */
        private val COMPOSER_TOO_LONG = intArrayOf(32, 1344, 748, 1668)

        /** chatQ: the Forçar estimativa bar in gold px (x0, y0, x1, y1). */
        private val FORCE_BAR = intArrayOf(32, 1436, 748, 1544)

        /** chatR: "Dieta Bot AI" label, plan bubble with the day panel, time and Registrar assim, in gold px (x0, y0, x1, y1). */
        private val PLAN_BUBBLE = intArrayOf(32, 556, 748, 1540)

        /** chatM: the thread below the header down to the actions slot, in gold px (x0, y0, x1, y1). */
        private val CHAT_M_THREAD = intArrayOf(0, 230, 780, 1436)

        /** chatS routine card (title to buttons, 1 px border) in gold px (x0, y0, x1, y1). */
        private val ROUTINE_CARD = intArrayOf(32, 798, 748, 1302)
        private val ROOT = File("../../..")
        private val RENDER_OUT = File("build/outputs/stitch-gold/render")
        private val DIFF_OUT = File("build/outputs/stitch-gold/diff")

        /** State drawn in the cfg gold: 2000 kcal same, 0% eat-back, 150/200/67, 4 slots, no workout. */
        val CFG_DAY = HomeFixtures.day().copy(
            slots = listOf(
                MealSlot(1, "Café da manhã", 7 * 60 + 30),
                MealSlot(2, "Almoço", 12 * 60 + 30),
                MealSlot(3, "Lanche da tarde", 16 * 60),
                MealSlot(4, "Jantar", 20 * 60),
            ),
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
