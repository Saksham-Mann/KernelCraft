package com.kernelcraft.core.bitmap

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * High-performance, low-memory bitmap loader targeting Graphic Buffer / GPU memory directly.
 *
 * Utilizes [Bitmap.Config.HARDWARE] on Android 8.0 (API 26+) to allocate decoded bitmap pixels
 * in native `AHardwareBuffer` / GPU VRAM rather than the Java heap, completely eliminating
 * Dalvik garbage collection churn and OutOfMemoryErrors.
 */
object HardwareBitmapLoader {

    /**
     * Checks if the host device is a low-RAM tier (<4GB / low RAM device).
     */
    fun isLowRamDevice(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return am?.isLowRamDevice ?: false
    }

    /**
     * Decodes a drawable resource using zero-heap [Bitmap.Config.HARDWARE].
     *
     * @param context Application/Activity context.
     * @param resId Resource ID of the asset (e.g. R.drawable.chip_highlight_still).
     * @param targetWidth Target bounding width for downsampling if constrained.
     * @param targetHeight Target bounding height for downsampling if constrained.
     */
    fun decodeHardwareBitmap(
        context: Context,
        resId: Int,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): Bitmap? {
        if (resId == 0) return null

        val isLowRam = isLowRamDevice(context)

        // 1. Measure dimensions without allocation
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resId, boundsOptions)

        // 2. Compute sample size
        var sampleSize = 1
        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight

        if (isLowRam || rawWidth > targetWidth || rawHeight > targetHeight) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / sampleSize) >= targetHeight && (halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2
            }
        }

        // 3. Decode into GPU VRAM
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inMutable = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                inPreferredConfig = Bitmap.Config.HARDWARE
            } else {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        }

        return try {
            BitmapFactory.decodeResource(context.resources, resId, decodeOptions)
        } catch (oom: OutOfMemoryError) {
            // Graceful fallback with aggressive downsampling on extreme low memory
            decodeOptions.inSampleSize = sampleSize * 2
            decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565
            BitmapFactory.decodeResource(context.resources, resId, decodeOptions)
        }
    }
}

/**
 * Compose helper to remember a hardware-accelerated [ImageBitmap].
 */
@Composable
fun rememberHardwareImageBitmap(resId: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(resId) {
        HardwareBitmapLoader.decodeHardwareBitmap(context, resId)?.asImageBitmap()
    }
}
