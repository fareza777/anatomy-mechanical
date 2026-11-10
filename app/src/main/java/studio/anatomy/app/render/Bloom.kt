package studio.anatomy.app.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect

/**
 * Cheap bloom for full-detail frames.
 *
 * Highlights are extracted into a quarter-resolution bitmap with a colour matrix,
 * blurred by compositing the same bitmap at a few offsets, then added back over the
 * frame. Everything runs through Skia, so there is no per-pixel Kotlin loop; the pass
 * is only used for paused frames and export plates, never while the mechanism runs.
 */
object BloomPass {

    fun apply(frame: Bitmap, strength: Float = 0.5f) {
        val width = frame.width
        val height = frame.height
        if (width < 64 || height < 64 || strength <= 0.01f) return
        val smallWidth = (width / 4).coerceAtLeast(8)
        val smallHeight = (height / 4).coerceAtLeast(8)

        var small: Bitmap? = null
        var highlights: Bitmap? = null
        var blurred: Bitmap? = null
        try {
            val downscaled = Bitmap.createScaledBitmap(frame, smallWidth, smallHeight, true)
            small = downscaled
            val bright = Bitmap.createBitmap(smallWidth, smallHeight, Bitmap.Config.ARGB_8888)
            highlights = bright
            val threshold = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(
                    ColorMatrix(
                        floatArrayOf(
                            1.55f, 0f, 0f, 0f, -96f,
                            0f, 1.55f, 0f, 0f, -96f,
                            0f, 0f, 1.55f, 0f, -96f,
                            0f, 0f, 0f, 1f, 0f,
                        )
                    )
                )
            }
            Canvas(bright).drawBitmap(downscaled, 0f, 0f, threshold)

            val glow = Bitmap.createBitmap(smallWidth, smallHeight, Bitmap.Config.ARGB_8888)
            blurred = glow
            val blurCanvas = Canvas(glow)
            val spread = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 78 }
            val radius = (smallWidth / 90f).coerceAtLeast(1.6f)
            blurCanvas.drawBitmap(bright, 0f, 0f, spread)
            blurCanvas.drawBitmap(bright, radius, 0f, spread)
            blurCanvas.drawBitmap(bright, -radius, 0f, spread)
            blurCanvas.drawBitmap(bright, 0f, radius, spread)
            blurCanvas.drawBitmap(bright, 0f, -radius, spread)

            val add = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                alpha = (strength.coerceIn(0f, 1f) * 255f).toInt()
            }
            Canvas(frame).drawBitmap(glow, null, Rect(0, 0, width, height), add)
        } catch (oom: OutOfMemoryError) {
            // Bloom is decoration; losing it must never take the frame down.
        } finally {
            if (small !== frame) small?.recycle()
            if (highlights !== frame) highlights?.recycle()
            if (blurred !== frame) blurred?.recycle()
        }
    }
}
