package com.kernelcraft.feature.teardown.annotation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * Architectural telemetry specifications for an individual hardware component.
 */
@Immutable
data class ComponentTelemetry(
    val architecture: String,
    val material: String,
    val busInterface: String,
    val powerDraw: String,
    val operatingFreq: String,
    val thermalEnvelope: String,
    val engineeringNotes: String
)

/**
 * Model representing an interactive spatial pin point anchored to a hardware component.
 *
 * @property id Unique identifier for the component.
 * @property name Human-readable component title (e.g. "Triple Camera Array").
 * @property tag Short highlight specification tag (e.g. "50MP OIS / Periscope").
 * @property category Component classification badge.
 * @property anchorX Normalized X coordinate within 9:16 video frame [0.0f..1.0f].
 * @property anchorY Normalized Y coordinate within 9:16 video frame [0.0f..1.0f].
 * @property startProgress Progress [0.0f..1.0f] when component begins separating/appearing.
 * @property endProgress Progress [0.0f..1.0f] when component collapses or gets occluded.
 * @property peakProgress Optimal progress where component is in sharp focus / maximum separation.
 * @property leaderLineDx Normalized delta X for leader line elbow offset.
 * @property leaderLineDy Normalized delta Y for leader line elbow offset.
 * @property telemetry In-depth technical specifications displayed in expansion card.
 */
@Immutable
data class TeardownAnnotation(
    val id: String,
    val name: String,
    val tag: String,
    val category: String,
    val anchorX: Float,
    val anchorY: Float,
    val startProgress: Float,
    val endProgress: Float,
    val peakProgress: Float,
    val leaderLineDx: Float = 0.12f,
    val leaderLineDy: Float = -0.06f,
    val telemetry: ComponentTelemetry
) {
    /**
     * Calculates the visibility weight [0.0f..1.0f] based on current scrub progress.
     * Uses Hermite cubic smoothstep interpolation for organic fade-in and fade-out ramps.
     */
    fun computeVisibilityWeight(progress: Float): Float {
        if (progress < startProgress || progress > endProgress) return 0f

        val rampInDuration = 0.04f
        val rampOutDuration = 0.04f

        return when {
            progress < startProgress + rampInDuration -> {
                val t = ((progress - startProgress) / rampInDuration).coerceIn(0f, 1f)
                t * t * (3f - 2f * t) // smoothstep in
            }
            progress > endProgress - rampOutDuration -> {
                val t = ((endProgress - progress) / rampOutDuration).coerceIn(0f, 1f)
                t * t * (3f - 2f * t) // smoothstep out
            }
            else -> 1f
        }
    }
}

/**
 * Calculated dimensions of the 9:16 video frame within the host Compose container.
 * Accommodates letterboxing/pillarboxing regardless of screen aspect ratio.
 */
@Immutable
data class VideoViewportBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun toScreenPoint(normalizedX: Float, normalizedY: Float): Offset {
        return Offset(
            x = left + normalizedX * width,
            y = top + normalizedY * height
        )
    }
}

/**
 * Calculates exact fit bounds for a 9:16 aspect ratio video surface.
 */
fun calculateFitBounds(
    containerWidth: Float,
    containerHeight: Float,
    videoAspect: Float = 9f / 16f
): VideoViewportBounds {
    if (containerWidth <= 0f || containerHeight <= 0f) {
        return VideoViewportBounds(0f, 0f, containerWidth, containerHeight)
    }
    val containerAspect = containerWidth / containerHeight
    return if (containerAspect > videoAspect) {
        // Container is wider than 9:16 -> pillarbox left/right
        val videoWidth = containerHeight * videoAspect
        val left = (containerWidth - videoWidth) / 2f
        VideoViewportBounds(left, 0f, videoWidth, containerHeight)
    } else {
        // Container is taller than 9:16 -> letterbox top/bottom
        val videoHeight = containerWidth / videoAspect
        val top = (containerHeight - videoHeight) / 2f
        VideoViewportBounds(0f, top, containerWidth, videoHeight)
    }
}

/**
 * Pre-calibrated hardware annotations mapped to the teardown footage timeline and spatial layout.
 */
object DefaultTeardownAnnotations {
    val annotations = listOf(
        TeardownAnnotation(
            id = "soc_processor",
            name = "Silicon SoC Die",
            tag = "4nm Octa-Core Processor",
            category = "SILICON",
            anchorX = 0.45f,
            anchorY = 0.30f,
            startProgress = 0.75f,
            endProgress = 1.00f,
            peakProgress = 0.92f,
            leaderLineDx = 0.12f,
            leaderLineDy = -0.06f,
            telemetry = ComponentTelemetry(
                architecture = "ARMv9.2-A: 1x Cortex-X4 @ 3.3GHz + 5x Cortex-A720 + 2x Cortex-A520",
                material = "Monolithic 4nm FinFET Die on PoP LPDDR5X DRAM Stack",
                busInterface = "High-Speed AXI / PCIe Gen 4 x4 System Interconnect",
                powerDraw = "3.5W Nominal TDP / 12W Short-Burst Peak",
                operatingFreq = "Up to 3.3 GHz Prime Core / 900 MHz GPU",
                thermalEnvelope = "Phase-Change TIM Paste directly bonded to Vapor Chamber",
                engineeringNotes = "Primary target for Silicon & Kernel Stack. Houses hardware security modules and CPU privilege rings."
            )
        ),
        TeardownAnnotation(
            id = "camera_array",
            name = "Camera System",
            tag = "50MP OIS + 12MP Ultra-Wide",
            category = "OPTICS",
            anchorX = 0.20f,
            anchorY = 0.42f,
            startProgress = 0.75f,
            endProgress = 1.00f,
            peakProgress = 0.88f,
            leaderLineDx = 0.12f,
            leaderLineDy = -0.05f,
            telemetry = ComponentTelemetry(
                architecture = "Triple Optical Module with Ball-Bearing Voice Coil Actuators",
                material = "Machined Anodized 6000-Series Aluminum + Sapphire Glass",
                busInterface = "4-Lane MIPI CSI-2 (2.5 Gbps/lane bandwidth)",
                powerDraw = "1.8W Peak Multi-Sensor Synchronous Capture",
                operatingFreq = "Dual Spectral AF Sensing @ 120Hz",
                thermalEnvelope = "Direct Copper Heatpipe Coupling to Motherboard Shield",
                engineeringNotes = "Dedicated 4-axis hardware gyroscope drives real-time voice coil magnet positioning for optical image stabilization."
            )
        ),
        TeardownAnnotation(
            id = "qi_coil",
            name = "Wireless Charging Coil",
            tag = "15W Qi Fast Induction",
            category = "POWER",
            anchorX = 0.45f,
            anchorY = 0.45f,
            startProgress = 0.75f,
            endProgress = 1.00f,
            peakProgress = 0.86f,
            leaderLineDx = 0.12f,
            leaderLineDy = -0.05f,
            telemetry = ComponentTelemetry(
                architecture = "Planar Multi-Strand Litz Wire Coil Array",
                material = "Oxygen-Free High Thermal Conductivity Copper (OFHC)",
                busInterface = "I2C Dedicated PMIC Telemetry Bus (100kHz)",
                powerDraw = "15W RX Induction / 5W Reverse Wireless TX",
                operatingFreq = "110 kHz to 205 kHz Resonant Band",
                thermalEnvelope = "38°C Active Thermal Throttle Ceiling",
                engineeringNotes = "High-permeability sintered ferrite sheet prevents magnetic eddy current coupling into the battery casing."
            )
        ),
        TeardownAnnotation(
            id = "battery_pack",
            name = "Battery Cell",
            tag = "5000 mAh Dual-Cell",
            category = "POWER",
            anchorX = 0.48f,
            anchorY = 0.69f,
            startProgress = 0.75f,
            endProgress = 1.00f,
            peakProgress = 0.84f,
            leaderLineDx = -0.12f,
            leaderLineDy = -0.05f,
            telemetry = ComponentTelemetry(
                architecture = "Dual-Cell Series Configuration with Dynamic Resistance Balancing",
                material = "Silicon-Carbon High-Density Anode + Polymer Electrolyte",
                busInterface = "HDQ / I2C Coulomb Counter Fuel Gauge",
                powerDraw = "Up to 65W High-Current Injection",
                operatingFreq = "100 kHz Impedance Track Monitoring",
                thermalEnvelope = "Dual NTC Thermistors (Cutoff: 45°C charge / 60°C discharge)",
                engineeringNotes = "Laser-welded copper tab terminals minimize ESR impedance to mitigate thermal buildup during rapid charge cycles."
            )
        ),
        TeardownAnnotation(
            id = "vapor_chamber",
            name = "Vapor Chamber",
            tag = "3D Titanium-Copper VC",
            category = "THERMAL",
            anchorX = 0.65f,
            anchorY = 0.55f,
            startProgress = 0.75f,
            endProgress = 1.00f,
            peakProgress = 0.82f,
            leaderLineDx = -0.12f,
            leaderLineDy = -0.05f,
            telemetry = ComponentTelemetry(
                architecture = "0.4mm Ultra-Thin Sealed Two-Phase Thermal Spreader",
                material = "Oxygen-Free Copper with Sintered Powder Capillary Wick Structure",
                busInterface = "Thermodynamic Passive Capillary Loop",
                powerDraw = "0W (Zero-Power Passive Phase Change)",
                operatingFreq = "Continuous Thermodynamic Vapor-Liquid Cycle",
                thermalEnvelope = "Dissipates up to 15W Across 4,200 mm² Spreader Area",
                engineeringNotes = "Deionized ultra-pure water working fluid vaporizes at hot spots, migrates across chassis, condenses, and returns via capillary action."
            )
        )
    )
}
