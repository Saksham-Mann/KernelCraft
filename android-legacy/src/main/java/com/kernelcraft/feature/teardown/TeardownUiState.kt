package com.kernelcraft.feature.teardown

import androidx.compose.runtime.Immutable

/**
 * Immutable UI State representing the interactive hardware teardown viewport.
 *
 * Adheres to MVI-lite architecture guidelines from Schema.md and Rules.md.
 * All properties are primitive or stable immutable objects to ensure Compose stability inference.
 *
 * @property trackId Identifier of the current hardware track.
 * @property scrollProgress Normalized scrub progress strictly clamped within [0.0f, 1.0f].
 * @property currentPositionMs Current video playback position in milliseconds.
 * @property durationMs Total duration of the video in milliseconds.
 * @property currentMilestone Active physical stage of the teardown.
 * @property scrubVelocity Current scrub velocity in normalized progress-units per second.
 * @property isDragging True if user's thumb is actively in contact with the viewport.
 * @property isCoasting True if inertia decay animation is running after a flick gesture.
 * @property showDiveInCta True when progress crosses >= 0.90f to prompt the Dive-In transition.
 * @property selectedComponentId Component ID currently highlighted or open in telemetry view.
 * @property isSpecsExpanded True if technical specifications sheet is presented.
 * @property isHapticsEnabled User preference for tactile feedback ticks.
 * @property showTutorialCoachMark True on first launch until the user performs their first drag.
 * @property isReassembling True while the reassemble spring rewind animation is active.
 */
@Immutable
data class TeardownUiState(
    val trackId: String = "chip01",
    val scrollProgress: Float = 0.0f,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 10_000L,
    val currentMilestone: TeardownMilestone = TeardownMilestone.ASSEMBLED,
    val scrubVelocity: Float = 0.0f,
    val isDragging: Boolean = false,
    val isCoasting: Boolean = false,
    val showDiveInCta: Boolean = false,
    val selectedComponentId: String? = null,
    val isSpecsExpanded: Boolean = false,
    val isHapticsEnabled: Boolean = true,
    val isAudioEnabled: Boolean = true,
    val showTutorialCoachMark: Boolean = true,
    val isReassembling: Boolean = false
) {
    /** True whenever the user is interacting either via direct drag or decay coasting */
    val isScrubbing: Boolean get() = isDragging || isCoasting

    /** Formats milliseconds to "MM:SS.ms" display string */
    val formattedTimestamp: String
        get() {
            val totalSeconds = currentPositionMs / 1000
            val fractionHundredths = (currentPositionMs % 1000) / 10
            return String.format("%02d.%02d", totalSeconds, fractionHundredths)
        }
}

/**
 * User-intent events dispatched from the UI to [TeardownViewModel].
 */
sealed interface TeardownEvent {
    data class ScrubUpdated(
        val progress: Float,
        val positionMs: Long,
        val velocity: Float,
        val isDragging: Boolean,
        val isCoasting: Boolean
    ) : TeardownEvent

    data class DurationResolved(val durationMs: Long) : TeardownEvent

    data class SelectComponent(val componentId: String?) : TeardownEvent

    data class SetSpecsExpanded(val isExpanded: Boolean) : TeardownEvent

    data object ToggleHaptics : TeardownEvent

    data object ToggleAudio : TeardownEvent

    data object DismissTutorial : TeardownEvent

    data object ReassemblePhone : TeardownEvent

    data object DiveInTriggered : TeardownEvent
}
