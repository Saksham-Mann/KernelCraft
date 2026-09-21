package com.kernelcraft.feature.teardown

/**
 * Key physical milestones along the smartphone teardown timeline.
 *
 * Each milestone represents a distinct architectural teardown chapter with precise
 * timestamp bounds (in milliseconds) and descriptive metadata for UI overlays.
 *
 * @property title Human-readable section title.
 * @property description Educational narrative detailing what hardware is exposed.
 * @property startMs Start timestamp in milliseconds (inclusive).
 * @property endMs End timestamp in milliseconds (exclusive).
 * @property accentTag Short hardware component badge (e.g. "CHASSIS", "BATTERY").
 */
enum class TeardownMilestone(
    val title: String,
    val description: String,
    val startMs: Long,
    val endMs: Long,
    val accentTag: String
) {
    ASSEMBLED(
        title = "Assembled Device",
        description = "Factory sealed chassis with back cover intact.",
        startMs = 0L,
        endMs = 2_200L,
        accentTag = "CHASSIS"
    ),
    BACK_COVER_REMOVED(
        title = "Back Cover Removed",
        description = "Wireless charging coil, NFC antenna, and battery cell exposed.",
        startMs = 2_200L,
        endMs = 4_800L,
        accentTag = "COIL & POWER"
    ),
    EXPLODED_LAYERS(
        title = "Exploded Layers",
        description = "Motherboard, optics array, and cooling floating in 3D space.",
        startMs = 4_800L,
        endMs = 7_500L,
        accentTag = "MOTHERBOARD"
    ),
    CHASSIS_FOCUS(
        title = "Silicon & Chassis",
        description = "Detailed substrate separation and silicon processor die focus.",
        startMs = 7_500L,
        endMs = Long.MAX_VALUE,
        accentTag = "SILICON"
    );

    /**
     * Checks whether the given playback timestamp falls within this milestone.
     */
    fun contains(positionMs: Long): Boolean = positionMs in startMs until endMs

    companion object {
        /**
         * Boundaries where milestone transitions occur.
         * Used for haptic tick triggering.
         */
        val BOUNDARY_THRESHOLDS_MS = listOf(2_200L, 4_800L, 7_500L)

        /**
         * Key hardware highlight timestamp within EXPLODED_LAYERS (peak layer explosion).
         */
        const val PEAK_EXPLOSION_MS = 8_000L

        /**
         * Resolves the active milestone for a given video position in milliseconds.
         */
        fun fromPositionMs(positionMs: Long): TeardownMilestone = when {
            positionMs < 2_200L -> ASSEMBLED
            positionMs < 4_800L -> BACK_COVER_REMOVED
            positionMs < 7_500L -> EXPLODED_LAYERS
            else -> CHASSIS_FOCUS
        }

        /**
         * Resolves the active milestone from a normalized progress [0f, 1f] against total duration.
         */
        fun fromProgress(progress: Float, durationMs: Long): TeardownMilestone {
            val positionMs = (progress * durationMs).toLong()
            return fromPositionMs(positionMs)
        }
    }
}
