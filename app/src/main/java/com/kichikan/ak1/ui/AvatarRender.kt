package com.kichikan.ak1.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a member's avatar *inside* their frame.
 *
 * The frame's transparent hole is measured from the image itself, so any frame (built-in or
 * imported) works: the avatar is clipped to that hole and can never stick out of the frame.
 */
object AvatarRender {
    private val holeCache = ConcurrentHashMap<String, Float>()

    fun decodeSampled(path: String?, target: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }

    /**
     * The same sampling rule for images opened through a content Uri (photo picker).
     * Without it a multi-megapixel photo is decoded at full size just to fill a 112dp circle,
     * which can exhaust the heap.
     */
    fun decodeSampled(context: android.content.Context, uri: android.net.Uri, target: Int): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }.getOrNull()
    }

    /** Diameter of the frame's transparent opening as a fraction of the frame size (0.5..0.95). */
    fun holeRatio(frame: Bitmap, key: String?): Float {
        key?.let { holeCache[it]?.let { cached -> return cached } }
        val w = frame.width
        val h = frame.height
        val cx = w / 2
        val cy = h / 2
        fun opaque(x: Int, y: Int) = Color.alpha(frame.getPixel(x.coerceIn(0, w - 1), y.coerceIn(0, h - 1))) >= 40
        var ratio = 0.74f
        if (!opaque(cx, cy)) {
            var smallest = Float.MAX_VALUE
            val steps = 16
            for (i in 0 until steps) {
                val angle = Math.PI * 2 * i / steps
                val dx = Math.cos(angle)
                val dy = Math.sin(angle)
                var r = 0
                val limit = min(w, h) / 2
                while (r < limit && !opaque((cx + dx * r).toInt(), (cy + dy * r).toInt())) r++
                smallest = min(smallest, r.toFloat())
            }
            if (smallest < Float.MAX_VALUE) ratio = (2f * smallest / min(w, h)).coerceIn(0.74f, 0.95f)
        }
        key?.let { holeCache[it] = ratio }
        return ratio
    }

    /** Square bitmap of [size] px: avatar clipped inside the frame's hole, frame on top. */
    fun compose(avatar: Bitmap?, frame: Bitmap?, frameKey: String?, size: Int, letter: String = ""): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val ratio = if (frame != null) holeRatio(frame, frameKey) else 0.94f
        val diameter = if (frame != null) min(size * ratio * 1.035f, size * 0.99f) else size * ratio * 0.985f
        val c = size / 2f
        val dst = RectF(c - diameter / 2f, c - diameter / 2f, c + diameter / 2f, c + diameter / 2f)
        canvas.save()
        canvas.clipPath(Path().apply { addCircle(c, c, diameter / 2f, Path.Direction.CW) })
        if (avatar != null) {
            val side = min(avatar.width, avatar.height)
            val src = Rect((avatar.width - side) / 2, (avatar.height - side) / 2, (avatar.width + side) / 2, (avatar.height + side) / 2)
            canvas.drawBitmap(avatar, src, dst, paint)
        } else {
            paint.shader = RadialGradient(c - diameter * 0.15f, c - diameter * 0.2f, diameter, intArrayOf(0xFF3A5BA8.toInt(), 0xFF0F2A66.toInt()), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(c, c, diameter / 2f, paint)
            paint.shader = null
            if (letter.isNotBlank()) {
                val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE; textSize = diameter * 0.46f; textAlign = Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                }
                canvas.drawText(letter.take(1), c, c + text.textSize * 0.35f, text)
            }
        }
        canvas.restore()
        if (frame != null) canvas.drawBitmap(frame, null, Rect(0, 0, size, size), paint)
        return out
    }

    fun compose(avatarPath: String?, framePath: String?, size: Int, letter: String = ""): Bitmap =
        compose(decodeSampled(avatarPath, size), decodeSampled(framePath, size), framePath, size, letter)
}

@Composable
fun MemberAvatar(avatarPath: String?, framePath: String?, size: Dp, letter: String = "", modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { (size * 2f).roundToPx().coerceIn(96, 640) }
    val image = remember(avatarPath, framePath, px, letter) {
        AvatarRender.compose(avatarPath, framePath, px, letter).asImageBitmap()
    }
    Image(bitmap = image, contentDescription = null, modifier = modifier.size(size))
}
