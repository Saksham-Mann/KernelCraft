export interface ComponentTelemetry {
  technicalName: string;
  subtitle: string;
  summary: string;
  keySpecs: string;
  architecture?: string;
  interfaceSpec?: string;
  keyFeatures?: string[];
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
    title: 'Disassembly',
    description: 'Hermetically sealed chassis expands into the layered exploded component view.',
    startProgress: 0.0,
    endProgress: 0.58,
    accentTag: 'DISASSEMBLY',
    emoji: '',
  },
  {
    id: 'snapping_back',
    title: 'Precision Reassembly',
    description: 'Exploded parts pull back together and lock hermetically into the chassis frame.',
    startProgress: 0.58,
    endProgress: 0.86,
    accentTag: 'REASSEMBLY',
    emoji: '',
  },
  {
    id: 'front_screen',
    title: 'Front AMOLED Display',
    description: 'The assembled phone rotates over to reveal the front Dynamic AMOLED display.',
    startProgress: 0.86,
    endProgress: 1.0,
    accentTag: 'DISPLAY READY',
    emoji: '',
  },
];

export const TEARDOWN_ANNOTATIONS: TeardownAnnotation[] = [
  {
    id: 'battery_pack',
    name: 'Battery & Power Management',
    tag: 'Battery & Power',
    category: 'POWER',
    icon: 'battery-charging',
    accentColor: '#34C759',
    anchorX: 0.48,
    anchorY: 0.68,
    startProgress: 0.18,
    endProgress: 0.58,
    telemetry: {
      technicalName: 'Battery & Power Management',
      subtitle: 'Safe, high-density energy with intelligent charging.',
      summary: 'Stores chemical energy and regulates current to ensure the CPU never draws more power than is safe.',
      keySpecs: '5,000 mAh high-capacity • Smart thermal auto-throttling',
      architecture: 'Series high-density chemical cell with PMIC voltage regulation',
      interfaceSpec: 'Integrated gas gauge sensor & over-current protection bus',
      keyFeatures: [
        '5,000 mAh high-capacity chemical energy storage',
        'Direct multi-rail PMIC distribution (1.8V, 3.3V, core)',
        'Thermal sensing NTC thermistors with auto-throttling safety cutoffs',
      ],
    },
  },
  {
    id: 'camera_array',
    name: 'Camera & Image Processor',
    tag: 'Camera & ISP',
    category: 'OPTICS',
    icon: 'camera',
    accentColor: '#007AFF',
    anchorX: 0.23,
    anchorY: 0.44,
    startProgress: 0.18,
    endProgress: 0.58,
    telemetry: {
      technicalName: 'Camera & Image Processor',
      subtitle: 'Translates photons into clear digital photographs.',
      summary: 'Light hits the sensor grid, and a dedicated image processor runs millions of calculations per second to enhance shadows, stabilize shaky hands, and balance color.',
      keySpecs: '50MP optical sensor • Instant hardware image enhancement',
      architecture: 'Multi-lens optical sensor array with voice-coil actuators',
      interfaceSpec: '4-Lane MIPI CSI-2 high-speed differential serial data lanes',
      keyFeatures: [
        'Sub-micron quad-bayer photodiodes with phase detection autofocus',
        'Zero-shutter-lag real-time hardware demosaicing and noise reduction',
        'Direct DMA streaming into shared unified memory buffers for GPU consumption',
      ],
    },
  },
  {
    id: 'cpu_brain',
    name: 'System on Chip (SoC)',
    tag: 'System on Chip',
    category: 'BRAIN',
    icon: 'hardware-chip',
    accentColor: '#FF9500',
    anchorX: 0.44,
    anchorY: 0.28,
    startProgress: 0.18,
    endProgress: 0.58,
    telemetry: {
      technicalName: 'System on Chip (SoC)',
      subtitle: 'The brain, graphics engine, and AI brain in one tiny chip.',
      summary: 'Combines the CPU, GPU, neural processor, and modem into a single silicon wafer smaller than a postage stamp.',
      keySpecs: '8-Core CPU + AI Neural Engine • 3.3 GHz peak clock',
      architecture: 'Tri-cluster compute cores (Prime, Performance, Efficiency)',
      interfaceSpec: 'Ultra-fast Network-on-Chip (NoC) interconnect',
      keyFeatures: [
        'Prime compute core executing out-of-order superscalar instructions',
        'Hardware memory management unit (MMU) with multi-level TLB translation',
        'Hardware privilege rings enforcing strict User Space vs Kernel Space isolation',
      ],
    },
  },
  {
    id: 'ram_memory',
    name: 'High-Speed Memory (RAM)',
    tag: 'LPDDR5 RAM',
    category: 'MEMORY',
    icon: 'flash',
    accentColor: '#AF52DE',
    anchorX: 0.60,
    anchorY: 0.32,
    startProgress: 0.18,
    endProgress: 0.58,
    telemetry: {
      technicalName: 'High-Speed Memory (RAM)',
      subtitle: "Your phone's ultra-fast scratchpad.",
      summary: 'RAM keeps your recently opened apps and games running in the background so they instantly open without having to reload from scratch.',
      keySpecs: 'Up to 51.2 GB/s speed • Keeps 15+ apps alive simultaneously',
      architecture: 'Package-on-Package (PoP) DRAM mounted directly atop SoC',
      interfaceSpec: '64-bit dual-channel bus operating at up to 6400 Mbps',
      keyFeatures: [
        'Up to 51.2 GB/s ultra-wide aggregate memory throughput',
        'Dynamic voltage & frequency scaling (DVFS) for low idle power draw',
        'Holds active process page tables, stack segments, and mapped graphics surfaces',
      ],
    },
  },
  {
    id: 'screen_display',
    name: 'AMOLED Display',
    tag: 'AMOLED Display',
    category: 'DISPLAY',
    icon: 'phone-portrait',
    accentColor: '#FF2D55',
    anchorX: 0.50,
    anchorY: 0.50,
    startProgress: 0.91,
    endProgress: 0.984,
    telemetry: {
      technicalName: 'AMOLED Display',
      subtitle: 'Millions of microscopic self-glowing pixels.',
      summary: 'Unlike LCD screens that need a backlight, every pixel turns completely off to produce deep true blacks and saves massive battery life in dark mode.',
      keySpecs: '120Hz refresh rate (super smooth touch) • Instant pixel response',
      architecture: 'Self-emissive organic LED matrix with 120Hz LTPO backplane',
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
