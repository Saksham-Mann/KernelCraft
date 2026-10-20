import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Platform,
  Dimensions,
  PixelRatio,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { Colors } from '../theme/colors';
import { Typography, FontFamilies } from '../theme/typography';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import * as Device from 'expo-device';

export type NativeSchedulerFamily =
  | 'WINDOWS_NT_BOOST'
  | 'LINUX_CFS_EEVDF'
  | 'MACOS_MACH_MLFQ';

interface ProcessDef {
  id: string;
  name: string;
  shortName: string;
  category: string;
  arrivalTime: number;
  burstTime: number;
  basePriority: number; // 1 (highest) to 4 (lowest)
  nice: number; // Linux CFS nice value (-5 to +5)
  weight: number; // CFS weight (3121, 1024, 1024, 335)
  initialQueue: number; // macOS MLFQ queue 0, 1, 2
  color: string;
}

interface ProcessRuntimeState {
  id: string;
  remainingBurst: number;
  completionTime: number | null;
  turnaroundTime: number | null;
  waitingTime: number | null;
  started: boolean;

  // Windows NT Dispatcher: dynamic priority boosting
  effectivePriority: number;
  consecutiveWaitTicks: number;
  boostTicksRemaining: number;

  // Linux CFS / EEVDF: Virtual Runtime tracking
  vruntime: number;

  // macOS Mach MLFQ: Multilevel Feedback Queue tracking
  currentQueue: number; // 0 (1ms), 1 (2ms), 2 (4ms)
  quantumUsedInQueue: number;
}

interface GanttSlice {
  id: string;
  processId: string;
  shortName: string;
  color: string;
  startTime: number;
  endTime: number;
  isIdle?: boolean;
}

interface HostTelemetry {
  osFamily: 'Windows' | 'Linux' | 'macOS';
  osName: string;
  kernelVersion: string;
  platformArch: string;
  cpuCores: number | string;
  screenResolution: string;
  pixelRatio: string;
  schedulingAlgorithmName: string;
  runtimeEnvironment: string;
  schedulerFamily: NativeSchedulerFamily;
}

const INITIAL_PROCESSES: ProcessDef[] = [
  {
    id: 'P1',
    name: 'Audio Playback Daemon',
    shortName: 'Audio',
    category: 'Real-Time Audio Buffer',
    arrivalTime: 0,
    burstTime: 4,
    basePriority: 1, // Real-time priority
    nice: -5,
    weight: 3121, // High weight in CFS
    initialQueue: 0, // Q0 in MLFQ
    color: '#22C55E', // Green
  },
  {
    id: 'P2',
    name: 'UI Frame Compositor',
    shortName: 'UI Render',
    category: 'Display Refresh Pipeline',
    arrivalTime: 1,
    burstTime: 5,
    basePriority: 2, // High priority
    nice: 0,
    weight: 1024, // Standard weight
    initialQueue: 0, // Q0 in MLFQ
    color: '#3B82F6', // Blue
  },
  {
    id: 'P3',
    name: 'Application Logic Worker',
    shortName: 'App Logic',
    category: 'User Input Handler',
    arrivalTime: 2,
    burstTime: 3,
    basePriority: 3, // Normal priority
    nice: 0,
    weight: 1024,
    initialQueue: 0, // Q0 in MLFQ
    color: '#F59E0B', // Amber
  },
  {
    id: 'P4',
    name: 'Cloud Data Synchronization',
    shortName: 'Sync Worker',
    category: 'Background Network Batch',
    arrivalTime: 4,
    burstTime: 4,
    basePriority: 4, // Low priority
    nice: 5,
    weight: 335, // Low weight in CFS
    initialQueue: 1, // Q1 in MLFQ
    color: '#A855F7', // Purple
  },
];

export const MyDeviceInspector: React.FC = () => {
  // ---------------------------------------------------------------------------
  // 1. Dynamic Host OS & Kernel Scheduler Detection
  // ---------------------------------------------------------------------------
  const [telemetry, setTelemetry] = useState<HostTelemetry>({
    osFamily: 'Windows',
    osName: 'Detecting...',
    kernelVersion: 'Detecting...',
    platformArch: 'Detecting...',
    cpuCores: 'Detecting...',
    screenResolution: 'Detecting...',
    pixelRatio: '1.0x',
    schedulingAlgorithmName: 'Detecting Kernel Scheduler...',
    runtimeEnvironment: 'Detecting...',
    schedulerFamily: 'WINDOWS_NT_BOOST',
  });

  useEffect(() => {
    const gatherTelemetry = async () => {
      const screen = Dimensions.get('screen');
      const pr = PixelRatio.get();

      // -----------------------------------------------------------------------
      // 1. Authoritative platform detection via expo-device + Platform.OS
      //    These values come directly from the native runtime or the Expo SDK,
      //    so they are accurate on Android, iOS, and web — no UA parsing needed.
      // -----------------------------------------------------------------------
      const platformOS = Platform.OS; // 'android' | 'ios' | 'web'

      // expo-device fields (synchronous properties; wrapped in async function
      // so callers can extend with async APIs like Device.getDeviceTypeAsync())
      const deviceOsName: string = Device.osName ?? platformOS;        // e.g. 'Android', 'iOS', 'Windows'
      const deviceOsVersion: string = Device.osVersion ?? 'Unknown';   // e.g. '14', '17.4'
      const deviceModelName: string = Device.modelName ?? 'Unknown';   // e.g. 'Pixel 8 Pro'
      const isDevice: boolean = Device.isDevice;                        // false in simulators / browsers

      // Optional: resolve device type (phone / tablet / desktop / TV)
      // This is the only truly async expo-device call; others are sync properties.
      let deviceType: Device.DeviceType | null = null;
      try {
        deviceType = await Device.getDeviceTypeAsync();
      } catch {
        // getDeviceTypeAsync is not supported on web; fall back to null
      }

      // -----------------------------------------------------------------------
      // 2. Map expo-device + Platform.OS → HostTelemetry fields
      // -----------------------------------------------------------------------
      let family: 'Windows' | 'Linux' | 'macOS' = 'Windows';
      let familyKey: NativeSchedulerFamily = 'WINDOWS_NT_BOOST';
      let os = 'Unknown OS';
      let kernel = 'Unknown Kernel';
      let arch = 'Unknown Arch';
      let schedName = 'Unknown Scheduler';
      let runtime = 'Unknown Runtime';
      let cores: number | string = 'N/A';

      if (platformOS === 'android') {
        // Android — runs on a Linux kernel with EEVDF + EAS scheduling
        family = 'Linux';
        familyKey = 'LINUX_CFS_EEVDF';
        os = `${deviceOsName} ${deviceOsVersion} (Linux Kernel)`;
        kernel = 'Linux Kernel 6.1 / 6.6 (EEVDF Enabled) + EAS';
        arch = 'ARMv9 / ARM64';
        schedName = 'EEVDF (Earliest Eligible Virtual Deadline First) & CFS + EAS';
        runtime = 'Android Runtime (ART)';

      } else if (platformOS === 'ios') {
        // iOS / iPadOS — runs on the Darwin / XNU Mach kernel
        family = 'macOS';
        familyKey = 'MACOS_MACH_MLFQ';
        os = `${deviceOsName} ${deviceOsVersion} (Darwin Kernel)`;
        kernel = 'Darwin / XNU (Mach Kernel 23.x)';
        arch = 'Apple Bionic ARM64';
        schedName = 'Mach Kernel Multilevel Feedback Queue (MLFQ) & GCD';
        runtime = 'JavaScriptCore (Hermes)';

      } else {
        // Web — resolve host OS from the Device module where available;
        // expo-device populates osName on web via the underlying platform APIs.
        const webOsName = deviceOsName.toLowerCase();

        if (webOsName.includes('windows')) {
          family = 'Windows';
          familyKey = 'WINDOWS_NT_BOOST';
          os = `${deviceOsName} ${deviceOsVersion}`;
          kernel = `Windows NT 10.0 (Build 22631 / 26200)`;
          arch = 'x86_64 (64-bit)';
          schedName = 'Windows NT Kernel Thread Dispatcher (Priority-Driven Preemptive MLFQ with Dynamic Priority Boosting)';
        } else if (webOsName.includes('mac') || webOsName.includes('darwin')) {
          family = 'macOS';
          familyKey = 'MACOS_MACH_MLFQ';
          os = `macOS ${deviceOsVersion} (Darwin)`;
          kernel = 'Darwin / XNU (Mach Kernel 23.x)';
          arch = 'Apple Silicon (ARM64)';
          schedName = 'Mach Kernel Multilevel Feedback Queue (MLFQ) & GCD';
        } else {
          // Linux / ChromeOS / Android-on-web
          family = 'Linux';
          familyKey = 'LINUX_CFS_EEVDF';
          os = `${deviceOsName || 'Linux'} ${deviceOsVersion}`;
          kernel = 'Linux Kernel 6.6+ (EEVDF Default Scheduler)';
          arch = 'x86_64';
          schedName = 'EEVDF (Earliest Eligible Virtual Deadline First) & CFS';
        }

        // Browser JS engine — read from navigator only on web where it is valid
        if (typeof navigator !== 'undefined') {
          cores = navigator.hardwareConcurrency ?? 'N/A';
          const ua = navigator.userAgent;
          if (/Edg\//i.test(ua))          runtime = 'Chromium Edge (V8)';
          else if (/Chrome/i.test(ua))    runtime = 'Chromium Engine (V8)';
          else if (/Firefox/i.test(ua))   runtime = 'Gecko Engine (SpiderMonkey)';
          else if (/Safari/i.test(ua))    runtime = 'WebKit Engine (Nitro)';
          else                            runtime = 'Expo Web Sandbox';
        }
      }

      const screenRes = `${Math.round(screen.width * pr)} x ${Math.round(screen.height * pr)} px`;

      setTelemetry({
        osFamily: family,
        osName: os,
        kernelVersion: kernel,
        platformArch: arch,
        cpuCores: cores,
        screenResolution: screenRes,
        pixelRatio: `${pr.toFixed(1)}x`,
        schedulingAlgorithmName: schedName,
        runtimeEnvironment: runtime,
        schedulerFamily: familyKey,
      });
    };

    gatherTelemetry();
  }, []);

  // ---------------------------------------------------------------------------
  // 2. Dynamic Simulation Engine (Directly Tied to Detected Kernel Scheduler)
  // ---------------------------------------------------------------------------
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [processStates, setProcessStates] = useState<ProcessRuntimeState[]>(() =>
    INITIAL_PROCESSES.map((p) => ({
      id: p.id,
      remainingBurst: p.burstTime,
      completionTime: null,
      turnaroundTime: null,
      waitingTime: null,
      started: false,
      effectivePriority: p.basePriority,
      consecutiveWaitTicks: 0,
      boostTicksRemaining: 0,
      vruntime: 0,
      currentQueue: p.initialQueue,
      quantumUsedInQueue: 0,
    }))
  );

  const [ganttTimeline, setGanttTimeline] = useState<GanttSlice[]>([]);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [contextSwitches, setContextSwitches] = useState<number>(0);

  // Auto-scroll Gantt chart
  const ganttScrollRef = useRef<ScrollView>(null);

  // Reset simulation state
  const resetSimulation = useCallback(() => {
    setIsPlaying(false);
    setCurrentTime(0);
    setContextSwitches(0);
    setGanttTimeline([]);
    setProcessStates(
      INITIAL_PROCESSES.map((p) => ({
        id: p.id,
        remainingBurst: p.burstTime,
        completionTime: null,
        turnaroundTime: null,
        waitingTime: null,
        started: false,
        effectivePriority: p.basePriority,
        consecutiveWaitTicks: 0,
        boostTicksRemaining: 0,
        vruntime: 0,
        currentQueue: p.initialQueue,
        quantumUsedInQueue: 0,
      }))
    );
  }, []);

  // Execute 1 Clock Tick (1 ms CPU step) driven directly by detected OS scheduler
  const stepClockTick = useCallback(() => {
    setProcessStates((prevStates) => {
      // Check if all processes completed
      const allDone = prevStates.every((p) => p.remainingBurst <= 0);
      if (allDone) {
        setIsPlaying(false);
        return prevStates;
      }

      const t = currentTime;

      // Filter processes that have arrived and have burst remaining
      const arrived = prevStates.filter((ps) => {
        const def = INITIAL_PROCESSES.find((p) => p.id === ps.id);
        return def && def.arrivalTime <= t && ps.remainingBurst > 0;
      });

      let chosenId: string | null = null;

      if (arrived.length > 0) {
        if (telemetry.schedulerFamily === 'WINDOWS_NT_BOOST') {
          // -------------------------------------------------------------------
          // WINDOWS NT KERNEL THREAD DISPATCHER
          // Priority-Driven Preemptive MLFQ with Dynamic Starvation Boost
          // -------------------------------------------------------------------
          // 1. Calculate dynamic priority boost for waiting threads
          const evaluated = arrived.map((proc) => {
            let effPrio = proc.effectivePriority;
            let boostRem = proc.boostTicksRemaining;

            // If a thread has waited >= 3 ticks without getting CPU, grant priority boost
            if (proc.consecutiveWaitTicks >= 3 && boostRem === 0) {
              effPrio = Math.max(1, proc.effectivePriority - 1); // Boost level
              boostRem = 2; // Lasts 2 ticks
            }
            return {
              ...proc,
              evalPriority: effPrio,
              evalBoostRem: boostRem,
            };
          });

          // Sort by effective priority (lowest numeric value = highest priority)
          evaluated.sort((a, b) => {
            if (a.evalPriority !== b.evalPriority) {
              return a.evalPriority - b.evalPriority;
            }
            const defA = INITIAL_PROCESSES.find((p) => p.id === a.id)!;
            const defB = INITIAL_PROCESSES.find((p) => p.id === b.id)!;
            return defA.arrivalTime - defB.arrivalTime;
          });

          chosenId = evaluated[0].id;
        } else if (telemetry.schedulerFamily === 'LINUX_CFS_EEVDF') {
          // -------------------------------------------------------------------
          // LINUX CFS / EEVDF SCHEDULER
          // Virtual Runtime (vruntime) tracking with weight normalization
          // -------------------------------------------------------------------
          // Sort by lowest vruntime
          const sorted = [...arrived].sort((a, b) => {
            if (Math.abs(a.vruntime - b.vruntime) > 0.01) {
              return a.vruntime - b.vruntime;
            }
            const defA = INITIAL_PROCESSES.find((p) => p.id === a.id)!;
            const defB = INITIAL_PROCESSES.find((p) => p.id === b.id)!;
            return defA.arrivalTime - defB.arrivalTime;
          });

          chosenId = sorted[0].id;
        } else {
          // -------------------------------------------------------------------
          // MACOS / IOS MACH KERNEL MLFQ
          // 3-Queue Multilevel Feedback: Q0 (1ms), Q1 (2ms), Q2 (4ms)
          // -------------------------------------------------------------------
          // Pick from highest priority non-empty queue (0 > 1 > 2)
          const sorted = [...arrived].sort((a, b) => {
            if (a.currentQueue !== b.currentQueue) {
              return a.currentQueue - b.currentQueue;
            }
            const defA = INITIAL_PROCESSES.find((p) => p.id === a.id)!;
            const defB = INITIAL_PROCESSES.find((p) => p.id === b.id)!;
            return defA.arrivalTime - defB.arrivalTime;
          });

          chosenId = sorted[0].id;
        }
      }

      // Check context switch & update Gantt Timeline
      setGanttTimeline((prevGantt) => {
        const lastSlice = prevGantt[prevGantt.length - 1];

        if (chosenId) {
          const chosenDef = INITIAL_PROCESSES.find((p) => p.id === chosenId)!;

          if (lastSlice && lastSlice.processId === chosenId) {
            // Extend active execution block
            const updated = [...prevGantt];
            updated[updated.length - 1] = {
              ...lastSlice,
              endTime: t + 1,
            };
            return updated;
          } else {
            // Context switch occurred
            if (lastSlice && !lastSlice.isIdle) {
              setContextSwitches((c) => c + 1);
            }
            return [
              ...prevGantt,
              {
                id: `${chosenId}-${t}`,
                processId: chosenId,
                shortName: chosenDef.shortName,
                color: chosenDef.color,
                startTime: t,
                endTime: t + 1,
              },
            ];
          }
        } else {
          // CPU Idle slice
          if (lastSlice && lastSlice.isIdle) {
            const updated = [...prevGantt];
            updated[updated.length - 1] = {
              ...lastSlice,
              endTime: t + 1,
            };
            return updated;
          } else {
            return [
              ...prevGantt,
              {
                id: `IDLE-${t}`,
                processId: 'IDLE',
                shortName: 'Idle',
                color: '#374151',
                startTime: t,
                endTime: t + 1,
                isIdle: true,
              },
            ];
          }
        }
      });

      // Update runtime state of all processes for next tick
      const nextStates = prevStates.map((proc) => {
        const def = INITIAL_PROCESSES.find((item) => item.id === proc.id)!;
        const hasArrived = def.arrivalTime <= t;
        const isSelected = proc.id === chosenId;

        if (isSelected) {
          const newRemaining = Math.max(0, proc.remainingBurst - 1);
          const isFinished = newRemaining === 0;
          const ct = isFinished ? t + 1 : null;
          const tat = isFinished ? ct! - def.arrivalTime : null;
          const wt = isFinished ? tat! - def.burstTime : null;

          // 1. Windows state update
          let newEffPrio = proc.effectivePriority;
          let newBoostRem = proc.boostTicksRemaining;
          if (newBoostRem > 0) {
            newBoostRem -= 1;
            if (newBoostRem === 0) {
              newEffPrio = def.basePriority; // Decay boost
            }
          }

          // 2. Linux CFS vruntime update: delta_vruntime = (1024 / weight) * 1ms
          const deltaVruntime = (1024 / def.weight) * 1.0;
          const newVruntime = proc.vruntime + deltaVruntime;

          // 3. macOS MLFQ queue state update
          let nextQueue = proc.currentQueue;
          let nextQuantumUsed = proc.quantumUsedInQueue + 1;
          if (proc.currentQueue === 0 && nextQuantumUsed >= 1) {
            nextQueue = 1; // Demote to Q1
            nextQuantumUsed = 0;
          } else if (proc.currentQueue === 1 && nextQuantumUsed >= 2) {
            nextQueue = 2; // Demote to Q2
            nextQuantumUsed = 0;
          } else if (proc.currentQueue === 2 && nextQuantumUsed >= 4) {
            nextQuantumUsed = 0; // Round robin in Q2
          }

          return {
            ...proc,
            started: true,
            remainingBurst: newRemaining,
            completionTime: isFinished ? ct : proc.completionTime,
            turnaroundTime: isFinished ? tat : proc.turnaroundTime,
            waitingTime: isFinished ? wt : proc.waitingTime,
            consecutiveWaitTicks: 0,
            effectivePriority: newEffPrio,
            boostTicksRemaining: newBoostRem,
            vruntime: newVruntime,
            currentQueue: nextQueue,
            quantumUsedInQueue: nextQuantumUsed,
          };
        } else {
          // Process not selected this tick
          let waitTicks = proc.consecutiveWaitTicks;
          let effPrio = proc.effectivePriority;
          let boostRem = proc.boostTicksRemaining;

          if (hasArrived && proc.remainingBurst > 0) {
            waitTicks += 1;
            // Windows Starvation check: boost priority after 3 wait ticks
            if (telemetry.schedulerFamily === 'WINDOWS_NT_BOOST') {
              if (waitTicks >= 3 && boostRem === 0) {
                effPrio = Math.max(1, def.basePriority - 1);
                boostRem = 2;
              }
            }

            // macOS Anti-starvation periodic boost (every 8 ticks)
            if (telemetry.schedulerFamily === 'MACOS_MACH_MLFQ' && t > 0 && t % 8 === 0) {
              proc.currentQueue = Math.max(0, proc.currentQueue - 1);
              proc.quantumUsedInQueue = 0;
            }
          }

          return {
            ...proc,
            consecutiveWaitTicks: waitTicks,
            effectivePriority: effPrio,
            boostTicksRemaining: boostRem,
          };
        }
      });

      // Advance clock
      setCurrentTime((prev) => prev + 1);

      if (Platform.OS !== 'web') {
        try {
          Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
        } catch (e) {}
      }

      return nextStates;
    });
  }, [currentTime, telemetry.schedulerFamily]);

  // Auto-play timer
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isPlaying) {
      interval = setInterval(() => {
        stepClockTick();
      }, 650);
    }
    return () => clearInterval(interval);
  }, [isPlaying, stepClockTick]);

  // Auto scroll Gantt timeline to end as blocks progress
  useEffect(() => {
    ganttScrollRef.current?.scrollToEnd({ animated: true });
  }, [ganttTimeline]);

  // Aggregate Metrics calculations
  const completedProcesses = processStates.filter((p) => p.completionTime !== null);
  const totalCompleted = completedProcesses.length;

  const avgTurnaround =
    totalCompleted > 0
      ? (
          completedProcesses.reduce((acc, p) => acc + (p.turnaroundTime || 0), 0) /
          totalCompleted
        ).toFixed(1)
      : '--';

  const avgWaiting =
    totalCompleted > 0
      ? (
          completedProcesses.reduce((acc, p) => acc + (p.waitingTime || 0), 0) /
          totalCompleted
        ).toFixed(1)
      : '--';

  const totalBusyTicks = ganttTimeline
    .filter((g) => !g.isIdle)
    .reduce((acc, g) => acc + (g.endTime - g.startTime), 0);

  const cpuUtilization =
    currentTime > 0 ? ((totalBusyTicks / currentTime) * 100).toFixed(0) : '100';

  const isAllComplete = processStates.every((p) => p.remainingBurst <= 0);

  return (
    <View style={styles.container}>
      {/* =================================================================== */}
      {/* 1. CPU SCHEDULING RULES (Clean Reference Design)                    */}
      {/* =================================================================== */}
      <View style={styles.cardSurface}>
        <View style={styles.rulesHeader}>
          <Text style={styles.rulesTitle}>CPU SCHEDULING RULES</Text>
          <Text style={styles.rulesSubtitle}>
            How does your phone decide which app gets the CPU?
          </Text>
        </View>

        <View style={styles.rulesList}>
          {/* Rule 1: CPU Core Allocation */}
          <View style={styles.ruleItemRow}>
            <View style={[styles.ruleIndexCircle, { backgroundColor: '#22C55E' }]}>
              <Text style={styles.ruleIndexText}>1</Text>
            </View>
            <View style={styles.ruleTextCol}>
              <Text style={styles.ruleTitle}>CPU Core Allocation</Text>
              <Text style={styles.ruleDescription}>
                A single CPU core can only calculate one task at a time.
              </Text>
            </View>
            {/* Visual Micro-Graphic: CPU Chip */}
            <View style={styles.ruleGraphicBox}>
              <View style={styles.cpuMiniChip}>
                <Ionicons name="hardware-chip" size={18} color="#22C55E" />
                <Text style={styles.cpuMiniText}>CPU</Text>
              </View>
              <View style={styles.cpuMiniTasksCol}>
                <View style={[styles.miniAppDot, { backgroundColor: '#EF4444' }]} />
                <View style={[styles.miniAppDot, { backgroundColor: '#A855F7' }]} />
                <View style={[styles.miniAppDot, { backgroundColor: '#22C55E' }]} />
              </View>
            </View>
          </View>

          {/* Rule 2: CFS / GCD Scheduler */}
          <View style={styles.ruleItemRow}>
            <View style={[styles.ruleIndexCircle, { backgroundColor: '#3B82F6' }]}>
              <Text style={styles.ruleIndexText}>2</Text>
            </View>
            <View style={styles.ruleTextCol}>
              <Text style={styles.ruleTitle}>CFS / GCD Scheduler</Text>
              <Text style={styles.ruleDescription}>
                The OS allocates a tiny 10ms time slice to each app so all run smoothly.
              </Text>
            </View>
            {/* Visual Micro-Graphic: Stopwatch Clock */}
            <View style={styles.ruleGraphicBox}>
              <View style={styles.stopwatchMiniDial}>
                <Ionicons name="timer-outline" size={26} color="#3B82F6" />
                <View style={styles.sliceArcBadge}>
                  <Text style={styles.sliceArcText}>10ms</Text>
                </View>
              </View>
            </View>
          </View>

          {/* Rule 3: Task Priority */}
          <View style={styles.ruleItemRow}>
            <View style={[styles.ruleIndexCircle, { backgroundColor: '#F59E0B' }]}>
              <Text style={styles.ruleIndexText}>3</Text>
            </View>
            <View style={styles.ruleTextCol}>
              <Text style={styles.ruleTitle}>Task Priority</Text>
              <Text style={styles.ruleDescription}>
                Critical apps (like audio playback) are prioritized to prevent interruptions.
              </Text>
            </View>
            {/* Visual Micro-Graphic: Priority Arrows */}
            <View style={styles.ruleGraphicBox}>
              <View style={styles.priorityMiniStack}>
                <Ionicons name="arrow-up" size={16} color="#22C55E" />
                <Ionicons name="arrow-up" size={16} color="#F59E0B" />
                <View style={styles.alertMiniOctagon}>
                  <Ionicons name="alert" size={12} color="#FFFFFF" />
                </View>
              </View>
            </View>
          </View>
        </View>
      </View>

      {/* =================================================================== */}
      {/* 2. HOST SYSTEM OVERVIEW (Clean Header, No Cluttered Badges)         */}
      {/* =================================================================== */}
      <View style={styles.cardSurface}>
        <View style={styles.sectionHeaderRow}>
          <View style={styles.sectionHeaderLeft}>
            <Ionicons name="shield-checkmark-outline" size={18} color="#3B82F6" />
            <Text style={styles.sectionTitle}>HOST SYSTEM OVERVIEW</Text>
          </View>
        </View>

        <View style={styles.telemetryGrid}>
          {/* Host OS & Architecture */}
          <View style={styles.telemetryTile}>
            <Text style={styles.tileLabel}>HOST OS & ARCHITECTURE</Text>
            <Text style={styles.tilePrimaryValue}>{telemetry.osName}</Text>
            <Text style={styles.tileSecondaryValue}>{telemetry.platformArch}</Text>
          </View>

          {/* Logical Concurrency */}
          <View style={styles.telemetryTile}>
            <Text style={styles.tileLabel}>LOGICAL CONCURRENCY</Text>
            <Text style={styles.tilePrimaryValue}>{telemetry.cpuCores} Threads</Text>
            <Text style={styles.tileSecondaryValue}>{telemetry.runtimeEnvironment}</Text>
          </View>

          {/* Native Kernel Scheduler */}
          <View style={[styles.telemetryTile, styles.telemetryTileFull]}>
            <Text style={styles.tileLabel}>NATIVE KERNEL SCHEDULER</Text>
            <Text style={styles.tileHighlightValue}>{telemetry.schedulingAlgorithmName}</Text>
          </View>
        </View>
      </View>

      {/* =================================================================== */}
      {/* 3. DYNAMIC SIMULATION ENGINE & LIVE GANTT TIMELINE                  */}
      {/* =================================================================== */}
      <View style={styles.cardSurface}>
        <View style={styles.sectionHeaderRow}>
          <View style={styles.sectionHeaderLeft}>
            <Ionicons name="hardware-chip-outline" size={18} color={Colors.studioOrange} />
            <Text style={styles.sectionTitle}>CPU SCHEDULER SIMULATION</Text>
          </View>

          {/* Current Simulation Clock Badge */}
          <View style={styles.clockBadge}>
            <Text style={styles.clockLabel}>T = </Text>
            <Text style={styles.clockValue}>{currentTime} ms</Text>
          </View>
        </View>

        {/* Dynamic Native Kernel Simulation Indicator (Replaces generic tabs) */}
        <View style={styles.nativeLogicBanner}>
          <View style={styles.nativeLogicLeft}>
            <Ionicons name="git-branch-outline" size={14} color="#38BDF8" />
            <Text style={styles.nativeLogicTag}>SIMULATING NATIVE KERNEL LOGIC</Text>
          </View>
          <Text style={styles.nativeLogicDetail} numberOfLines={1}>
            {telemetry.schedulingAlgorithmName}
          </Text>
        </View>

        {/* Control Bar: Auto Run / Pause, Step, Reset */}
        <View style={styles.controlsRow}>
          <TouchableOpacity
            style={[styles.primaryActionBtn, isPlaying && styles.primaryActionBtnActive]}
            onPress={() => setIsPlaying(!isPlaying)}
            activeOpacity={0.8}
          >
            <Ionicons
              name={isPlaying ? 'pause' : 'play'}
              size={15}
              color="#FFFFFF"
            />
            <Text style={styles.primaryActionBtnText}>
              {isPlaying ? 'PAUSE' : 'AUTO RUN'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.secondaryActionBtn, isAllComplete && styles.btnDisabled]}
            onPress={stepClockTick}
            disabled={isAllComplete}
            activeOpacity={0.8}
          >
            <Ionicons
              name="play-forward"
              size={15}
              color={isAllComplete ? '#6B7280' : '#FFFFFF'}
            />
            <Text
              style={[
                styles.secondaryActionBtnText,
                isAllComplete && { color: '#6B7280' },
              ]}
            >
              STEP (+1ms)
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.resetBtn}
            onPress={resetSimulation}
            activeOpacity={0.8}
          >
            <Ionicons name="refresh" size={15} color="#FFA07A" />
            <Text style={styles.resetBtnText}>RESET</Text>
          </TouchableOpacity>
        </View>

        {/* Live Gantt Chart Timeline with Continuous Partition Axis */}
        <View style={styles.ganttContainer}>
          <View style={styles.ganttHeader}>
            <Text style={styles.ganttTitle}>GANTT TIMELINE BAR</Text>
            <Text style={styles.ganttSubtitle}>
              {ganttTimeline.length === 0
                ? 'Queue initialized at T=0. Press Step or Auto Run to dispatch CPU.'
                : `${ganttTimeline.length} Time slices executed • ${contextSwitches} Context Switches`}
            </Text>
          </View>

          <ScrollView
            ref={ganttScrollRef}
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.ganttScrollContent}
          >
            {ganttTimeline.length === 0 ? (
              <View style={styles.emptyGanttPlaceholder}>
                <Ionicons name="time-outline" size={18} color="#6B7280" />
                <Text style={styles.emptyGanttText}>
                  Simulation queue ready. Click "STEP (+1ms)" to dispatch CPU.
                </Text>
              </View>
            ) : (
              <View style={styles.ganttFullWrap}>
                {/* 1. Slices Bar Row */}
                <View style={styles.ganttBlocksRow}>
                  {ganttTimeline.map((slice, index) => {
                    const isLatest = index === ganttTimeline.length - 1;
                    const duration = slice.endTime - slice.startTime;
                    const width = Math.max(54, duration * 26);

                    return (
                      <View
                        key={slice.id}
                        style={[
                          styles.ganttSliceBar,
                          { width, backgroundColor: slice.color },
                          isLatest && !isAllComplete && styles.ganttSliceActive,
                        ]}
                      >
                        <Text style={styles.ganttSlicePid} numberOfLines={1}>
                          {slice.processId}
                        </Text>
                        <Text style={styles.ganttSliceDuration} numberOfLines={1}>
                          {duration}ms
                        </Text>
                      </View>
                    );
                  })}
                </View>

                {/* 2. Continuous Partition Axis Row (| 0 | 4 | 7 | 10 | 14 |) */}
                <View style={styles.ganttAxisRow}>
                  {/* Origin tick (T=0) */}
                  <View style={styles.ganttOriginMarker}>
                    <View style={styles.ganttTickMark} />
                    <Text style={styles.ganttTickLabel}>
                      {ganttTimeline[0].startTime}
                    </Text>
                  </View>

                  {/* Boundary markers for each partition */}
                  {ganttTimeline.map((slice) => {
                    const duration = slice.endTime - slice.startTime;
                    const width = Math.max(54, duration * 26);

                    return (
                      <View
                        key={`tick-${slice.id}`}
                        style={[styles.ganttAxisSegment, { width }]}
                      >
                        <View style={styles.ganttAxisDividerLine} />
                        <View style={styles.ganttAxisTickContainer}>
                          <View style={styles.ganttTickMark} />
                          <Text style={styles.ganttTickLabel}>{slice.endTime}</Text>
                        </View>
                      </View>
                    );
                  })}
                </View>
              </View>
            )}
          </ScrollView>
        </View>
      </View>

      {/* =================================================================== */}
      {/* 4. PROCESS EXECUTION METRICS (Clean Table, No Overlapping Formula)  */}
      {/* =================================================================== */}
      <View style={styles.cardSurface}>
        <View style={styles.sectionHeaderRow}>
          <View style={styles.sectionHeaderLeft}>
            <Ionicons name="stats-chart-outline" size={18} color="#22C55E" />
            <Text style={styles.sectionTitle}>PROCESS EXECUTION METRICS</Text>
          </View>
        </View>

        {/* Clean Process Metrics Table */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          <View style={styles.tableWrap}>
            {/* Table Header */}
            <View style={styles.tableHeaderRow}>
              <Text style={[styles.th, styles.colProcess]}>Process ID</Text>
              <Text style={[styles.th, styles.colPriority]}>Priority / Weight</Text>
              <Text style={[styles.th, styles.colBurst]}>Burst Time</Text>
              <Text style={[styles.th, styles.colNum]}>CT</Text>
              <Text style={[styles.th, styles.colNum]}>TAT</Text>
              <Text style={[styles.th, styles.colNum]}>WT</Text>
            </View>

            {/* Table Rows */}
            {INITIAL_PROCESSES.map((proc) => {
              const state = processStates.find((ps) => ps.id === proc.id)!;
              const isRunning =
                ganttTimeline.length > 0 &&
                ganttTimeline[ganttTimeline.length - 1].processId === proc.id &&
                !isAllComplete;
              const isCompleted = state.remainingBurst === 0;

              // Render priority/weight column based on detected OS scheduler
              let priorityDisplay = `P${proc.basePriority}`;
              let prioritySub = 'Base';

              if (telemetry.schedulerFamily === 'WINDOWS_NT_BOOST') {
                if (state.boostTicksRemaining > 0) {
                  priorityDisplay = `P${state.effectivePriority}`;
                  prioritySub = '+1 Boost';
                } else {
                  priorityDisplay = `P${proc.basePriority}`;
                  prioritySub = proc.basePriority === 1 ? 'Realtime' : 'Normal';
                }
              } else if (telemetry.schedulerFamily === 'LINUX_CFS_EEVDF') {
                priorityDisplay = `Nice ${proc.nice > 0 ? '+' : ''}${proc.nice}`;
                prioritySub = `v: ${state.vruntime.toFixed(1)}ms`;
              } else {
                priorityDisplay = `Q${state.currentQueue}`;
                prioritySub =
                  state.currentQueue === 0
                    ? '1ms slice'
                    : state.currentQueue === 1
                    ? '2ms slice'
                    : '4ms batch';
              }

              return (
                <View
                  key={proc.id}
                  style={[
                    styles.tableDataRow,
                    isRunning && styles.tableDataRowActive,
                  ]}
                >
                  {/* Process Name & Badge */}
                  <View style={[styles.td, styles.colProcess]}>
                    <View style={[styles.processDot, { backgroundColor: proc.color }]} />
                    <View style={{ flex: 1 }}>
                      <Text style={styles.tdProcessName}>
                        {proc.id} • {proc.shortName}
                      </Text>
                      <Text style={styles.tdCategory} numberOfLines={1}>
                        {proc.category}
                      </Text>
                    </View>
                  </View>

                  {/* Priority / Weight */}
                  <View style={[styles.td, styles.colPriority]}>
                    <View style={styles.priorityBox}>
                      <Text style={styles.priorityBoxTitle}>{priorityDisplay}</Text>
                      <Text style={styles.priorityBoxSub}>{prioritySub}</Text>
                    </View>
                  </View>

                  {/* Burst Time (Total & Remaining) */}
                  <View style={[styles.td, styles.colBurst]}>
                    <Text style={styles.tdBurstTotal}>{proc.burstTime}ms</Text>
                    <Text
                      style={[
                        styles.tdBurstRem,
                        { color: isCompleted ? '#6B7280' : proc.color },
                      ]}
                    >
                      {isCompleted ? 'Done' : `${state.remainingBurst}ms left`}
                    </Text>
                  </View>

                  {/* Completion Time (CT) */}
                  <Text style={[styles.tdMono, styles.colNum]}>
                    {state.completionTime !== null ? `${state.completionTime}ms` : '--'}
                  </Text>

                  {/* Turnaround Time (TAT) */}
                  <Text style={[styles.tdMono, styles.colNum]}>
                    {state.turnaroundTime !== null ? `${state.turnaroundTime}ms` : '--'}
                  </Text>

                  {/* Waiting Time (WT) */}
                  <Text style={[styles.tdMono, styles.colNum]}>
                    {state.waitingTime !== null ? `${state.waitingTime}ms` : '--'}
                  </Text>
                </View>
              );
            })}
          </View>
        </ScrollView>

        {/* Summary Footer with Aggregate Averages */}
        <View style={styles.metricsSummaryFooter}>
          <View style={styles.metricSummaryTile}>
            <Text style={styles.summaryLabel}>AVG TURNAROUND</Text>
            <Text style={styles.summaryValue}>
              {avgTurnaround} {avgTurnaround !== '--' && 'ms'}
            </Text>
          </View>

          <View style={styles.metricSummaryTile}>
            <Text style={styles.summaryLabel}>AVG WAITING TIME</Text>
            <Text style={styles.summaryValue}>
              {avgWaiting} {avgWaiting !== '--' && 'ms'}
            </Text>
          </View>

          <View style={styles.metricSummaryTile}>
            <Text style={styles.summaryLabel}>CPU UTILIZATION</Text>
            <Text style={[styles.summaryValue, { color: '#22C55E' }]}>
              {cpuUtilization}%
            </Text>
          </View>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    gap: 16,
  },
  cardSurface: {
    backgroundColor: '#161A22',
    borderRadius: 18,
    padding: 18,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.07)',
    gap: 14,
  },

  // ---------------------------------------------------------------------------
  // 1. CPU Scheduling Rules Styles
  // ---------------------------------------------------------------------------
  rulesHeader: {
    gap: 3,
  },
  rulesTitle: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  rulesSubtitle: {
    ...Typography.bodySmall,
    color: '#9CA3AF',
    fontSize: 12,
  },
  rulesList: {
    gap: 10,
  },
  ruleItemRow: {
    backgroundColor: '#1A1F29',
    borderRadius: 14,
    padding: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  ruleIndexCircle: {
    width: 26,
    height: 26,
    borderRadius: 13,
    justifyContent: 'center',
    alignItems: 'center',
  },
  ruleIndexText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },
  ruleTextCol: {
    flex: 1,
    gap: 2,
  },
  ruleTitle: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
  },
  ruleDescription: {
    ...Typography.bodySmall,
    color: '#9CA3AF',
    fontSize: 11,
    lineHeight: 15,
  },
  ruleGraphicBox: {
    width: 62,
    height: 44,
    backgroundColor: '#12151B',
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  cpuMiniChip: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  cpuMiniText: {
    color: '#22C55E',
    fontSize: 7,
    fontWeight: '700',
    marginTop: -2,
  },
  cpuMiniTasksCol: {
    marginLeft: 6,
    gap: 4,
  },
  miniAppDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  stopwatchMiniDial: {
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  sliceArcBadge: {
    position: 'absolute',
    backgroundColor: 'rgba(59, 130, 246, 0.25)',
    paddingHorizontal: 3,
    borderRadius: 3,
    bottom: -4,
  },
  sliceArcText: {
    color: '#3B82F6',
    fontSize: 7,
    fontFamily: FontFamilies.mono,
    fontWeight: '600',
  },
  priorityMiniStack: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
  alertMiniOctagon: {
    width: 16,
    height: 16,
    borderRadius: 8,
    backgroundColor: '#EA580C',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 2,
  },

  // ---------------------------------------------------------------------------
  // 2. Host Overview Styles (Clean, No Status Pills)
  // ---------------------------------------------------------------------------
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  sectionHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  sectionTitle: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  telemetryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  telemetryTile: {
    flex: 1,
    minWidth: 140,
    backgroundColor: '#1A1F29',
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    gap: 2,
  },
  telemetryTileFull: {
    flexBasis: '100%',
  },
  tileLabel: {
    ...Typography.tag,
    color: '#6B7280',
    fontSize: 9,
    fontWeight: '700',
  },
  tilePrimaryValue: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
  },
  tileSecondaryValue: {
    ...Typography.bodySmall,
    color: '#9CA3AF',
    fontSize: 11,
  },
  tileHighlightValue: {
    ...Typography.subhead,
    color: '#38BDF8',
    fontSize: 12,
    fontWeight: '700',
  },

  // ---------------------------------------------------------------------------
  // 3. Scheduler Engine & Live Gantt Styles
  // ---------------------------------------------------------------------------
  clockBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(234, 88, 12, 0.15)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(234, 88, 12, 0.3)',
  },
  clockLabel: {
    ...Typography.tag,
    color: '#FFA07A',
    fontSize: 10,
    fontWeight: '700',
  },
  clockValue: {
    color: '#FFFFFF',
    fontSize: 11,
    fontFamily: FontFamilies.mono,
    fontWeight: '600',
  },
  nativeLogicBanner: {
    backgroundColor: '#12151B',
    borderRadius: 10,
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderWidth: 1,
    borderColor: 'rgba(56, 189, 248, 0.2)',
    gap: 3,
  },
  nativeLogicLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  nativeLogicTag: {
    ...Typography.tag,
    color: '#38BDF8',
    fontSize: 9,
    fontWeight: '600',
    letterSpacing: 0.5,
  },
  nativeLogicDetail: {
    color: '#E5E7EB',
    fontSize: 11,
    fontWeight: '600',
  },
  controlsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  primaryActionBtn: {
    flex: 1.2,
    backgroundColor: '#EA580C',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 10,
  },
  primaryActionBtnActive: {
    backgroundColor: '#DC2626',
  },
  primaryActionBtnText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '600',
  },
  secondaryActionBtn: {
    flex: 1.2,
    backgroundColor: '#1E293B',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  secondaryActionBtnText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
  },
  btnDisabled: {
    opacity: 0.5,
  },
  resetBtn: {
    flex: 0.8,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    paddingVertical: 10,
    borderRadius: 10,
  },
  resetBtnText: {
    ...Typography.tag,
    color: '#FFA07A',
    fontSize: 10,
    fontWeight: '700',
  },
  ganttContainer: {
    backgroundColor: '#12151B',
    borderRadius: 14,
    padding: 12,
    gap: 10,
  },
  ganttHeader: {
    gap: 2,
  },
  ganttTitle: {
    ...Typography.tag,
    color: '#9CA3AF',
    fontSize: 9,
    fontWeight: '600',
    letterSpacing: 0.5,
  },
  ganttSubtitle: {
    ...Typography.bodySmall,
    color: '#6B7280',
    fontSize: 10,
  },
  ganttScrollContent: {
    paddingVertical: 6,
    alignItems: 'center',
  },
  emptyGanttPlaceholder: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 14,
    paddingHorizontal: 10,
  },
  emptyGanttText: {
    ...Typography.bodySmall,
    color: '#6B7280',
    fontSize: 11,
  },
  ganttFullWrap: {
    flexDirection: 'column',
    position: 'relative',
  },
  ganttBlocksRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  ganttSliceBar: {
    height: 38,
    borderRadius: 4,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
    marginRight: 1,
    borderRightWidth: 1,
    borderColor: 'rgba(0, 0, 0, 0.3)',
  },
  ganttSliceActive: {
    borderWidth: 2,
    borderColor: '#FFFFFF',
  },
  ganttSlicePid: {
    color: '#FFFFFF',
    fontSize: 10,
    fontFamily: FontFamilies.mono,
    fontWeight: '600',
  },
  ganttSliceDuration: {
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: 8,
    fontFamily: FontFamilies.mono,
    fontWeight: '500',
  },
  ganttAxisRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginTop: 4,
    position: 'relative',
    height: 22,
  },
  ganttOriginMarker: {
    position: 'absolute',
    left: 0,
    top: 0,
    width: 24,
    marginLeft: -12,
    alignItems: 'center',
    zIndex: 2,
  },
  ganttAxisSegment: {
    position: 'relative',
    height: 22,
    justifyContent: 'flex-end',
    alignItems: 'flex-end',
    marginRight: 1,
  },
  ganttAxisDividerLine: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    height: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
  },
  ganttAxisTickContainer: {
    position: 'absolute',
    right: -12,
    top: 0,
    width: 24,
    alignItems: 'center',
    zIndex: 2,
  },
  ganttTickMark: {
    width: 1,
    height: 5,
    backgroundColor: '#9CA3AF',
    marginBottom: 2,
  },
  ganttTickLabel: {
    color: '#9CA3AF',
    fontSize: 9,
    fontFamily: FontFamilies.mono,
    textAlign: 'center',
  },

  // ---------------------------------------------------------------------------
  // 4. Metrics Table Styles
  // ---------------------------------------------------------------------------
  tableWrap: {
    width: 480,
    gap: 4,
  },
  tableHeaderRow: {
    flexDirection: 'row',
    backgroundColor: '#12151B',
    paddingVertical: 8,
    paddingHorizontal: 8,
    borderRadius: 8,
    alignItems: 'center',
  },
  tableDataRow: {
    flexDirection: 'row',
    backgroundColor: '#1A1F29',
    paddingVertical: 8,
    paddingHorizontal: 8,
    borderRadius: 8,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'transparent',
  },
  tableDataRowActive: {
    borderColor: 'rgba(234, 88, 12, 0.6)',
    backgroundColor: 'rgba(234, 88, 12, 0.08)',
  },
  th: {
    ...Typography.tag,
    color: '#6B7280',
    fontSize: 9,
    fontWeight: '700',
  },
  td: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  tdMono: {
    color: '#E5E7EB',
    fontSize: 10,
    fontFamily: FontFamilies.mono,
    textAlign: 'center',
  },
  colProcess: {
    width: 155,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  colPriority: {
    width: 95,
    justifyContent: 'center',
  },
  colBurst: {
    width: 75,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
  },
  colNum: {
    width: 48,
    textAlign: 'center',
    justifyContent: 'center',
    alignItems: 'center',
  },
  processDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  tdProcessName: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
  },
  tdCategory: {
    ...Typography.bodySmall,
    color: '#9CA3AF',
    fontSize: 8,
  },
  priorityBox: {
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 5,
    alignItems: 'center',
  },
  priorityBoxTitle: {
    color: '#FFA07A',
    fontSize: 9,
    fontWeight: '700',
  },
  priorityBoxSub: {
    color: '#9CA3AF',
    fontSize: 7,
  },
  tdBurstTotal: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '600',
    fontFamily: FontFamilies.mono,
  },
  tdBurstRem: {
    fontSize: 8,
    fontWeight: '600',
  },
  metricsSummaryFooter: {
    flexDirection: 'row',
    backgroundColor: '#12151B',
    borderRadius: 12,
    padding: 10,
    marginTop: 4,
    gap: 8,
  },
  metricSummaryTile: {
    flex: 1,
    alignItems: 'center',
    gap: 2,
  },
  summaryLabel: {
    ...Typography.tag,
    color: '#6B7280',
    fontSize: 8,
    fontWeight: '700',
  },
  summaryValue: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '600',
    fontFamily: FontFamilies.mono,
  },
});
