export interface ComponentTelemetry {
  architecture: string;
  material: string;
  busInterface: string;
  powerDraw: string;
  operatingFreq: string;
  thermalEnvelope: string;
  engineeringNotes: string;
}

export interface TeardownAnnotation {
  id: string;
  name: string;
  tag: string;
  category: 'SILICON' | 'OPTICS' | 'POWER' | 'THERMAL';
  anchorX: number; // Normalized X [0..1]
  anchorY: number; // Normalized Y [0..1]
  startProgress: number; // [0..1]
  endProgress: number;   // [0..1]
  peakProgress: number;  // [0..1]
  leaderLineDx: number;
  leaderLineDy: number;
  telemetry: ComponentTelemetry;
}

export interface TeardownMilestone {
  id: string;
  title: string;
  description: string;
  startProgress: number;
  endProgress: number;
  accentTag: string;
}

export const TEARDOWN_MILESTONES: TeardownMilestone[] = [
  {
    id: 'assembled',
    title: 'Assembled Device',
    description: 'Factory sealed chassis with back cover and display intact.',
    startProgress: 0.0,
    endProgress: 0.22,
    accentTag: 'CHASSIS',
  },
  {
    id: 'back_cover_removed',
    title: 'Back Cover Removed',
    description: 'Wireless charging coil, NFC antenna, and battery cell exposed.',
    startProgress: 0.22,
    endProgress: 0.48,
    accentTag: 'COIL & POWER',
  },
  {
    id: 'exploded_layers',
    title: 'Exploded Layers',
    description: 'Motherboard, optics array, and cooling floating in 3D space.',
    startProgress: 0.48,
    endProgress: 0.75,
    accentTag: 'MOTHERBOARD',
  },
  {
    id: 'chassis_focus',
    title: 'Silicon & Chassis',
    description: 'Detailed substrate separation and silicon processor die focus.',
    startProgress: 0.75,
    endProgress: 1.0,
    accentTag: 'SILICON',
  },
];

export const TEARDOWN_ANNOTATIONS: TeardownAnnotation[] = [
  {
    id: 'soc_processor',
    name: 'Silicon SoC Die',
    tag: '4nm Octa-Core Processor',
    category: 'SILICON',
    anchorX: 0.46,
    anchorY: 0.32,
    startProgress: 0.72,
    endProgress: 1.0,
    peakProgress: 0.92,
    leaderLineDx: 0.16,
    leaderLineDy: -0.08,
    telemetry: {
      architecture: 'ARMv9.2-A: 1x Cortex-X4 @ 3.3GHz + 5x Cortex-A720 + 2x Cortex-A520',
      material: 'Monolithic 4nm FinFET Die on PoP LPDDR5X DRAM Stack',
      busInterface: 'High-Speed AXI / PCIe Gen 4 x4 System Interconnect',
      powerDraw: '3.5W Nominal TDP / 12W Short-Burst Peak',
      operatingFreq: 'Up to 3.3 GHz Prime Core / 900 MHz GPU',
      thermalEnvelope: 'Phase-Change TIM Paste directly bonded to Vapor Chamber',
      engineeringNotes: 'Primary target for Silicon & Kernel Stack. Houses hardware security modules and CPU privilege rings.',
    },
  },
  {
    id: 'camera_array',
    name: 'Camera System',
    tag: '50MP OIS + 12MP Ultra-Wide',
    category: 'OPTICS',
    anchorX: 0.24,
    anchorY: 0.42,
    startProgress: 0.70,
    endProgress: 1.0,
    peakProgress: 0.88,
    leaderLineDx: 0.14,
    leaderLineDy: -0.06,
    telemetry: {
      architecture: 'Triple Optical Module with Ball-Bearing Voice Coil Actuators',
      material: 'Machined Anodized 6000-Series Aluminum + Sapphire Glass',
      busInterface: '4-Lane MIPI CSI-2 (2.5 Gbps/lane bandwidth)',
      powerDraw: '1.8W Peak Multi-Sensor Synchronous Capture',
      operatingFreq: 'Dual Spectral AF Sensing @ 120Hz',
      thermalEnvelope: 'Direct Copper Heatpipe Coupling to Motherboard Shield',
      engineeringNotes: 'Dedicated 4-axis hardware gyroscope drives real-time voice coil magnet positioning for optical image stabilization.',
    },
  },
  {
    id: 'qi_coil',
    name: 'Wireless Charging Coil',
    tag: '15W Qi Fast Induction',
    category: 'POWER',
    anchorX: 0.48,
    anchorY: 0.46,
    startProgress: 0.24,
    endProgress: 0.88,
    peakProgress: 0.42,
    leaderLineDx: 0.16,
    leaderLineDy: -0.05,
    telemetry: {
      architecture: 'Planar Multi-Strand Litz Wire Coil Array',
      material: 'Oxygen-Free High Thermal Conductivity Copper (OFHC)',
      busInterface: 'I2C Dedicated PMIC Telemetry Bus (100kHz)',
      powerDraw: '15W RX Induction / 5W Reverse Wireless TX',
      operatingFreq: '110 kHz to 205 kHz Resonant Band',
      thermalEnvelope: '38°C Active Thermal Throttle Ceiling',
      engineeringNotes: 'High-permeability sintered ferrite sheet prevents magnetic eddy current coupling into the battery casing.',
    },
  },
  {
    id: 'battery_pack',
    name: 'Battery Cell',
    tag: '5000 mAh Dual-Cell',
    category: 'POWER',
    anchorX: 0.50,
    anchorY: 0.68,
    startProgress: 0.25,
    endProgress: 1.0,
    peakProgress: 0.55,
    leaderLineDx: -0.16,
    leaderLineDy: -0.06,
    telemetry: {
      architecture: 'Dual-Cell Series Configuration with Dynamic Resistance Balancing',
      material: 'Silicon-Carbon High-Density Anode + Polymer Electrolyte',
      busInterface: 'HDQ / I2C Coulomb Counter Fuel Gauge',
      powerDraw: 'Up to 65W High-Current Injection',
      operatingFreq: '100 kHz Impedance Track Monitoring',
      thermalEnvelope: 'Dual NTC Thermistors (Cutoff: 45°C charge / 60°C discharge)',
      engineeringNotes: 'Laser-welded copper tab terminals minimize ESR impedance to mitigate thermal buildup during rapid charge cycles.',
    },
  },
  {
    id: 'vapor_chamber',
    name: 'Vapor Chamber',
    tag: '3D Titanium-Copper VC',
    category: 'THERMAL',
    anchorX: 0.66,
    anchorY: 0.54,
    startProgress: 0.70,
    endProgress: 1.0,
    peakProgress: 0.82,
    leaderLineDx: -0.15,
    leaderLineDy: -0.06,
    telemetry: {
      architecture: '0.4mm Ultra-Thin Sealed Two-Phase Thermal Spreader',
      material: 'Oxygen-Free Copper with Sintered Powder Capillary Wick Structure',
      busInterface: 'Thermodynamic Passive Capillary Loop',
      powerDraw: '0W (Zero-Power Passive Phase Change)',
      operatingFreq: 'Continuous Thermodynamic Vapor-Liquid Cycle',
      thermalEnvelope: 'Dissipates up to 15W Across 4,200 mm² Spreader Area',
      engineeringNotes: 'Deionized ultra-pure water working fluid vaporizes at hot spots, migrates across chassis, condenses, and returns via capillary action.',
    },
  },
];

// Pre-mapped required static frame assets (00 to 39)
export const TEARDOWN_FRAME_ASSETS = [
  require('../../assets/teardown_frames/frame_00.webp'),
  require('../../assets/teardown_frames/frame_01.webp'),
  require('../../assets/teardown_frames/frame_02.webp'),
  require('../../assets/teardown_frames/frame_03.webp'),
  require('../../assets/teardown_frames/frame_04.webp'),
  require('../../assets/teardown_frames/frame_05.webp'),
  require('../../assets/teardown_frames/frame_06.webp'),
  require('../../assets/teardown_frames/frame_07.webp'),
  require('../../assets/teardown_frames/frame_08.webp'),
  require('../../assets/teardown_frames/frame_09.webp'),
  require('../../assets/teardown_frames/frame_10.webp'),
  require('../../assets/teardown_frames/frame_11.webp'),
  require('../../assets/teardown_frames/frame_12.webp'),
  require('../../assets/teardown_frames/frame_13.webp'),
  require('../../assets/teardown_frames/frame_14.webp'),
  require('../../assets/teardown_frames/frame_15.webp'),
  require('../../assets/teardown_frames/frame_16.webp'),
  require('../../assets/teardown_frames/frame_17.webp'),
  require('../../assets/teardown_frames/frame_18.webp'),
  require('../../assets/teardown_frames/frame_19.webp'),
  require('../../assets/teardown_frames/frame_20.webp'),
  require('../../assets/teardown_frames/frame_21.webp'),
  require('../../assets/teardown_frames/frame_22.webp'),
  require('../../assets/teardown_frames/frame_23.webp'),
  require('../../assets/teardown_frames/frame_24.webp'),
  require('../../assets/teardown_frames/frame_25.webp'),
  require('../../assets/teardown_frames/frame_26.webp'),
  require('../../assets/teardown_frames/frame_27.webp'),
  require('../../assets/teardown_frames/frame_28.webp'),
  require('../../assets/teardown_frames/frame_29.webp'),
  require('../../assets/teardown_frames/frame_30.webp'),
  require('../../assets/teardown_frames/frame_31.webp'),
  require('../../assets/teardown_frames/frame_32.webp'),
  require('../../assets/teardown_frames/frame_33.webp'),
  require('../../assets/teardown_frames/frame_34.webp'),
  require('../../assets/teardown_frames/frame_35.webp'),
  require('../../assets/teardown_frames/frame_36.webp'),
  require('../../assets/teardown_frames/frame_37.webp'),
  require('../../assets/teardown_frames/frame_38.webp'),
  require('../../assets/teardown_frames/frame_39.webp'),
];

export const TOTAL_FRAMES = TEARDOWN_FRAME_ASSETS.length;

export function getMilestoneForProgress(progress: number): TeardownMilestone {
  const p = Math.max(0, Math.min(1, progress));
  for (const m of TEARDOWN_MILESTONES) {
    if (p >= m.startProgress && p <= m.endProgress) {
      return m;
    }
  }
  return TEARDOWN_MILESTONES[TEARDOWN_MILESTONES.length - 1];
}
