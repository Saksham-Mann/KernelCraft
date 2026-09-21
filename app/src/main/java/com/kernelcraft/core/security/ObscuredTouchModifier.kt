package com.kernelcraft.core.security

import android.view.MotionEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInteropFilter

/**
 * Defensive Compose modifier that rejects obscured and partially obscured touch events,
 * preventing UI redressing, overlay clickjacking, and tapjacking attacks.
 *
 * Checks:
 * - [MotionEvent.FLAG_WINDOW_IS_OBSCURED]: Another window is completely obscuring the area.
 * - [MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED]: A transparent or partial overlay covers the touch target.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.rejectObscuredTouches(
    onObscuredTouchDetected: (() -> Unit)? = null
): Modifier {
    return this.pointerInteropFilter { motionEvent ->
        val flags = motionEvent.flags
        val isObscured = (flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0 ||
            (flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0

        if (isObscured) {
            onObscuredTouchDetected?.invoke()
            true // Consume and drop obscured event, blocking pass-through to interactive target
        } else {
            false // Allow standard un-obscured touch processing
        }
    }
}

/**
 * Helper function to evaluate whether a MotionEvent flag indicates an obscured touch.
 */
fun isTouchObscured(flags: Int): Boolean {
    val dangerous = MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED
    return (flags and dangerous) != 0
}
