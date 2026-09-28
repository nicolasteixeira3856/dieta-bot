package com.nutri.android.core.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.FileProvider
import com.nutri.android.domain.PhotoGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PhotoResult {
    /** JPEG in app storage, ≤ 16 MB. */
    data class Ready(val path: String) : PhotoResult

    /** Over 16 MB: deleted, never posted. */
    data object TooLarge : PhotoResult

    data object Failed : PhotoResult
}

/** Chat photos in filesDir/photos. The ViewModel only sees this. */
interface PhotoFiles {
    /** Empty target file + content Uri for ActivityResultContracts.TakePicture. */
    fun newCapture(): Pair<File, Uri>

    suspend fun acceptCapture(file: File): PhotoResult

    /** Photo picker Uri → JPEG file. JPEG bytes are copied as they are; anything else becomes JPEG q90. */
    suspend fun import(uri: Uri): PhotoResult

    /** Upload body: the file bytes, base64 (no wrap). */
    suspend fun base64(path: String): String?

    fun delete(path: String)
}

@Singleton
class PhotoStore @Inject constructor(@ApplicationContext private val context: Context) : PhotoFiles {
    private val dir: File get() = File(context.filesDir, DIR).apply { mkdirs() }

    override fun newCapture(): Pair<File, Uri> {
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        return file to FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    override suspend fun acceptCapture(file: File): PhotoResult = withContext(Dispatchers.IO) { gate(file) }

    override suspend fun import(uri: Uri): PhotoResult = withContext(Dispatchers.IO) {
        val raw = File(dir, "${UUID.randomUUID()}.import")
        val target = File(dir, raw.nameWithoutExtension + ".jpg")
        try {
            importInto(uri, raw, target)
        } catch (e: Exception) {
            target.delete()
            PhotoResult.Failed
        } finally {
            raw.delete()
        }
    }

    private fun importInto(uri: Uri, raw: File, target: File): PhotoResult {
        val copied = context.contentResolver.openInputStream(uri)?.use { input -> raw.outputStream().use { input.copyTo(it) } }
        if (copied == null) return PhotoResult.Failed
        if (PhotoGate.isJpeg(head(raw))) {
            if (!raw.renameTo(target)) return PhotoResult.Failed
        } else {
            // HEIC, WebP, PNG...: full-size JPEG q90, no downscale (spec foto rule 2).
            val bitmap = decodeFull(raw) ?: return PhotoResult.Failed
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, PhotoGate.JPEG_QUALITY, it) }
            bitmap.recycle()
        }
        return gate(target)
    }

    override suspend fun base64(path: String): String? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!PhotoGate.fits(file.length())) return@withContext null
        runCatching { Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) }.getOrNull()
    }

    override fun delete(path: String) {
        File(path).delete()
    }

    private fun gate(file: File): PhotoResult = when {
        !file.exists() || file.length() == 0L -> PhotoResult.Failed.also { file.delete() }
        !PhotoGate.fits(file.length()) -> PhotoResult.TooLarge.also { file.delete() }
        else -> PhotoResult.Ready(file.path)
    }

    private fun head(file: File): ByteArray = FileInputStream(file).use { input -> ByteArray(3).also { input.read(it) } }

    private fun decodeFull(file: File): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Applies EXIF/HEIF rotation; software bitmap so it can be compressed.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            BitmapFactory.decodeFile(file.path)
        }
    }.getOrNull()

    companion object {
        const val DIR = "photos"
        const val AUTHORITY_SUFFIX = ".photos"

        /** Bubble preview: subsampled (longest side ≤ 720) and turned by the EXIF orientation. */
        fun preview(path: String): Bitmap? = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            val options = BitmapFactory.Options().apply { inSampleSize = PhotoGate.sampleSize(bounds.outWidth, bounds.outHeight) }
            val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
            val degrees = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
        }.getOrNull()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoModule {
    @Binds
    abstract fun photoFiles(impl: PhotoStore): PhotoFiles
}
