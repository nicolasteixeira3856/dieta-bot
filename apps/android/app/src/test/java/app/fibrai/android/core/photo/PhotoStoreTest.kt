package app.fibrai.android.core.photo

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import app.fibrai.android.domain.PhotoGate
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

    private fun size(path: String): Pair<Int, Int> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        return bounds.outWidth to bounds.outHeight
    }

    private fun isJpeg(bytes: ByteArray) = bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()

    @Test
    fun smallJpegFromGallery_isReencoded_sameSize() = runBlocking<Unit> {
        val result = store.import(source(encoded(Bitmap.CompressFormat.JPEG), "a.jpg")) as PhotoResult.Ready
        assertThat(isJpeg(File(result.path).readBytes())).isTrue()
        assertThat(size(result.path)).isEqualTo(1600 to 1200)
        assertThat(result.path).startsWith(File(context.filesDir, "photos").path)
        assertThat(File(context.filesDir, "photos").listFiles()!!.none { it.name.endsWith(".import") }).isTrue()
    }

    @Test
    fun bigPhoto_longestSideBecomes2048() = runBlocking<Unit> {
        // 4400 x 3300: decode sample 2 (2200 x 1650), then the exact resize.
        val result = store.import(source(encoded(Bitmap.CompressFormat.JPEG, 4400, 3300), "big.jpg")) as PhotoResult.Ready
        assertThat(size(result.path)).isEqualTo(2048 to 1536)

        val capture = File(context.filesDir, "photos/cap.jpg").apply { parentFile!!.mkdirs(); writeBytes(encoded(Bitmap.CompressFormat.JPEG, 3300, 4400)) }
        val captured = store.acceptCapture(capture) as PhotoResult.Ready
        assertThat(size(captured.path)).isEqualTo(1536 to 2048)
        assertThat(capture.exists()).isFalse()
    }

    @Test
    fun nonJpeg_becomesJpeg() = runBlocking<Unit> {
        // WebP / PNG take the same path as HEIC.
        for (format in listOf(Bitmap.CompressFormat.PNG, Bitmap.CompressFormat.WEBP)) {
            val result = store.import(source(encoded(format, 1600, 1200), "b.img")) as PhotoResult.Ready
            assertThat(isJpeg(File(result.path).readBytes())).isTrue()
            assertThat(size(result.path)).isEqualTo(1600 to 1200)
        }
    }

    @Test
    fun exifRotationApplied_exifNotCopied() = runBlocking<Unit> {
        val file = File(context.cacheDir, "exif.jpg").apply { writeBytes(encoded(Bitmap.CompressFormat.JPEG, 1600, 1200)) }
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "23/1,33/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S")
            setAttribute(ExifInterface.TAG_MAKE, "Phone")
            saveAttributes()
        }
        val result = store.import(Uri.fromFile(file)) as PhotoResult.Ready
        assertThat(size(result.path)).isEqualTo(1200 to 1600)
        val exif = ExifInterface(result.path)
        assertThat(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED))
            .isAnyOf(ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL)
        assertThat(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)).isNull()
        assertThat(exif.getAttribute(ExifInterface.TAG_MAKE)).isNull()
    }

    @Test
    fun formerly16MbFile_nowPassesSmall() = runBlocking<Unit> {
        // Valid JPEG + padding up to 16 MB + 1 byte: before ADR-018 this was "Foto grande demais.".
        val big = encoded(Bitmap.CompressFormat.JPEG).copyOf((PhotoGate.MAX_BYTES + 1).toInt())
        val result = store.import(source(big, "big.jpg")) as PhotoResult.Ready
        assertThat(File(result.path).length()).isLessThan(2L * 1024 * 1024)
        assertThat(store.base64(result.path)).isNotNull()
    }

    @Test
    fun notAnImage_isFailed_nothingLeft() = runBlocking<Unit> {
        val before = File(context.filesDir, "photos").listFiles()?.size ?: 0
        assertThat(store.import(source("not a photo".toByteArray(), "x.bin"))).isEqualTo(PhotoResult.Failed)
        assertThat(File(context.filesDir, "photos").listFiles()!!.size).isEqualTo(before)
    }

    @Test
    fun base64_refusesOver16Mb() = runBlocking<Unit> {
        val huge = File(context.filesDir, "photos/huge.jpg").apply { parentFile!!.mkdirs(); writeBytes(ByteArray((PhotoGate.MAX_BYTES + 1).toInt())) }
        assertThat(store.base64(huge.path)).isNull()
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
