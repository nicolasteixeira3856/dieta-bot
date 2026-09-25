package com.nutri.android.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.nutri.android.domain.PhotoScale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoCompressor @Inject constructor(@ApplicationContext private val context: Context) {
    fun jpegBase64(uri: Uri): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val (w, h) = PhotoScale.dimensions(bounds.outWidth, bounds.outHeight)
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val scaled = if (original.width == w && original.height == h) {
            original
        } else {
            Bitmap.createScaledBitmap(original, w, h, true)
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, PhotoScale.JPEG_QUALITY, out)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
