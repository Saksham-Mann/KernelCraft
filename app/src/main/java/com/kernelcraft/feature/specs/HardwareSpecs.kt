package com.kernelcraft.feature.specs

import androidx.compose.runtime.Immutable

/**
 * Single data row specification entry.
 */
@Immutable
data class SpecItem(
    val label: String,
    val value: String,
    val badge: String? = null,
    val detail: String? = null
)

/**
 * Architectural specification category.
 */
@Immutable
data class SpecCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val accentTag: String,
    val iconSymbol: String,
    val specs: List<SpecItem>
)

/**
 * High-level summary metric chip displayed in the sheet's peek state.
 */
@Immutable
data class SummaryChip(
    val id: String,
    val title: String,
    val metric: String,
    val iconSymbol: String
)

/**
 * Hardware architecture specifications matching Techspec.md and teardown assets.
 */
object HardwareSpecsData {

    val summaryChips = listOf(
        SummaryChip(
            id = "silicon",
            title = "Core SoC",
            metric = "4nm GAA",
            iconSymbol = "SOC"
        ),
        SummaryChip(
            id = "thermal",
            title = "Cooling",
            metric = "4,200 mm²",
            iconSymbol = "THM"
        ),
        SummaryChip(
            id = "power",
            title = "Battery",
            metric = "5000 mAh",
            iconSymbol = "PWR"
        ),
        SummaryChip(
            id = "optics",
            title = "Camera",
            metric = "50MP OIS",
            iconSymbol = "CAM"
        )
    )

    val categories = listOf(
        SpecCategory(
            id = "silicon",
            title = "Core Silicon Architecture",
            subtitle = "Application Processor & Neural Compute",
            accentTag = "COMPUTE",
            iconSymbol = "SOC",
            specs = listOf(
                SpecItem(
                    label = "System on Chip",
                    value = "Octa-Core Heterogeneous SoC",
                    badge = "ARMv9.2-A",
                    detail = "Tri-cluster: 1x Cortex-X4 @ 3.3GHz, 5x Cortex-A720 @ 3.15GHz, 2x Cortex-A520 @ 2.27GHz"
                ),
                SpecItem(
                    label = "Process Geometry",
                    value = "4nm FinFET Lithography",
                    badge = "4nm",
                    detail = "Extreme ultraviolet (EUV) patterning with 300mm wafer density"
                ),
                SpecItem(
                    label = "Transistor Density",
                    value = "~160 Million Transistors / mm²",
                    badge = "160M/mm²",
                    detail = "Monolithic die packaging with direct micro-bump interconnects"
                ),
                SpecItem(
                    label = "Graphics Processing Unit",
                    value = "Hardware Ray-Tracing GPU",
                    badge = "Ray Tracing",
                    detail = "Vulkan 1.3 / OpenGL ES 3.2 support with unified shader architecture"
                ),
                SpecItem(
                    label = "Neural Processing Unit",
                    value = "45 TOPS Hexagon Engine",
                    badge = "45 TOPS",
                    detail = "Dedicated INT4/INT8/FP16 tensor accelerator for on-device AI models"
                ),
                SpecItem(
                    label = "System Memory",
                    value = "12GB / 16GB LPDDR5X",
                    badge = "8533 Mbps",
                    detail = "Package-on-Package (PoP) substrate stacked directly above CPU core array"
                )
            )
        ),
        SpecCategory(
            id = "thermal_power",
            title = "Power & Thermal Subsystem",
            subtitle = "Heat Dissipation & Energy Storage",
            accentTag = "THERMAL & POWER",
            iconSymbol = "THM",
            specs = listOf(
                SpecItem(
                    label = "Vapor Chamber",
                    value = "4,200 mm² 3D Titanium-Copper VC",
                    badge = "4,200 mm²",
                    detail = "Two-phase phase-change cooling with sintered capillary wick structure"
                ),
                SpecItem(
                    label = "Thermal Interface Material",
                    value = "High-Thermal Pyrolytic Graphite",
                    badge = "TIM Paste",
                    detail = "Coupled directly to aluminum midframe to eliminate localized hotspots"
                ),
                SpecItem(
                    label = "Battery Chemistry",
                    value = "Dual-Cell Silicon-Carbon",
                    badge = "5000 mAh",
                    detail = "High energy density (750 Wh/L) with laser-welded low-ESR copper busbars"
                ),
                SpecItem(
                    label = "Wired Injection",
                    value = "65W High-Current Charging",
                    badge = "65W USB-PD",
                    detail = "Direct charge pump bypasses internal PMIC converter to reduce heat"
                ),
                SpecItem(
                    label = "Wireless Induction",
                    value = "15W Qi Resonant Array",
                    badge = "15W Qi",
                    detail = "Multi-strand Litz wire coil with high-permeability sintered ferrite shield"
                ),
                SpecItem(
                    label = "Thermal Safety Monitoring",
                    value = "Dual NTC Temperature Cutoffs",
                    badge = "Dual NTC",
                    detail = "Real-time thermal throttling triggers at 45°C during rapid charge cycles"
                )
            )
        ),
        SpecCategory(
            id = "optics",
            title = "Optics & Imaging Engine",
            subtitle = "Sensors, Actuators & ISP Pipelines",
            accentTag = "OPTICS",
            iconSymbol = "CAM",
            specs = listOf(
                SpecItem(
                    label = "Primary Sensor",
                    value = "50MP 1/1.3\" Custom CMOS",
                    badge = "50MP",
                    detail = "1.2μm native pixel pitch, 2.4μm 4-in-1 Quad Bayer binning configuration"
                ),
                SpecItem(
                    label = "Optical Stabilization",
                    value = "4-Axis Voice Coil Motor (VCM)",
                    badge = "OIS ±1.5°",
                    detail = "Ball-bearing suspension with dedicated hardware gyro sensing at 1000Hz"
                ),
                SpecItem(
                    label = "Primary Optics",
                    value = "7-Element (1G + 6P) Aspheric Lens",
                    badge = "f/1.68",
                    detail = "Nano-dielectric coating with anti-reflective fluorite treatment"
                ),
                SpecItem(
                    label = "Ultra-Wide Angle",
                    value = "12MP 120° Panoramic Field",
                    badge = "120° FOV",
                    detail = "Dual Pixel autofocus with integrated 2.5cm macro photography mode"
                ),
                SpecItem(
                    label = "Periscope Telephoto",
                    value = "10MP 3x/5x Folded Optics",
                    badge = "5x Optical",
                    detail = "Dual-prism optical reflection path with independent OIS compensation"
                ),
                SpecItem(
                    label = "Image Signal Processor",
                    value = "18-Bit Cognitive ISP Pipeline",
                    badge = "3.2 GP/s",
                    detail = "Zero shutter lag (ZSL) multi-frame raw alignment in hardware"
                )
            )
        ),
        SpecCategory(
            id = "structure",
            title = "Structural Materials & Chassis",
            subtitle = "Mechanical Tolerances & Ingress Seals",
            accentTag = "MATERIALS",
            iconSymbol = "MAT",
            specs = listOf(
                SpecItem(
                    label = "Chassis Frame",
                    value = "6000-Series Armor Aluminum",
                    badge = "Armor Al",
                    detail = "CNC milled monocoque architecture with anodized scratch-resistant coating"
                ),
                SpecItem(
                    label = "Machining Precision",
                    value = "CNC Micro-Toleranced Internal Cavity",
                    badge = "±0.02 mm",
                    detail = "High-precision pocket milling for snug component isolation and thermal coupling"
                ),
                SpecItem(
                    label = "Surface Protection",
                    value = "Corning Gorilla Glass Victus 2",
                    badge = "Victus 2",
                    detail = "Aluminosilicate chemical strengthening on front display and rear chassis"
                ),
                SpecItem(
                    label = "Camera Island Cover",
                    value = "Polished Sapphire Crystal",
                    badge = "Mohs 9",
                    detail = "Diamond-turned bevel edges with supreme scratch and abrasion resistance"
                ),
                SpecItem(
                    label = "Ingress Rating",
                    value = "IP68 Submersion Resistance",
                    badge = "IP68",
                    detail = "Continuous perimeter liquid silicone gasket with acoustic Gore-Tex pressure vents"
                ),
                SpecItem(
                    label = "Tactile Engine",
                    value = "0815 X-Axis Linear Transducer",
                    badge = "X-Axis Haptic",
                    detail = "Wideband resonance delivering crisp clicks, ticks, and texture vibrations"
                )
            )
        )
    )
}
