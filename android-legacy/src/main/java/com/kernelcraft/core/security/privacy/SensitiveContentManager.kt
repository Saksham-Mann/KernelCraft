package com.kernelcraft.core.security.privacy

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager

/**
 * Manages modern Android 15 (API 35+) sensitive content protection, dynamic screen
 * recording defense, and immutable intent declarations.
 */
object SensitiveContentManager {

    /**
     * Applies Android 15+ Sensitive Content Protection to an individual View hierarchy.
     * When sensitive, the OS automatically redacts/masks the view during screen sharing,
     * remote desktop projection, and untrusted accessibility captures.
     */
    fun applyContentSensitivity(view: View, isSensitive: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // API 35+
            try {
                val sensitivityFlag = if (isSensitive) {
                    View.CONTENT_SENSITIVITY_SENSITIVE
                } else {
                    View.CONTENT_SENSITIVITY_NOT_SENSITIVE
                }
                view.contentSensitivity = sensitivityFlag
            } catch (_: Throwable) {
                // Gracefully fallback on older preview builds
            }
        }
    }

    /**
     * Dynamically sets or clears WindowManager.LayoutParams.FLAG_SECURE on the target Window.
     */
    fun setScreenCaptureProtection(window: Window, enableSecure: Boolean) {
        if (enableSecure) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Safe PendingIntent factory strictly enforcing PendingIntent.FLAG_IMMUTABLE.
     * Prevents intent mutation attacks and unauthorized privilege escalation.
     */
    fun createSafePendingIntent(
        context: Context,
        requestCode: Int,
        intent: Intent,
        flags: Int = 0
    ): PendingIntent {
        val safeFlags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            safeFlags
        )
    }

    /**
     * Checks if a bitmask contains the mandatory FLAG_IMMUTABLE bit.
     */
    fun isPendingIntentImmutable(flags: Int): Boolean {
        return (flags and PendingIntent.FLAG_IMMUTABLE) != 0
    }
}
