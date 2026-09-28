export interface ComponentTelemetry {
  technicalName: string;
  subtitle: string;
  summary: string;
  architecture: string;
  interfaceSpec: string;
  keyFeatures: string[];
}

export interface TeardownAnnotation {
  id: string;
  name: string;
  tag: string;
  category: 'POWER' | 'OPTICS' | 'BRAIN' | 'MEMORY' | 'DISPLAY';
  icon: string;
  accentColor: string;
  anchorX: number; // Normalized X [0..1]
  anchorY: number; // Normalized Y [0..1]
  startProgress: number; // [0..1]
  endProgress: number;   // [0..1]
  telemetry: ComponentTelemetry;
}

export interface TeardownMilestone {
  id: string;
  title: string;
  description: string;
  startProgress: number;
  endProgress: number;
  accentTag: string;
  emoji: string;
}

export const TEARDOWN_MILESTONES: TeardownMilestone[] = [
  {
    id: 'closed_back',
    title: 'Assembled Device',
    description: 'Hermetically sealed chassis protecting internal logic, optical array, and power substrate.',
    startProgress: 0.0,
    endProgress: 0.22,
    accentTag: 'CHASSIS',
    emoji: '📱',
  },
  {
    id: 'exploded_view',
    title: 'Disassembled Architecture',
    description: 'Substrates floating in 3D: High-density battery, CMOS sensor array, and LPDDR5 SoC logic board.',
    startProgress: 0.22,
    endProgress: 0.70,
    accentTag: 'TEARDOWN',
    emoji: '🔬',
  },
  {
    id: 'snapping_back',
    title: 'Precision Reassembly',
    description: 'Components locking back into the aluminum mid-frame and heat dissipation envelope.',
    startProgress: 0.70,
    endProgress: 0.86,
    accentTag: 'ASSEMBLY',
    emoji: '⚙️',
  },
  {
    id: 'front_screen',
    title: 'Front Display Module',
    description: 'Dynamic AMOLED panel facing forward, ready to transition into the OS runtime software layer.',
    startProgress: 0.86,
    endProgress: 1.0,
    accentTag: 'RUNNING OS',
    emoji: '💡',
  },
];

export const TEARDOWN_ANNOTATIONS: TeardownAnnotation[] = [
  {
    id: 'battery_pack',
    name: 'Lithium-Ion Power Cell',
    tag: 'Power Cell',
    category: 'POWER',
    icon: 'battery-charging',
    accentColor: '#34C759',
    anchorX: 0.48,
    anchorY: 0.65,
    startProgress: 0.22,
    endProgress: 0.72,
    telemetry: {
      technicalName: 'Lithium-Ion Power Cell',
      subtitle: 'Chemical Energy Storage & Power Management IC (PMIC)',
      summary: 'Dual-cell high energy density lithium-polymer battery coupled with a fast-switching buck-boost Power Management IC (PMIC) distributing stabilized voltage rails to logic substrates.',
      architecture: 'Series Dual-Cell Li-Po with Integrated Gas Gauge Sensor',
      interfaceSpec: 'I2C / SMBus Coulomb Counter with Over-Current Protection',
      keyFeatures: [
        '5,000 mAh high-capacity chemical storage',
        'Direct multi-rail PMIC distribution (1.8V, 3.3V, 0.9V core)',
        'Thermal sensing NTC thermistors with auto-throttling safety cutoffs',
      ],
    },
  },
  {
    id: 'camera_array',
    name: 'CMOS Sensor & ISP',
    tag: 'CMOS & ISP',
    category: 'OPTICS',
    icon: 'camera',
    accentColor: '#007AFF',
    anchorX: 0.24,
    anchorY: 0.38,
    startProgress: 0.22,
    endProgress: 0.72,
    telemetry: {
      technicalName: 'CMOS Sensor & ISP',
      subtitle: 'Multi-lens optical array & Image Signal Processor',
      summary: 'High-resolution back-illuminated (BSI) CMOS active-pixel sensor coupled to high-bandwidth MIPI CSI-2 serial data lanes feeding hardware Image Signal Processor (ISP) pipelines.',
      architecture: 'Multi-Lens 50MP BSI Optical Array with Voice-Coil Actuators (OIS)',
      interfaceSpec: '4-Lane MIPI CSI-2 (up to 2.5 Gbps per physical differential lane)',
      keyFeatures: [
        'Sub-micron quad-bayer photodiodes with phase detection autofocus',
        'Zero-shutter-lag real-time hardware demosaicing and noise reduction',
        'Direct DMA streaming into shared unified memory buffers for GPU consumption',
      ],
    },
  },
  {
    id: 'cpu_brain',
    name: 'SoC & Logic Board',
    tag: 'SoC & Logic',
    category: 'BRAIN',
    icon: 'hardware-chip',
    accentColor: '#FF9500',
    anchorX: 0.44,
    anchorY: 0.26,
    startProgress: 0.25,
    endProgress: 0.72,
    telemetry: {
      technicalName: 'SoC & Logic Board',
      subtitle: 'Central Processing Unit, GPU & Neural Engine',
      summary: 'Monolithic silicon System-on-Chip (SoC) fabricated on leading-edge FinFET lithography. Integrates multi-cluster ARMv9 CPU cores, multi-core GPU, and dedicated Neural Processing Unit (NPU).',
      architecture: 'ARMv9.2-A Tri-Cluster (Prime + Performance + Efficiency)',
      interfaceSpec: 'Coherent AXI5 / NoC (Network-on-Chip) Interconnect at 3.3 GHz',
      keyFeatures: [
        'Prime compute core executing out-of-order superscalar instructions',
        'Hardware memory management unit (MMU) with multi-level TLB translation',
        'Hardware privilege rings enforcing strict User Space vs Kernel Space isolation',
      ],
    },
  },
  {
    id: 'ram_memory',
    name: 'LPDDR5 High-Speed Memory',
    tag: 'LPDDR5 RAM',
    category: 'MEMORY',
    icon: 'flash',
    accentColor: '#AF52DE',
    anchorX: 0.64,
    anchorY: 0.33,
    startProgress: 0.25,
    endProgress: 0.72,
    telemetry: {
      technicalName: 'LPDDR5 High-Speed Memory',
      subtitle: 'Volatile fast-access workspace for active tasks',
      summary: 'Package-on-Package (PoP) low-power double data rate (LPDDR5) DRAM mounted directly atop the SoC die for ultra-short trace length and maximized memory bandwidth.',
      architecture: '16-Bank LPDDR5 SDRAM with On-Die Error Correction Code (ECC)',
      interfaceSpec: '64-bit Dual-Channel Bus operating at up to 6400 Mbps throughput',
      keyFeatures: [
        'Up to 51.2 GB/s ultra-wide aggregate memory throughput',
        'Dynamic voltage & frequency scaling (DVFS) for low idle power draw',
        'Holds active process page tables, stack segments, and mapped graphics surfaces',
      ],
    },
  },
  {
    id: 'screen_display',
    name: 'Dynamic AMOLED Panel',
    tag: 'AMOLED Display',
    category: 'DISPLAY',
    icon: 'phone-portrait',
    accentColor: '#FF2D55',
    anchorX: 0.50,
    anchorY: 0.50,
    startProgress: 0.86,
    endProgress: 0.93,
    telemetry: {
      technicalName: 'Dynamic AMOLED Panel',
      subtitle: 'Self-Emissive Matrix & Display Driver IC (DDIC)',
      summary: 'Flexible substrate active-matrix organic light-emitting diode (AMOLED) panel driven by a dedicated high-speed Display Driver IC (DDIC) connected via MIPI DSI.',
      architecture: 'Diamond PenTile Organic LED Matrix with 120Hz LTPO Backplane',
      interfaceSpec: 'MIPI DSI-2 with VESA Display Stream Compression (DSC)',
      keyFeatures: [
        'Over 2.4 million individual self-illuminating RGB subpixels',
        'Hardware touch digitization layer sampling at 240Hz polling rate',
        'Direct hardware frame-buffer presentation driven by Kernel SurfaceFlinger',
      ],
    },
  },
];

// Pre-mapped required static frame assets (00 to 59: 60 frames)
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
  require('../../assets/teardown_frames/frame_40.webp'),
  require('../../assets/teardown_frames/frame_41.webp'),
  require('../../assets/teardown_frames/frame_42.webp'),
  require('../../assets/teardown_frames/frame_43.webp'),
  require('../../assets/teardown_frames/frame_44.webp'),
  require('../../assets/teardown_frames/frame_45.webp'),
  require('../../assets/teardown_frames/frame_46.webp'),
  require('../../assets/teardown_frames/frame_47.webp'),
  require('../../assets/teardown_frames/frame_48.webp'),
  require('../../assets/teardown_frames/frame_49.webp'),
  require('../../assets/teardown_frames/frame_50.webp'),
  require('../../assets/teardown_frames/frame_51.webp'),
  require('../../assets/teardown_frames/frame_52.webp'),
  require('../../assets/teardown_frames/frame_53.webp'),
  require('../../assets/teardown_frames/frame_54.webp'),
  require('../../assets/teardown_frames/frame_55.webp'),
  require('../../assets/teardown_frames/frame_56.webp'),
  require('../../assets/teardown_frames/frame_57.webp'),
  require('../../assets/teardown_frames/frame_58.webp'),
  require('../../assets/teardown_frames/frame_59.webp'),
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
