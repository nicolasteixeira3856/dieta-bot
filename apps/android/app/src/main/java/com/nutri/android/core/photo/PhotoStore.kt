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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PhotoResult {
    /** JPEG q85 in app storage, longest side ≤ 2048 px, no EXIF (ADR-018). */
    data class Ready(val path: String) : PhotoResult

    /** Did not fit in memory to decode (or still over 16 MB after the resize): deleted, never posted. */
    data object TooLarge : PhotoResult

    data object Failed : PhotoResult
}

/** Chat photos in filesDir/photos. The ViewModel only sees this. */
interface PhotoFiles {
    /** Empty target file + content Uri for ActivityResultContracts.TakePicture. */
    fun newCapture(): Pair<File, Uri>

    /** Camera file → normalized JPEG (see [PhotoStore.normalize]). The original is deleted. */
    suspend fun acceptCapture(file: File): PhotoResult

    /** Photo picker Uri → normalized JPEG, whatever the source format. */
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

    override suspend fun acceptCapture(file: File): PhotoResult = withContext(Dispatchers.IO) {
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        try {
            if (!file.exists() || file.length() == 0L) PhotoResult.Failed else normalize(file, target)
        } finally {
            file.delete()
        }
    }

    override suspend fun import(uri: Uri): PhotoResult = withContext(Dispatchers.IO) {
        val raw = File(dir, "${UUID.randomUUID()}.import")
        val target = File(dir, raw.nameWithoutExtension + ".jpg")
        try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input -> raw.outputStream().use { input.copyTo(it) } }
            if (copied == null) PhotoResult.Failed else normalize(raw, target)
        } catch (e: Exception) {
            target.delete()
            PhotoResult.Failed
        } finally {
            raw.delete()
        }
    }

    /**
     * ADR-018, camera and gallery alike: power-of-two subsample on decode, EXIF rotation applied,
     * longest side resized to 2048 (never enlarged), JPEG q85. The EXIF block is not copied.
     */
    private fun normalize(source: File, target: File): PhotoResult {
        val bitmap = try {
            decodeUpright(source)
        } catch (e: OutOfMemoryError) {
            return PhotoResult.TooLarge.also { target.delete() }
        } ?: return PhotoResult.Failed.also { target.delete() }
        try {
            val (w, h) = PhotoGate.targetSize(bitmap.width, bitmap.height)
            val sized = if (w == bitmap.width && h == bitmap.height) bitmap else Bitmap.createScaledBitmap(bitmap, w, h, true)
            target.outputStream().use { sized.compress(Bitmap.CompressFormat.JPEG, PhotoGate.JPEG_QUALITY, it) }
            if (sized !== bitmap) sized.recycle()
        } catch (e: OutOfMemoryError) {
            return PhotoResult.TooLarge.also { target.delete() }
        } finally {
            bitmap.recycle()
        }
        return gate(target)
    }

    /** Subsampled so the longest side stays in 2048..4096, turned upright. Null when not an image. */
    private fun decodeUpright(file: File): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Applies the EXIF / HEIF rotation; software bitmap so it can be scaled and compressed.
            return runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.setTargetSampleSize(PhotoGate.decodeSample(info.size.width, info.size.height))
                }
            }.getOrElse { if (it is OutOfMemoryError) throw it else null }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = PhotoGate.decodeSample(bounds.outWidth, bounds.outHeight) }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return null
        val degrees = exifDegrees(file.path)
        if (degrees == 0f) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
            .also { if (it !== bitmap) bitmap.recycle() }
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
            // Photos stored since ADR-018 are already upright; older ones still carry EXIF.
            val degrees = exifDegrees(path)
            if (degrees == 0f) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
        }.getOrNull()

        private fun exifDegrees(path: String): Float = runCatching {
            when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoModule {
    @Binds
    abstract fun photoFiles(impl: PhotoStore): PhotoFiles
}
