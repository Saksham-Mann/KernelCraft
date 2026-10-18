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
  Animated,
} from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

export type SchedulingAlgorithm = 'PRIORITY' | 'ROUND_ROBIN' | 'CFS';

interface ProcessDef {
  id: string;
  name: string;
  shortName: string;
  category: string;
  arrivalTime: number;
  burstTime: number;
  priority: number; // 1 = highest, 4 = lowest
  color: string;
}

interface ProcessRuntimeState {
  id: string;
  remainingBurst: number;
  completionTime: number | null;
  turnaroundTime: number | null;
  waitingTime: number | null;
  started: boolean;
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
  osName: string;
  osVersion: string;
  platformArch: string;
  cpuCores: number | string;
  screenResolution: string;
  pixelRatio: string;
  schedulingAlgorithmName: string;
  runtimeEnvironment: string;
  algorithmKey: SchedulingAlgorithm;
}

const INITIAL_PROCESSES: ProcessDef[] = [
  {
    id: 'P1',
    name: 'Audio Playback Daemon',
    shortName: 'Audio',
    category: 'Real-Time Audio Buffer',
    arrivalTime: 0,
    burstTime: 4,
    priority: 1,
    color: '#22C55E', // Green
  },
  {
    id: 'P2',
    name: 'UI Frame Compositor',
    shortName: 'UI Render',
    category: 'Display Refresh Pipeline',
    arrivalTime: 1,
    burstTime: 5,
    priority: 2,
    color: '#3B82F6', // Blue
  },
  {
    id: 'P3',
    name: 'Application Logic Worker',
    shortName: 'App Logic',
    category: 'User Input Handler',
    arrivalTime: 2,
    burstTime: 3,
    priority: 3,
    color: '#F59E0B', // Amber
  },
  {
    id: 'P4',
    name: 'Cloud Data Synchronization',
    shortName: 'Sync Worker',
    category: 'Background Network Batch',
    arrivalTime: 4,
    burstTime: 4,
    priority: 4,
    color: '#A855F7', // Purple
  },
];

export const MyDeviceInspector: React.FC = () => {
  // ---------------------------------------------------------------------------
  // 1. Host OS Telemetry Detection
  // ---------------------------------------------------------------------------
  const [telemetry, setTelemetry] = useState<HostTelemetry>({
    osName: 'Detecting...',
    osVersion: '',
    platformArch: 'Detecting...',
    cpuCores: 'Detecting...',
    screenResolution: 'Detecting...',
    pixelRatio: '1.0x',
    schedulingAlgorithmName: 'CFS / Preemptive Priority',
    runtimeEnvironment: 'Detecting...',
    algorithmKey: 'PRIORITY',
  });

  const [selectedAlgorithm, setSelectedAlgorithm] = useState<SchedulingAlgorithm>('PRIORITY');

  useEffect(() => {
    const screen = Dimensions.get('screen');
    const pr = PixelRatio.get();

    let os = Platform.OS.toUpperCase();
    let version = String(Platform.Version || '');
    let arch = 'ARM64 / x86_64';
    let cores: number | string = 8;
    let schedName = 'CFS (Completely Fair Scheduler) / EEVDF';
    let runtime = 'Native Runtime';
    let detectedAlgo: SchedulingAlgorithm = 'CFS';

    if (Platform.OS === 'web' && typeof window !== 'undefined' && typeof navigator !== 'undefined') {
      const ua = navigator.userAgent;
      const uaData = (navigator as any).userAgentData;

      // Platform architecture
      if (/Win64|x64|WOW64|x86_64/i.test(ua)) {
        arch = 'x86_64 (64-bit)';
      } else if (/arm64|aarch64/i.test(ua)) {
        arch = 'ARM64';
      }

      // OS Detection & Native Scheduling mapping
      if (/Windows/i.test(ua) || uaData?.platform === 'Windows') {
        os = 'Windows';
        version = '11 / 10';
        schedName = 'Multithreaded Priority-Driven Preemptive Scheduler';
        detectedAlgo = 'PRIORITY';
      } else if (/Macintosh|Mac OS X/i.test(ua) || uaData?.platform === 'macOS') {
        os = 'macOS';
        schedName = 'Mach Multilevel Feedback Queue (MLFQ) & GCD';
        detectedAlgo = 'ROUND_ROBIN';
        if (!/x86_64/i.test(ua)) arch = 'Apple Silicon (ARM64)';
      } else if (/Android/i.test(ua) || uaData?.platform === 'Android') {
        os = 'Android (Linux Kernel)';
        schedName = 'CFS (Completely Fair Scheduler) / EEVDF + EAS';
        detectedAlgo = 'CFS';
        arch = 'ARMv9 / ARMv8 64-bit';
      } else if (/iPhone|iPad|iPod/i.test(ua) || uaData?.platform === 'iOS') {
        os = 'iOS';
        schedName = 'Mach Multilevel Feedback Queue (MLFQ) & GCD';
        detectedAlgo = 'ROUND_ROBIN';
        arch = 'Apple Bionic ARM64';
      } else if (/Linux/i.test(ua) || uaData?.platform === 'Linux') {
        os = 'Linux';
        schedName = 'CFS (Completely Fair Scheduler) / EEVDF';
        detectedAlgo = 'CFS';
      }

      if (navigator.hardwareConcurrency) {
        cores = navigator.hardwareConcurrency;
      }

      if (/Chrome/i.test(ua)) {
        runtime = 'Chromium Engine (V8)';
      } else if (/Safari/i.test(ua)) {
        runtime = 'WebKit Engine (Nitro)';
      } else if (/Firefox/i.test(ua)) {
        runtime = 'Gecko Engine (SpiderMonkey)';
      } else if (/Edge/i.test(ua)) {
        runtime = 'Chromium Edge';
      } else {
        runtime = 'Expo Web Container';
      }
    } else {
      // Native iOS / Android
      if (Platform.OS === 'android') {
        os = 'Android';
        schedName = 'CFS (Completely Fair Scheduler) / EEVDF + EAS';
        detectedAlgo = 'CFS';
        arch = 'ARM64';
        runtime = 'Android Runtime (ART)';
      } else if (Platform.OS === 'ios') {
        os = 'iOS';
        schedName = 'Mach Multilevel Feedback Queue (MLFQ) & GCD';
        detectedAlgo = 'ROUND_ROBIN';
        arch = 'Apple Silicon';
        runtime = 'Darwin / XNU';
      }
    }

    const screenRes = `${Math.round(screen.width * pr)} x ${Math.round(screen.height * pr)} px`;

    setTelemetry({
      osName: os,
      osVersion: version,
      platformArch: arch,
      cpuCores: cores,
      screenResolution: screenRes,
      pixelRatio: `${pr.toFixed(1)}x`,
      schedulingAlgorithmName: schedName,
      runtimeEnvironment: runtime,
      algorithmKey: detectedAlgo,
    });

    setSelectedAlgorithm(detectedAlgo);
  }, []);

  // ---------------------------------------------------------------------------
  // 2. Interactive CPU Scheduling Engine
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
    }))
  );

  const [ganttTimeline, setGanttTimeline] = useState<GanttSlice[]>([]);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [contextSwitches, setContextSwitches] = useState<number>(0);

  // Round robin queue tracker: queue of process IDs
  const rrQueueRef = useRef<string[]>([]);
  const rrQuantumRemainingRef = useRef<number>(2);
  const rrCurrentProcRef = useRef<string | null>(null);

  // Auto-scroll Gantt chart
  const ganttScrollRef = useRef<ScrollView>(null);

  // Reset simulation state
  const resetSimulation = useCallback(() => {
    setIsPlaying(false);
    setCurrentTime(0);
    setContextSwitches(0);
    setGanttTimeline([]);
    rrQueueRef.current = [];
    rrQuantumRemainingRef.current = 2;
    rrCurrentProcRef.current = null;
    setProcessStates(
      INITIAL_PROCESSES.map((p) => ({
        id: p.id,
        remainingBurst: p.burstTime,
        completionTime: null,
        turnaroundTime: null,
        waitingTime: null,
        started: false,
      }))
    );
  }, []);

  // Change algorithm and reset
  const handleAlgorithmChange = (algo: SchedulingAlgorithm) => {
    setSelectedAlgorithm(algo);
    resetSimulation();
  };

  // Execute 1 Clock Tick (1 ms CPU step)
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
        if (selectedAlgorithm === 'PRIORITY') {
          // Preemptive Priority: lowest priority value = highest priority
          const sorted = [...arrived].sort((a, b) => {
            const defA = INITIAL_PROCESSES.find((p) => p.id === a.id)!;
            const defB = INITIAL_PROCESSES.find((p) => p.id === b.id)!;
            if (defA.priority !== defB.priority) {
              return defA.priority - defB.priority;
            }
            return defA.arrivalTime - defB.arrivalTime;
          });
          chosenId = sorted[0].id;
        } else if (selectedAlgorithm === 'CFS') {
          // CFS / Shortest Remaining Time First (SRTF): smallest remaining burst runs first
          const sorted = [...arrived].sort((a, b) => {
            if (a.remainingBurst !== b.remainingBurst) {
              return a.remainingBurst - b.remainingBurst;
            }
            const defA = INITIAL_PROCESSES.find((p) => p.id === a.id)!;
            const defB = INITIAL_PROCESSES.find((p) => p.id === b.id)!;
            return defA.arrivalTime - defB.arrivalTime;
          });
          chosenId = sorted[0].id;
        } else if (selectedAlgorithm === 'ROUND_ROBIN') {
          // Round Robin: Quantum = 2ms
          // Maintain FIFO queue
          const queue = rrQueueRef.current;
          // Add any newly arrived processes not yet in queue
          for (const item of arrived) {
            if (!queue.includes(item.id) && rrCurrentProcRef.current !== item.id) {
              queue.push(item.id);
            }
          }

          if (rrCurrentProcRef.current && rrQuantumRemainingRef.current > 0) {
            const currentObj = arrived.find((p) => p.id === rrCurrentProcRef.current);
            if (currentObj && currentObj.remainingBurst > 0) {
              chosenId = rrCurrentProcRef.current;
              rrQuantumRemainingRef.current -= 1;
            } else {
              // Current finished, pop next
              chosenId = queue.shift() || null;
              rrCurrentProcRef.current = chosenId;
              rrQuantumRemainingRef.current = 1; // 1 used this tick
            }
          } else {
            // Quantum expired or no current: if current still has remaining, re-queue
            if (rrCurrentProcRef.current) {
              const currentObj = arrived.find((p) => p.id === rrCurrentProcRef.current);
              if (currentObj && currentObj.remainingBurst > 0) {
                queue.push(rrCurrentProcRef.current);
              }
            }
            chosenId = queue.shift() || null;
            rrCurrentProcRef.current = chosenId;
            rrQuantumRemainingRef.current = 1; // 1 used this tick
          }
        }
      }

      // Check context switch
      setGanttTimeline((prevGantt) => {
        const lastSlice = prevGantt[prevGantt.length - 1];

        if (chosenId) {
          const chosenDef = INITIAL_PROCESSES.find((p) => p.id === chosenId)!;

          if (lastSlice && lastSlice.processId === chosenId) {
            // Extend existing block
            const updated = [...prevGantt];
            updated[updated.length - 1] = {
              ...lastSlice,
              endTime: t + 1,
            };
            return updated;
          } else {
            // Context switch
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

      // Update state of the executing process
      const nextStates = prevStates.map((p) => {
        if (p.id === chosenId) {
          const newRemaining = Math.max(0, p.remainingBurst - 1);
          const isFinished = newRemaining === 0;
          const ct = isFinished ? t + 1 : null;
          const def = INITIAL_PROCESSES.find((item) => item.id === p.id)!;
          const tat = isFinished ? (ct! - def.arrivalTime) : null;
          const wt = isFinished ? (tat! - def.burstTime) : null;

          return {
            ...p,
            started: true,
            remainingBurst: newRemaining,
            completionTime: isFinished ? ct : p.completionTime,
            turnaroundTime: isFinished ? tat : p.turnaroundTime,
            waitingTime: isFinished ? wt : p.waitingTime,
          };
        }
        return p;
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
  }, [currentTime, selectedAlgorithm]);

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

  // Auto scroll Gantt timeline to end
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
      {/* 1. CPU SCHEDULING RULES (Reference: Image 4)                        */}
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
            {/* Visual Micro-Graphic: Priority Arrows & Alert */}
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
      {/* 2. HOST SYSTEM TELEMETRY (Clean, Streamlined Status Card)           */}
      {/* =================================================================== */}
      <View style={styles.cardSurface}>
        <View style={styles.sectionHeaderRow}>
          <View style={styles.sectionHeaderLeft}>
            <Ionicons name="shield-checkmark" size={18} color="#3B82F6" />
            <Text style={styles.sectionTitle}>HOST SYSTEM TELEMETRY</Text>
          </View>
          <View style={styles.activeEngineBadge}>
            <View style={styles.liveGreenDot} />
            <Text style={styles.activeEngineText}>LIVE TELEMETRY</Text>
          </View>
        </View>

        <View style={styles.telemetryGrid}>
          {/* OS & Architecture */}
          <View style={styles.telemetryTile}>
            <Text style={styles.tileLabel}>HOST OS & ARCHITECTURE</Text>
            <Text style={styles.tilePrimaryValue}>{telemetry.osName}</Text>
            <Text style={styles.tileSecondaryValue}>{telemetry.platformArch}</Text>
          </View>

          {/* CPU Hardware Concurrency */}
          <View style={styles.telemetryTile}>
            <Text style={styles.tileLabel}>LOGICAL CONCURRENCY</Text>
            <Text style={styles.tilePrimaryValue}>{telemetry.cpuCores} Threads</Text>
            <Text style={styles.tileSecondaryValue}>{telemetry.runtimeEnvironment}</Text>
          </View>

          {/* Native OS Scheduling Engine */}
          <View style={[styles.telemetryTile, styles.telemetryTileFull]}>
            <Text style={styles.tileLabel}>NATIVE KERNEL SCHEDULER</Text>
            <Text style={styles.tileHighlightValue}>{telemetry.schedulingAlgorithmName}</Text>
          </View>
        </View>
      </View>

      {/* =================================================================== */}
      {/* 3. INTERACTIVE CPU SCHEDULER ENGINE & LIVE GANTT CHART              */}
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

        {/* Algorithm Switcher */}
        <View style={styles.algoSelectorRow}>
          <TouchableOpacity
            style={[
              styles.algoTab,
              selectedAlgorithm === 'PRIORITY' && styles.algoTabActive,
            ]}
            onPress={() => handleAlgorithmChange('PRIORITY')}
            activeOpacity={0.8}
          >
            <Text
              style={[
                styles.algoTabText,
                selectedAlgorithm === 'PRIORITY' && styles.algoTabTextActive,
              ]}
            >
              Preemptive Priority
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.algoTab,
              selectedAlgorithm === 'ROUND_ROBIN' && styles.algoTabActive,
            ]}
            onPress={() => handleAlgorithmChange('ROUND_ROBIN')}
            activeOpacity={0.8}
          >
            <Text
              style={[
                styles.algoTabText,
                selectedAlgorithm === 'ROUND_ROBIN' && styles.algoTabTextActive,
              ]}
            >
              Round Robin (RR)
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.algoTab,
              selectedAlgorithm === 'CFS' && styles.algoTabActive,
            ]}
            onPress={() => handleAlgorithmChange('CFS')}
            activeOpacity={0.8}
          >
            <Text
              style={[
                styles.algoTabText,
                selectedAlgorithm === 'CFS' && styles.algoTabTextActive,
              ]}
            >
              CFS / SRTF
            </Text>
          </TouchableOpacity>
        </View>

        {/* Control Bar: Play / Pause, Next Step, Reset */}
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
            <Ionicons name="play-forward" size={15} color={isAllComplete ? '#6B7280' : '#FFFFFF'} />
            <Text style={[styles.secondaryActionBtnText, isAllComplete && { color: '#6B7280' }]}>
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

        {/* Live Gantt Chart Timeline */}
        <View style={styles.ganttContainer}>
          <View style={styles.ganttHeader}>
            <Text style={styles.ganttTitle}>GANTT TIMELINE BAR</Text>
            <Text style={styles.ganttSubtitle}>
              {ganttTimeline.length === 0
                ? 'Queue initialized at T=0. Press Step or Auto Run.'
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
                  Timeline ready. Click "STEP (+1ms)" to dispatch CPU.
                </Text>
              </View>
            ) : (
              ganttTimeline.map((slice, index) => {
                const isLatest = index === ganttTimeline.length - 1;
                const duration = slice.endTime - slice.startTime;
                const width = Math.max(54, duration * 26);

                return (
                  <View key={slice.id} style={[styles.ganttSliceWrap, { width }]}>
                    <View
                      style={[
                        styles.ganttSliceBar,
                        { backgroundColor: slice.color },
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
                    <View style={styles.ganttTimestampRow}>
                      <Text style={styles.timestampStart}>{slice.startTime}</Text>
                      <Text style={styles.timestampEnd}>{slice.endTime}</Text>
                    </View>
                  </View>
                );
              })
            )}
          </ScrollView>
        </View>
      </View>

      {/* =================================================================== */}
      {/* 4. PROCESS EXECUTION & METRICS TABLE                                */}
      {/* =================================================================== */}
      <View style={styles.cardSurface}>
        <View style={styles.sectionHeaderRow}>
          <View style={styles.sectionHeaderLeft}>
            <Ionicons name="stats-chart-outline" size={18} color="#22C55E" />
            <Text style={styles.sectionTitle}>PROCESS EXECUTION METRICS</Text>
          </View>
          <Text style={styles.formulaNote}>TAT = CT - AT  |  WT = TAT - BT</Text>
        </View>

        {/* Responsive Table Container */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          <View style={styles.tableWrap}>
            {/* Table Header */}
            <View style={styles.tableHeaderRow}>
              <Text style={[styles.th, styles.colProcess]}>Process (Task)</Text>
              <Text style={[styles.th, styles.colNum]}>AT</Text>
              <Text style={[styles.th, styles.colNum]}>BT</Text>
              <Text style={[styles.th, styles.colNum]}>Rem</Text>
              <Text style={[styles.th, styles.colNum]}>Prio</Text>
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
                      <Text style={styles.tdProcessName}>{proc.id} • {proc.shortName}</Text>
                      <Text style={styles.tdCategory} numberOfLines={1}>
                        {proc.category}
                      </Text>
                    </View>
                  </View>

                  {/* Arrival Time */}
                  <Text style={[styles.tdMono, styles.colNum]}>{proc.arrivalTime}ms</Text>

                  {/* Total Burst Time */}
                  <Text style={[styles.tdMono, styles.colNum]}>{proc.burstTime}ms</Text>

                  {/* Remaining Burst */}
                  <Text
                    style={[
                      styles.tdMono,
                      styles.colNum,
                      { color: isCompleted ? '#6B7280' : proc.color, fontWeight: '700' },
                    ]}
                  >
                    {state.remainingBurst}ms
                  </Text>

                  {/* Priority */}
                  <View style={[styles.td, styles.colNum]}>
                    <View style={styles.priorityPill}>
                      <Text style={styles.priorityPillText}>P{proc.priority}</Text>
                    </View>
                  </View>

                  {/* Completion Time */}
                  <Text style={[styles.tdMono, styles.colNum]}>
                    {state.completionTime !== null ? `${state.completionTime}ms` : '--'}
                  </Text>

                  {/* Turnaround Time */}
                  <Text style={[styles.tdMono, styles.colNum]}>
                    {state.turnaroundTime !== null ? `${state.turnaroundTime}ms` : '--'}
                  </Text>

                  {/* Waiting Time */}
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
    fontWeight: '800',
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
    fontWeight: '800',
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
    fontWeight: '800',
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
    fontWeight: '800',
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
  // 2. Host Telemetry Styles
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
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  activeEngineBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: 'rgba(34, 197, 94, 0.12)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(34, 197, 94, 0.25)',
  },
  liveGreenDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#22C55E',
  },
  activeEngineText: {
    ...Typography.tag,
    color: '#22C55E',
    fontSize: 9,
    fontWeight: '800',
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
  // 3. Scheduler Engine & Gantt Styles
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
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
  },
  algoSelectorRow: {
    flexDirection: 'row',
    backgroundColor: '#12151B',
    padding: 4,
    borderRadius: 12,
    gap: 4,
  },
  algoTab: {
    flex: 1,
    paddingVertical: 7,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
  },
  algoTabActive: {
    backgroundColor: '#2563EB',
  },
  algoTabText: {
    ...Typography.tag,
    color: '#9CA3AF',
    fontSize: 9,
    fontWeight: '700',
  },
  algoTabTextActive: {
    color: '#FFFFFF',
    fontWeight: '800',
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
    fontWeight: '800',
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
    fontWeight: '800',
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
    paddingVertical: 12,
    paddingHorizontal: 10,
  },
  emptyGanttText: {
    ...Typography.bodySmall,
    color: '#6B7280',
    fontSize: 11,
  },
  ganttSliceWrap: {
    marginRight: 2,
  },
  ganttSliceBar: {
    height: 36,
    borderRadius: 6,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  ganttSliceActive: {
    borderWidth: 2,
    borderColor: '#FFFFFF',
  },
  ganttSlicePid: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '800',
  },
  ganttSliceDuration: {
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: 8,
    fontWeight: '600',
  },
  ganttTimestampRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 3,
    paddingHorizontal: 1,
  },
  timestampStart: {
    color: '#9CA3AF',
    fontSize: 9,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
  },
  timestampEnd: {
    color: '#9CA3AF',
    fontSize: 9,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
  },

  // ---------------------------------------------------------------------------
  // 4. Metrics Table Styles
  // ---------------------------------------------------------------------------
  formulaNote: {
    ...Typography.tag,
    color: '#6B7280',
    fontSize: 8,
  },
  tableWrap: {
    width: 440,
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
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
    textAlign: 'center',
  },
  colProcess: {
    width: 140,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  colNum: {
    width: 42,
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
  priorityPill: {
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    paddingHorizontal: 5,
    paddingVertical: 1,
    borderRadius: 4,
  },
  priorityPillText: {
    color: '#FFA07A',
    fontSize: 8,
    fontWeight: '800',
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
    fontWeight: '800',
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' }),
  },
});
