package com.nutri.android.ui

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
import com.nutri.android.core.designsystem.LocalPalette
import com.nutri.android.core.designsystem.NutriTheme
import com.nutri.android.feature.onboarding.CeilingScreen
import com.nutri.android.feature.onboarding.EatScreen
import com.nutri.android.feature.onboarding.MacrosScreen
import com.nutri.android.feature.onboarding.OnboardingUiState
import com.nutri.android.feature.onboarding.SlotDraft
import com.nutri.android.feature.onboarding.SlotsScreen
import com.nutri.android.feature.splash.SplashScreen
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders splash + O1..O4 with the state shown in the Stitch gold and diffs them against
 * docs/qa/stitch/{theme}/<id>.png. JVM renders and diff masks land in build/outputs/stitch-gold/.
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

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o2_dark() = check("o2", dark = true) { O2() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o2_light() = check("o2", dark = false) { O2() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1103dp-xhdpi")
    fun o3_dark() = check("o3", dark = true, fullPage = true) { O3() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h1103dp-xhdpi")
    fun o3_light() = check("o3", dark = false, fullPage = true) { O3() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o4_dark() = check("o4", dark = true) { O4() }

    @Test @Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
    fun o4_light() = check("o4", dark = false) { O4() }

    @Composable private fun O1() = CeilingScreen(GOLD_STATE, {}, {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {})

    @Composable private fun O2() = EatScreen(GOLD_STATE, {}, {}, {}, {})

    @Composable private fun O3() = SlotsScreen(GOLD_STATE, {}, { _, _ -> }, { _, _ -> }, {}, {})

    @Composable private fun O4() = MacrosScreen(GOLD_STATE, {}, {}, {}, {}, {})

    private fun check(id: String, dark: Boolean, fullPage: Boolean = false, screen: @Composable () -> Unit) {
        compose.setContent {
            NutriTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(LocalPalette.current.phone)) {
                    Box(Modifier.padding(top = STATUS_DP.dp, bottom = NAV_DP.dp)) { screen() }
                }
            }
        }
        compose.waitForIdle()
        val app = compose.onRoot().captureToImage().asAndroidBitmap()
        val theme = if (dark) "dark" else "light"
        save(app, File(RENDER_OUT, "$theme/$id.png"))

        val goldFile = File(GOLD, "$theme/$id.png")
        val gold = BitmapFactory.decodeFile(goldFile.path) ?: error("missing gold $goldFile")
        val top = if (fullPage) 0 else BAND_PX
        val raw = diff(app, gold, top)
        val appBlur = blur(app)
        val goldBlur = blur(gold)
        val blurred = diff(appBlur, goldBlur, top)
        val inkRatio = ink(appBlur, 0).toDouble() / ink(goldBlur, top).coerceAtLeast(1)
        save(blurred.mask, File(DIFF_OUT, "$theme-$id.png"))
        println("GOLD_DIFF $theme/$id blurred ${"%.2f".format(blurred.percent)}% raw ${"%.2f".format(raw.percent)}% ink ${"%.2f".format(inkRatio)}")
        assertWithMessage("$theme/$id content ink vs gold").that(inkRatio).isIn(com.google.common.collect.Range.closed(0.8, 1.25))
        assertWithMessage("$theme/$id differs from Stitch gold (blurred)").that(blurred.percent).isAtMost(MAX_DIFF_PERCENT)
    }

    private class Diff(val percent: Double, val mask: Bitmap)

    /** Share of compared pixels whose max channel delta exceeds [CHANNEL_TOLERANCE]. */
    private fun diff(app: Bitmap, gold: Bitmap, goldTop: Int): Diff {
        val w = minOf(app.width, gold.width)
        val h = minOf(app.height, gold.height - goldTop)
        val from = STATUS_DP * 2
        val to = h - IGNORE_BOTTOM_DP * 2
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

    /** Pixels off the page background (median colour) in the compared rows: catches blank captures. */
    private fun ink(img: Bitmap, top: Int): Int {
        val from = STATUS_DP * 2
        val to = PHONE_ROWS.coerceAtMost(img.height - top) - IGNORE_BOTTOM_DP * 2
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
        private const val BAND_PX = 40
        private const val CHANNEL_TOLERANCE = 40
        private const val BLUR_RADIUS = 3
        private const val PHONE_ROWS = 1688
        private const val MAX_DIFF_PERCENT = 2.0
        private val ROOT = File("../../..")
        private val GOLD = File(ROOT, "docs/qa/stitch")
        private val RENDER_OUT = File("build/outputs/stitch-gold/render")
        private val DIFF_OUT = File("build/outputs/stitch-gold/diff")

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
