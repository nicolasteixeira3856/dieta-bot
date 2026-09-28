package com.nutri.android.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PhotoGateTest {
    @Test
    fun rejectsOver16Mb_andEmpty() {
        assertThat(PhotoGate.fits(0)).isFalse()
        assertThat(PhotoGate.fits(1)).isTrue()
        assertThat(PhotoGate.fits(16L * 1024 * 1024)).isTrue()
        assertThat(PhotoGate.fits(16L * 1024 * 1024 + 1)).isFalse()
    }

    @Test
    fun previewSample_keepsLongestSideAtMost720() {
        assertThat(PhotoGate.sampleSize(4000, 3000)).isEqualTo(8) // 500 x 375
        assertThat(PhotoGate.sampleSize(3000, 4032)).isEqualTo(8) // 375 x 504
        assertThat(PhotoGate.sampleSize(1441, 900)).isEqualTo(2)
        assertThat(PhotoGate.sampleSize(720, 540)).isEqualTo(1)
        for ((w, h) in listOf(4000 to 3000, 6000 to 8000, 1280 to 960, 721 to 10)) {
            assertThat(maxOf(w, h) / PhotoGate.sampleSize(w, h)).isAtMost(PhotoGate.PREVIEW_SIDE)
        }
    }

    @Test
    fun jpegMagic() {
        assertThat(PhotoGate.isJpeg(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))).isTrue()
        assertThat(PhotoGate.isJpeg("RIFF".toByteArray())).isFalse()
        assertThat(PhotoGate.isJpeg(byteArrayOf(0xFF.toByte()))).isFalse()
    }
}
