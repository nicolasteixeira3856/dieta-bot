package com.nutri.android.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaptureTest {
    @Test
    fun t0Day1ZeroChips() {
        val ui = captureState("t0")
        assertThat(ui.appDay).isEqualTo(1)
        assertThat(ui.chips).isEmpty()
        assertThat(ui.chipLabel).isNull()
        assertThat(ui.stage).isEqualTo(Stage.HOME)
        assertThat(ui.sheet).isNull()
        assertThat(ui.remaining).isEqualTo(1840)
    }

    @Test
    fun t0d2HasRemovableChip() {
        val ui = captureState("t0d2")
        assertThat(ui.appDay).isGreaterThan(1)
        assertThat(ui.chipLabel).isEqualTo("iogurte manhã")
        assertThat(ui.chipNote).isNotEmpty()
    }

    @Test
    fun splashIsBootStage() {
        val ui = captureState("splash")
        assertThat(ui.stage).isEqualTo(Stage.SPLASH)
        assertThat(ui.capture).isTrue()
    }

    @Test
    fun t1loadMarksLoading() {
        val ui = captureState("t1load")
        assertThat(ui.sheet).isEqualTo(SheetKind.T1)
        assertThat(ui.loading).isTrue()
    }

    @Test
    fun o1o2t3UseTheThreeModes() {
        assertThat(captureState("o1").ceilingMode).isEqualTo("same")
        assertThat(captureState("o2").eat).isEqualTo("zero")
        assertThat(captureState("t3quero").fitMode).isEqualTo("want")
        assertThat(captureState("t3tenho").fitMode).isEqualTo("have")
        assertThat(captureState("t3ideia").fitMode).isEqualTo("idea")
    }
}
