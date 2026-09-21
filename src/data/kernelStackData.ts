import { Colors } from '../theme/colors';

export interface TierSubsystem {
  title: string;
  tag: string;
  description: string;
  metricLabel: string;
  metricValue: string;
}

export interface OsTier {
  id: string;
  index: number;
  title: string;
  subtitle: string;
  badge: string;
  privilegeRing: string;
  securityDomain: string;
  themeColor: string;
  keyApis: string[];
  subsystems: TierSubsystem[];
}

export const OS_TIERS: OsTier[] = [
  {
    id: 'silicon',
    index: 0,
    title: 'Physical Silicon & SoC',
    subtitle: 'Monolithic 4nm FinFET Microarchitecture & System Interconnect',
    badge: 'HARDWARE LAYER',
    privilegeRing: 'Bare Metal / ARM TrustZone',
    securityDomain: 'Root of Trust & Secure Processing Unit (SPU)',
    themeColor: Colors.steelBlue,
    keyApis: [
      'AXI/NoC Interconnect',
      'LPDDR5X Memory Bus (8533 Mbps)',
      'DVFS Dynamic Voltage Regulators',
      'Hardware GICv3 Interrupt Controller',
    ],
    subsystems: [
      {
        title: 'Monolithic 4nm FinFET Die',
        tag: 'PROCESS NODE',
        description: 'Manufactured on TSMC N4P process with ~16 billion transistors, integrating CPU, GPU, NPU, and modem on a single silicon substrate.',
        metricLabel: 'Transistor Density',
        metricValue: '142 MTr/mm²',
      },
      {
        title: 'DynamIQ Cluster Interconnect',
        tag: 'CPU COHERENCY',
        description: 'ARM CoreLink CI-700 system interconnect linking Cortex-X4, A720, and A520 clusters to a unified 8MB L3 system cache.',
        metricLabel: 'L3 Cache Bandwidth',
        metricValue: '1.2 TB/s',
      },
      {
        title: 'Hardware Security Island',
        tag: 'ARM TRUSTZONE',
        description: 'Cryptographically isolated secure enclave executing Trusty OS for biometric key storage and Android KeyMint operations.',
        metricLabel: 'Hardware Root',
        metricValue: 'eFuse OTP Ring',
      },
    ],
  },
  {
    id: 'kernel',
    index: 1,
    title: 'Linux Kernel',
    subtitle: 'Process Scheduling, Memory Paging, Drivers & SELinux',
    badge: 'KERNEL SPACE (RING 0)',
    privilegeRing: 'EL1 Supervisor Mode',
    securityDomain: 'SELinux Enforcing, App Sandboxing (UID/GID)',
    themeColor: Colors.steelBlue,
    keyApis: [
      'clone() / fork() / execve()',
      'epoll_wait() / futex()',
      'mmap() / madvise(MADV_DONTNEED)',
      'ioctl(/dev/binder, BINDER_WRITE_READ)',
    ],
    subsystems: [
      {
        title: 'Energy-Aware Scheduler (EAS)',
        tag: 'SCHEDULING',
        description: 'Extends the Completely Fair Scheduler (CFS) with energy models, placing interactive threads on Prime cores and background tasks on Efficiency cores.',
        metricLabel: 'Context Switch Overhead',
        metricValue: '< 1.2 µs',
      },
      {
        title: 'ZRAM Swap & Compaction',
        tag: 'VIRTUAL MEMORY',
        description: 'Compresses anonymous memory pages using LZ4/ZSTD in a RAM-backed block device, preventing sluggish NAND flash wear.',
        metricLabel: 'Compression Ratio',
        metricValue: '2.85x (1.8GB in 630MB)',
      },
      {
        title: 'Binder Driver (/dev/binder)',
        tag: 'KERNEL IPC',
        description: 'Custom Linux character driver providing high-speed zero-copy IPC using single-copy memory mapping between process address spaces.',
        metricLabel: 'IPC Throughput',
        metricValue: '~65,000 tx/sec',
      },
    ],
  },
  {
    id: 'hal',
    index: 2,
    title: 'Hardware Abstraction Layer (HAL)',
    subtitle: 'Vendor-Neutral Interfaces & AIDL/HIDL Daemons',
    badge: 'HARDWARE ABSTRACTION',
    privilegeRing: 'EL0 Isolated User Mode',
    securityDomain: 'Restricted hwservice_manager, Treble Boundary',
    themeColor: Colors.steelBlue,
    keyApis: [
      'android.hardware.camera.provider@2.7',
      'android.hardware.graphics.composer@3.0 (HWC)',
      'android.hardware.audio@7.1',
      'android.hardware.biometrics.fingerprint',
    ],
    subsystems: [
      {
        title: 'Stable AIDL Interfaces',
        tag: 'TREBLE ARCHITECTURE',
        description: 'Replaces legacy HIDL with Stable AIDL, decoupling the Android Framework from vendor proprietary BSP drivers across Android OS upgrades.',
        metricLabel: 'ABI Compatibility',
        metricValue: 'Strict Stable AIDL',
      },
      {
        title: 'Hardware Composer (HWC3)',
        tag: 'SURFACE FLINGER',
        description: 'Directly programs the display processor (DPU) overlays to composite UI surfaces directly in hardware, bypassing GPU overhead.',
        metricLabel: 'Display Pipeline',
        metricValue: '120Hz Zero-Jank',
      },
      {
        title: 'Camera HAL3 Pipeline',
        tag: 'IMAGE SIGNAL PROCESSOR',
        description: 'Directly coordinates capture requests between the Android Camera2 API and vendor ISP hardware processing blocks.',
        metricLabel: 'Capture Latency',
        metricValue: '< 18ms Frame Interval',
      },
    ],
  },
  {
    id: 'art',
    index: 3,
    title: 'Android Runtime (ART) & Bionic',
    subtitle: 'AOT/JIT Compilation, Concurrent GC & System Libraries',
    badge: 'MANAGED RUNTIME',
    privilegeRing: 'EL0 User Mode',
    securityDomain: 'Isolated Dalvik/ART VM, Bionic Libc Sandboxing',
    themeColor: Colors.mustard,
    keyApis: [
      'art::gc::ConcurrentCopyingCollector',
      'Baseline Profiles & Cloud DEX PGO',
      'Bionic libc / libm / libdl',
      'Zygote forkAndSpecializeAppProcess()',
    ],
    subsystems: [
      {
        title: 'Concurrent Copying (CC) GC',
        tag: 'MEMORY MANAGEMENT',
        description: 'Generational garbage collector executing concurrently with app threads using read barriers, eliminating visible frame drops.',
        metricLabel: 'GC Pause Time',
        metricValue: '< 1.5ms per collection',
      },
      {
        title: 'Baseline Profiles & Cloud PGO',
        tag: 'COMPILATION ENGINE',
        description: 'Pre-compiles critical user interaction pathways into native machine code before first app launch, boosting startup by up to 40%.',
        metricLabel: 'Startup Acceleration',
        metricValue: '+38% First Frame',
      },
      {
        title: 'Zygote Template Forking',
        tag: 'PROCESS LAUNCH',
        description: 'Pre-loads all core framework classes and Android resources, using Linux Copy-on-Write (COW) memory sharing when spawning apps.',
        metricLabel: 'Warm Fork Time',
        metricValue: '~22ms from Intent',
      },
    ],
  },
  {
    id: 'framework',
    index: 4,
    title: 'Userspace & UI Framework',
    subtitle: 'System Services, IPC Dispatch & UI Rendering Pipeline',
    badge: 'APPLICATION LAYER',
    privilegeRing: 'EL0 Unprivileged User Mode',
    securityDomain: 'App UID Sandbox, Runtime Permissions',
    themeColor: Colors.mustard,
    keyApis: [
      'androidx.compose.runtime (Snapshot State)',
      'android.view.Choreographer (120Hz VSYNC)',
      'android.os.ServiceManager / ActivityManager',
      'android.graphics.HardwareRenderer (RenderThread)',
    ],
    subsystems: [
      {
        title: 'Declarative UI Engine',
        tag: 'UI TOOLKIT',
        description: 'Reactive declarative UI engine using positional memoization and snapshot state tracking to intelligently recompose only dirty nodes.',
        metricLabel: 'Frame Budget',
        metricValue: '8.33ms (120 FPS Target)',
      },
      {
        title: 'Choreographer & RenderThread',
        tag: 'SYSTEM PIPELINE',
        description: 'Locks UI traversal to hardware VSYNC pulses, dispatching draw commands to Skia/Vulkan on a dedicated background RenderThread.',
        metricLabel: 'VSYNC Cadence',
        metricValue: 'Hardware Locked',
      },
      {
        title: 'App Sandboxing (UID Protection)',
        tag: 'ANDROID SECURITY',
        description: 'Assigns each installed app a unique Linux user ID (e.g. u0_a184), enforcing strict filesystem and network isolation.',
        metricLabel: 'SELinux Context',
        metricValue: 'u:r:untrusted_app:s0',
      },
    ],
  },
];

export type CpuGovernor = 'schedutil' | 'performance' | 'powersave';

export interface CpuCore {
  index: number;
  name: string;
  cluster: 'PRIME' | 'PERF' | 'EFF';
  baseFreqGhz: number;
  maxFreqGhz: number;
  currentFreqGhz: number;
  load: number;
  task: string;
}

export function generateCoreData(governor: CpuGovernor, isThrottled: boolean): CpuCore[] {
  const throttleMultiplier = isThrottled ? 0.7 : 1.0;

  return [
    {
      index: 0,
      name: 'Core 0',
      cluster: 'PRIME',
      baseFreqGhz: 0.8,
      maxFreqGhz: 3.3 * throttleMultiplier,
      currentFreqGhz:
        governor === 'performance'
          ? 3.3 * throttleMultiplier
          : governor === 'powersave'
          ? 0.8
          : 2.7 * throttleMultiplier,
      load: governor === 'powersave' ? 45 : 82,
      task: 'RenderThread (120fps)',
    },
    {
      index: 1,
      name: 'Core 1',
      cluster: 'PERF',
      baseFreqGhz: 0.6,
      maxFreqGhz: 3.15,
      currentFreqGhz:
        governor === 'performance' ? 3.15 : governor === 'powersave' ? 0.6 : 2.2,
      load: 65,
      task: 'SurfaceFlinger',
    },
    {
      index: 2,
      name: 'Core 2',
      cluster: 'PERF',
      baseFreqGhz: 0.6,
      maxFreqGhz: 3.15,
      currentFreqGhz:
        governor === 'performance' ? 3.15 : governor === 'powersave' ? 0.6 : 1.85,
      load: 54,
      task: 'ExoPlayer Codec',
    },
    {
      index: 3,
      name: 'Core 3',
      cluster: 'PERF',
      baseFreqGhz: 0.6,
      maxFreqGhz: 3.15,
      currentFreqGhz:
        governor === 'performance' ? 3.15 : governor === 'powersave' ? 0.6 : 1.45,
      load: 40,
      task: 'JIT Compiler Pool',
    },
    {
      index: 4,
      name: 'Core 4',
      cluster: 'EFF',
      baseFreqGhz: 0.4,
      maxFreqGhz: 2.27,
      currentFreqGhz:
        governor === 'performance' ? 2.27 : governor === 'powersave' ? 0.4 : 1.15,
      load: 28,
      task: 'kswapd0 (Paging)',
    },
    {
      index: 5,
      name: 'Core 5',
      cluster: 'EFF',
      baseFreqGhz: 0.4,
      maxFreqGhz: 2.27,
      currentFreqGhz:
        governor === 'performance' ? 2.27 : governor === 'powersave' ? 0.4 : 0.95,
      load: 18,
      task: 'Binder Worker #1',
    },
  ];
}
