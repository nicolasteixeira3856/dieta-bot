package com.nutri.android.domain

import kotlin.math.max

/** Chat photo rules (spec memoria-push, foto). No downscale for upload: the bytes go as they are. */
object PhotoGate {
    /** 16 MB JPEG. Above it the photo never reaches the API (server caps the same, S1). */
    const val MAX_BYTES = 16L * 1024 * 1024

    /** Bubble preview: longest side ≤ 720 px after subsampling. */
    const val PREVIEW_SIDE = 720

    /** HEIC / WebP / PNG from the gallery are re-encoded to JPEG at this quality, full size. */
    const val JPEG_QUALITY = 90

    fun fits(bytes: Long): Boolean = bytes in 1..MAX_BYTES

    /** Power-of-two BitmapFactory.inSampleSize so the longest side ends ≤ [maxSide]. */
    fun sampleSize(width: Int, height: Int, maxSide: Int = PREVIEW_SIDE): Int {
        val longest = max(width, height)
        var sample = 1
        while (longest / sample > maxSide) sample *= 2
        return sample
    }

    /** JPEG magic number FF D8 FF. */
    fun isJpeg(head: ByteArray): Boolean =
        head.size >= 3 && head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte() && head[2] == 0xFF.toByte()
}
