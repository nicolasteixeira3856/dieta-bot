package com.nutri.android.core.photo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nutri.android.domain.PhotoGate
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class PhotoStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val store = PhotoStore(context)

    private fun encoded(format: Bitmap.CompressFormat, w: Int = 1600, h: Int = 1200): ByteArray {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 120, 40)) }
        return ByteArrayOutputStream().also { bitmap.compress(format, 80, it) }.toByteArray()
    }

    private fun source(bytes: ByteArray, name: String): Uri =
        Uri.fromFile(File(context.cacheDir, name).apply { writeBytes(bytes) })

    @Test
    fun jpegFromGallery_isCopiedByteForByte() = runBlocking<Unit> {
        val jpeg = encoded(Bitmap.CompressFormat.JPEG)
        val result = store.import(source(jpeg, "a.jpg")) as PhotoResult.Ready
        assertThat(File(result.path).readBytes()).isEqualTo(jpeg)
        assertThat(result.path).startsWith(File(context.filesDir, "photos").path)
        assertThat(File(context.filesDir, "photos").listFiles()!!.none { it.name.endsWith(".import") }).isTrue()
    }

    @Test
    fun nonJpeg_becomesFullSizeJpeg() = runBlocking<Unit> {
        // WebP / PNG take the same branch as HEIC: decode, JPEG q90, no downscale.
        for (format in listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.WEBP)) {
            val result = store.import(source(encoded(format, 1600, 1200), "b.img")) as PhotoResult.Ready
            val bytes = File(result.path).readBytes()
            assertThat(PhotoGate.isJpeg(bytes)).isTrue()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(result.path, bounds)
            assertThat(bounds.outWidth to bounds.outHeight).isEqualTo(1600 to 1200)
        }
    }

    @Test
    fun over16Mb_isRejectedAndDeleted_neverEncoded() = runBlocking<Unit> {
        // Valid JPEG header + padding: 16 MB + 1 byte.
        val big = encoded(Bitmap.CompressFormat.JPEG).copyOf((PhotoGate.MAX_BYTES + 1).toInt())
        assertThat(store.import(source(big, "big.jpg"))).isEqualTo(PhotoResult.TooLarge)
        assertThat(File(context.filesDir, "photos").listFiles()!!.filter { it.length() > PhotoGate.MAX_BYTES }).isEmpty()

        val capture = File(context.filesDir, "photos/cap.jpg").apply { parentFile!!.mkdirs(); writeBytes(big) }
        assertThat(store.acceptCapture(capture)).isEqualTo(PhotoResult.TooLarge)
        assertThat(capture.exists()).isFalse()
        assertThat(store.base64(capture.path)).isNull()
    }

    @Test
    fun cancelledCapture_isFailed() = runBlocking<Unit> {
        val empty = File(context.filesDir, "photos/empty.jpg").apply { parentFile!!.mkdirs(); writeBytes(ByteArray(0)) }
        assertThat(store.acceptCapture(empty)).isEqualTo(PhotoResult.Failed)
        assertThat(empty.exists()).isFalse()
    }

    @Test
    fun preview_isSubsampledTo720() {
        val jpeg = encoded(Bitmap.CompressFormat.JPEG, 4000, 3000)
        val file = File(context.cacheDir, "big-preview.jpg").apply { writeBytes(jpeg) }
        val preview = PhotoStore.preview(file.path)!!
        assertThat(maxOf(preview.width, preview.height)).isAtMost(PhotoGate.PREVIEW_SIDE)
        assertThat(preview.width).isEqualTo(500)
    }
}
