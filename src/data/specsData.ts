export interface SpecItem {
  label: string;
  value: string;
  badge?: string;
  detail?: string;
}

export interface SpecCategory {
  id: string;
  title: string;
  subtitle: string;
  accentTag: string;
  specs: SpecItem[];
}

export interface SummaryChip {
  id: string;
  title: string;
  metric: string;
}

export const SUMMARY_CHIPS: SummaryChip[] = [
  { id: 'silicon', title: 'Core SoC', metric: '4nm FinFET' },
  { id: 'thermal', title: 'Cooling', metric: '4,200 mm²' },
  { id: 'power', title: 'Battery', metric: '5000 mAh' },
  { id: 'optics', title: 'Camera', metric: '50MP OIS' },
];

export const SPEC_CATEGORIES: SpecCategory[] = [
  {
    id: 'silicon',
    title: 'Core Silicon Architecture',
    subtitle: 'Application Processor & Neural Compute',
    accentTag: 'COMPUTE',
    specs: [
      {
        label: 'System on Chip',
        value: 'Octa-Core Heterogeneous SoC',
        badge: 'ARMv9.2-A',
        detail: 'Tri-cluster: 1x Cortex-X4 @ 3.3GHz, 5x Cortex-A720 @ 3.15GHz, 2x Cortex-A520 @ 2.27GHz',
      },
      {
        label: 'Process Geometry',
        value: '4nm FinFET Lithography',
        badge: '4nm EUV',
        detail: 'Extreme ultraviolet (EUV) patterning with monolithic packaging',
      },
      {
        label: 'Transistor Density',
        value: '~160 Million Transistors / mm²',
        badge: '160M/mm²',
        detail: 'High density die layout with direct micro-bump interconnects',
      },
      {
        label: 'Neural Engine (NPU)',
        value: '45 TOPS Hexagon Tensor Accelerator',
        badge: '45 TOPS',
        detail: 'INT4/INT8/FP16 native matrix execution for on-device LLMs',
      },
    ],
  },
  {
    id: 'thermal',
    title: 'Thermal Dissipation & Chassis',
    subtitle: 'Phase-Change Passive Cooling Matrix',
    accentTag: 'COOLING',
    specs: [
      {
        label: 'Vapor Chamber',
        value: '4,200 mm² Bimetallic Titanium-Copper',
        badge: '0.4mm Thin',
        detail: 'Ultra-thin sintered powder capillary loop directly bonded to motherboard shield',
      },
      {
        label: 'Thermal Interface (TIM)',
        value: 'Liquid-Metal Phase-Change Thermal Matrix',
        badge: '73 W/m-K',
        detail: 'Phase change at 45°C to eliminate micro air gaps over the SoC die',
      },
      {
        label: 'Chassis Material',
        value: 'Grade 5 Aerospace Titanium Sub-frame',
        badge: 'Ti-6Al-4V',
        detail: 'Diffusion bonded to internal recycled aluminum heat sink',
      },
    ],
  },
  {
    id: 'power',
    title: 'Power Subsystem & Charging',
    subtitle: 'High-Current Battery & Inductive Qi',
    accentTag: 'POWER',
    specs: [
      {
        label: 'Cell Chemistry',
        value: 'Silicon-Carbon High-Density Anode',
        badge: '5000 mAh',
        detail: 'Dual series cells (2x 2500mAh) enabling 10V/6.5A fast charging',
      },
      {
        label: 'Wired Fast Charge',
        value: '65W USB Power Delivery 3.0 PPS',
        badge: '65W PPS',
        detail: '0% to 50% in 14 minutes with active thermal impedance tracking',
      },
      {
        label: 'Wireless Qi Charging',
        value: '15W Qi2 MagSafe Magnetic Induction',
        badge: '15W Qi2',
        detail: 'Multi-strand litz wire planar coil with sintered ferrite shield',
      },
    ],
  },
  {
    id: 'optics',
    title: 'Optics & Sensor Array',
    subtitle: 'High-Resolution Computational Imaging',
    accentTag: 'OPTICS',
    specs: [
      {
        label: 'Primary Wide Sensor',
        value: '50MP 1/1.3" Quad-Bayer CMOS',
        badge: 'f/1.68 OIS',
        detail: 'Sensor-shift 4-axis ball bearing voice coil stabilization',
      },
      {
        label: 'Ultra-Wide Sensor',
        value: '12MP 120° FOV Ultra-Wide Macro',
        badge: '12MP f/2.2',
        detail: 'Automated 2.5cm macro autofocus via phase detection',
      },
      {
        label: 'Image Processor (ISP)',
        value: '18-bit Triple Spectra ISP (3.2 GP/s)',
        badge: '18-Bit ISP',
        detail: 'Direct MIPI CSI-2 4-lane hardware stream into Linux V4L2 drivers',
      },
    ],
  },
];
