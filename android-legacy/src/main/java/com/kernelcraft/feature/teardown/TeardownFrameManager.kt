package com.kernelcraft.feature.teardown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream

/**
 * High-performance frame cache manager for the teardown sequence.
 *
 * Provides instant, zero-latency 120 FPS scrubbing through pre-rendered
 * teardown frames (00 = Fully Assembled, 39 = Fully Exploded).
 * Completely bypasses host GPU Vulkan video decoding crashes on emulators.
 */
object TeardownFrameManager {
    const val TOTAL_FRAMES = 40
    private val frameCache = arrayOfNulls<Bitmap>(TOTAL_FRAMES)
    private var isPreloading = false

    /**
     * Retrieves the frame bitmap for a given index [0..39].
     * Synchronously decodes if not yet cached.
     */
    fun getFrame(context: Context, index: Int): Bitmap? {
        val clampedIndex = index.coerceIn(0, TOTAL_FRAMES - 1)
        frameCache[clampedIndex]?.let { return it }

        return try {
            loadBitmap(context, clampedIndex)?.also {
                frameCache[clampedIndex] = it
            }
        } catch (e: Exception) {
            Log.w("TeardownFrameManager", "Error decoding frame $clampedIndex", e)
            null
        }
    }

    /**
     * Preloads all frames in the background to ensure instantaneous scrub responsiveness.
     */
    fun preloadFrames(context: Context, scope: CoroutineScope) {
        if (isPreloading) return
        isPreloading = true

        scope.launch(Dispatchers.IO) {
            // Load start and end frames first for immediate visual readiness
            getFrame(context, 0)
            getFrame(context, TOTAL_FRAMES - 1)

            for (i in 0 until TOTAL_FRAMES) {
                if (frameCache[i] == null) {
                    loadBitmap(context, i)?.let {
                        frameCache[i] = it
                    }
                }
            }
        }
    }

    private fun loadBitmap(context: Context, index: Int): Bitmap? {
        val fileName = String.format("teardown_frames/frame_%02d.webp", index)
        return try {
            context.assets.open(fileName).use { stream: InputStream ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        for (i in 0 until TOTAL_FRAMES) {
            frameCache[i]?.recycle()
            frameCache[i] = null
        }
        isPreloading = false
    }
}
