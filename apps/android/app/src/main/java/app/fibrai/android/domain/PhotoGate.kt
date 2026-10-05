package app.fibrai.android.domain

import kotlin.math.max
import kotlin.math.roundToInt

/** Chat photo rules (spec memoria-push, foto; ADR-018): longest side ≤ 2048 px, JPEG q85, no EXIF. */
object PhotoGate {
    /** 16 MB JPEG. Defence only after ADR-018: a 2048 px JPEG q85 never gets near it (server caps the same, S1). */
    const val MAX_BYTES = 16L * 1024 * 1024

    /** Upload: longest side after [normalize][app.fibrai.android.core.photo.PhotoStore]. */
    const val MAX_SIDE = 2048

    /** Bubble preview: longest side ≤ 720 px after subsampling. */
    const val PREVIEW_SIDE = 720

    /** Every chat photo (camera or gallery, any format) is re-encoded to JPEG at this quality. */
    const val JPEG_QUALITY = 85

    fun fits(bytes: Long): Boolean = bytes in 1..MAX_BYTES

    /** Power-of-two BitmapFactory.inSampleSize so the longest side ends ≤ [maxSide]. */
    fun sampleSize(width: Int, height: Int, maxSide: Int = PREVIEW_SIDE): Int {
        val longest = max(width, height)
        var sample = 1
        while (longest / sample > maxSide) sample *= 2
        return sample
    }

    /**
     * Power-of-two decode sample that keeps the longest side ≥ [MAX_SIDE] (so between 2048 and 4096
     * for big photos): the cheap coarse step before the exact resize. 1 for photos already small.
     */
    fun decodeSample(width: Int, height: Int): Int {
        val longest = max(width, height)
        var sample = 1
        while (longest / (sample * 2) >= MAX_SIDE) sample *= 2
        return sample
    }

    /** Final size: longest side = [MAX_SIDE], aspect kept, never enlarged. */
    fun targetSize(width: Int, height: Int): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= MAX_SIDE) return width to height
        val scale = MAX_SIDE.toDouble() / longest
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }
}
