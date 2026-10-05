package app.fibrai.android.domain

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
    fun upload_longestSideIs2048_neverEnlarged() {
        assertThat(PhotoGate.targetSize(8000, 6000)).isEqualTo(2048 to 1536)
        assertThat(PhotoGate.targetSize(6000, 8000)).isEqualTo(1536 to 2048)
        assertThat(PhotoGate.targetSize(1000, 800)).isEqualTo(1000 to 800)
        assertThat(PhotoGate.targetSize(2048, 10)).isEqualTo(2048 to 10)
    }

    @Test
    fun decodeSample_keepsLongestSideBetween2048And4096() {
        assertThat(PhotoGate.decodeSample(8000, 6000)).isEqualTo(2) // 4000
        assertThat(PhotoGate.decodeSample(8160, 6120)).isEqualTo(2) // 50 MP: 4080
        assertThat(PhotoGate.decodeSample(16320, 12240)).isEqualTo(4) // 200 MP: 4080
        assertThat(PhotoGate.decodeSample(4096, 3072)).isEqualTo(2) // 2048
        assertThat(PhotoGate.decodeSample(4095, 3000)).isEqualTo(1)
        assertThat(PhotoGate.decodeSample(1000, 800)).isEqualTo(1)
        for ((w, h) in listOf(8000 to 6000, 16320 to 12240, 5000 to 100, 12000 to 9000)) {
            assertThat(maxOf(w, h) / PhotoGate.decodeSample(w, h)).isIn(com.google.common.collect.Range.closed(PhotoGate.MAX_SIDE, 2 * PhotoGate.MAX_SIDE))
        }
    }
}
