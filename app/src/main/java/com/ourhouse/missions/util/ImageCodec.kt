package com.ourhouse.missions.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Shrinks a picked photo until it fits comfortably inside a Firestore document
 * (hard limit 1 MiB) and converts it to/from base64, so photos can be stored without
 * Firebase Storage (which needs the paid Blaze plan).
 */
object ImageCodec {

    /** ~280 KB of JPEG once decoded — well under the 1 MiB document limit. */
    const val MAX_BASE64_CHARS = 380_000

    private val maxSides = listOf(1280, 1024, 800, 640)
    private val qualities = listOf(80, 70, 60, 50)

    /** Blocking — call from a background dispatcher. Throws if the image can't be read
     *  or can't be made small enough. */
    fun encode(resolver: ContentResolver, uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        require(longest > 0) { "לא ניתן לקרוא את התמונה" }

        // Decode at a power-of-two reduction first so a 12MP photo never lands in memory in full.
        var sample = 1
        while (longest / (sample * 2) >= maxSides.first()) sample *= 2
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: throw IllegalArgumentException("לא ניתן לקרוא את התמונה")

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val upright = rotate(decoded, orientation)

        for (side in maxSides) {
            val scaled = scaleToFit(upright, side)
            for (quality in qualities) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (encoded.length <= MAX_BASE64_CHARS) return encoded
            }
        }
        throw IllegalArgumentException("התמונה גדולה מדי")
    }

    /** Returns null if [data] isn't a decodable image. */
    fun decode(data: String): Bitmap? = runCatching {
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    private fun scaleToFit(source: Bitmap, maxSide: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxSide) return source
        val ratio = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(source, (source.width * ratio).toInt(), (source.height * ratio).toInt(), true)
    }

    private fun rotate(source: Bitmap, exifOrientation: Int): Bitmap {
        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
