import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Animated,
  Platform,
} from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

interface Choice {
  id: string;
  label: string; // "Option A", "Option B", "Option C"
  text: string;
  nextNodeId: string;
}

interface PipelineNode {
  id: string;
  stageNumber?: number;
  stageName: string;
  challenge: string;
  subtext?: string;
  icon: string;
  accentColor: string;
  choices: Choice[];
  isTerminal?: boolean;
  terminalType?: 'SUCCESS' | 'HALTED';
  terminalTitle?: string;
  terminalMessage?: string;
  technicalDiagnostic?: string;
}

const PIPELINE_NODES: Record<string, PipelineNode> = {
  NODE_COMPILATION: {
    id: 'NODE_COMPILATION',
    stageNumber: 1,
    stageName: '1. Compilation',
    challenge:
      'You just wrote your mobile app code. How does the system convert human-readable code into instructions the machine understands?',
    subtext: 'Phase: Source Code (.ts/.c) → Machine Bytecode',
    icon: 'code-slash',
    accentColor: '#34C759',
    choices: [
      {
        id: 'OPT_COMPILER',
        label: 'Option A',
        text: 'Run through Compiler & Assembler to produce object bytecode',
        nextNodeId: 'NODE_LINKING',
      },
      {
        id: 'OPT_RAW_TEXT',
        label: 'Option B',
        text: 'Send raw text files directly to the CPU registers',
        nextNodeId: 'HALTED_RAW_TEXT',
      },
      {
        id: 'OPT_IMAGE_CONTAINER',
        label: 'Option C',
        text: 'Wrap source code inside an image container',
        nextNodeId: 'HALTED_IMAGE_WRAPPER',
      },
    ],
  },
  NODE_LINKING: {
    id: 'NODE_LINKING',
    stageNumber: 2,
    stageName: '2. Linking',
    challenge:
      'Your compiled object code needs math and UI graphics frameworks. How are external functions connected?',
    subtext: 'Phase: Object Modules (.o) → Executable Binary',
    icon: 'git-merge-outline',
    accentColor: '#007AFF',
    choices: [
      {
        id: 'OPT_LINKER_RESOLVE',
        label: 'Option A',
        text: 'Linker resolves symbols and packages shared libraries into a binary',
        nextNodeId: 'NODE_LOADING',
      },
      {
        id: 'OPT_ZIP_ARCHIVE',
        label: 'Option B',
        text: 'Compress everything into a ZIP file without resolving references',
        nextNodeId: 'HALTED_UNRESOLVED_SYMBOL',
      },
    ],
  },
  NODE_LOADING: {
    id: 'NODE_LOADING',
    stageNumber: 3,
    stageName: '3. Memory Loading',
    challenge:
      'The executable binary is resting on flash storage. What brings it into active RAM?',
    subtext: 'Phase: Storage Flash → Virtual Memory Pages',
    icon: 'layers-outline',
    accentColor: '#AF52DE',
    choices: [
      {
        id: 'OPT_OS_LOADER',
        label: 'Option A',
        text: 'OS Loader allocates memory space and maps binary segments into RAM',
        nextNodeId: 'NODE_PRIVILEGE',
      },
      {
        id: 'OPT_GPU_DRAW',
        label: 'Option B',
        text: 'GPU draws the binary straight to the display buffer',
        nextNodeId: 'HALTED_GPU_MISMATCH',
      },
    ],
  },
  NODE_PRIVILEGE: {
    id: 'NODE_PRIVILEGE',
    stageNumber: 4,
    stageName: '4. Privilege Boundary',
    challenge:
      'The app is in RAM running in User Space, but needs to access the hardware camera. How does it cross the security boundary?',
    subtext: 'Phase: User Mode (Ring 3) → Supervisor Kernel Mode (Ring 0)',
    icon: 'shield-checkmark-outline',
    accentColor: '#FF9500',
    choices: [
      {
        id: 'OPT_SVC_SYSCALL',
        label: 'Option A',
        text: 'Execute an SVC Trap / System Call to request Kernel Space handling',
        nextNodeId: 'NODE_SCHEDULING',
      },
      {
        id: 'OPT_DIRECT_HW_MANIP',
        label: 'Option B',
        text: 'Directly manipulate the camera hardware pins from User Space',
        nextNodeId: 'HALTED_SIGSEGV',
      },
    ],
  },
  NODE_SCHEDULING: {
    id: 'NODE_SCHEDULING',
    stageNumber: 5,
    stageName: '5. CPU Scheduling',
    challenge:
      'The kernel accepted the request, but multiple background apps are competing for CPU cores. Who decides execution priority?',
    subtext: 'Phase: Task Descriptors & Completely Fair Scheduler (CFS)',
    icon: 'speedometer-outline',
    accentColor: '#FF2D55',
    choices: [
      {
        id: 'OPT_SCHEDULER_CFS',
        label: 'Option A',
        text: 'The CPU Scheduler allocates time slices via CFS / Priority Queues',
        nextNodeId: 'TERMINAL_SUCCESS',
      },
      {
        id: 'OPT_MONOPOLY_CPU',
        label: 'Option B',
        text: 'First-installed app monopolizes the CPU permanently',
        nextNodeId: 'HALTED_STARVATION',
      },
    ],
  },

  // Terminal Success Node (0 Choices)
  TERMINAL_SUCCESS: {
    id: 'TERMINAL_SUCCESS',
    stageNumber: 6,
    stageName: '6. Success',
    challenge: 'Execution Pipeline Complete',
    icon: 'checkmark-done-circle',
    accentColor: '#34C759',
    choices: [],
    isTerminal: true,
    terminalType: 'SUCCESS',
    terminalTitle: 'SUCCESS: App Rendered on Display!',
    terminalMessage:
      'App rendered onto the display! Pipeline executed flawlessly. From human-readable source code to assembler bytecode, symbol linking, ELF segment loading, privileged SVC syscalls, and fair CPU time slices, the execution pipeline completed without a single fault.',
    technicalDiagnostic:
      'Return Code: 0 (EXIT_SUCCESS) • Graphics: 120 FPS SurfaceFlinger • MMU Privilege Ring Transitions: Verified • CPU Latency: < 4.2ms.',
  },

  // Dead-End Fault Nodes (0 Choices)
  HALTED_RAW_TEXT: {
    id: 'HALTED_RAW_TEXT',
    stageName: 'Halted: CPU Fault',
    challenge: 'Pipeline Halted at Instruction Decode',
    icon: 'alert-circle',
    accentColor: '#FF3B30',
    choices: [],
    isTerminal: true,
    terminalType: 'HALTED',
    terminalTitle: 'EXECUTION HALTED: CPU Instruction Trap',
    terminalMessage:
      'CPU registers execute binary machine code opcodes. Silicon logic gates cannot execute raw ASCII text files without a compilation and assembly pass. Backtrack to resolve the failure.',
    technicalDiagnostic:
      'Signal: SIGILL (Illegal Instruction) • Faulting address: 0x00000000 • Instruction decode unit failed on ASCII character sequence.',
  },
  HALTED_IMAGE_WRAPPER: {
    id: 'HALTED_IMAGE_WRAPPER',
    stageName: 'Halted: Format Error',
    challenge: 'Pipeline Halted at Binary Validation',
    icon: 'alert-circle',
    accentColor: '#FF3B30',
    choices: [],
    isTerminal: true,
    terminalType: 'HALTED',
    terminalTitle: 'EXECUTION HALTED: Non-Executable Format',
    terminalMessage:
      'Image containers (PNG / JPEG) do not contain ELF / Mach-O executable header formats, data segments, or machine code instructions. Backtrack to resolve the failure.',
    technicalDiagnostic:
      'Error: ENOEXEC (Exec format error) • Missing magic number: 0x7F 0x45 0x4C 0x46 (.ELF).',
  },
  HALTED_UNRESOLVED_SYMBOL: {
    id: 'HALTED_UNRESOLVED_SYMBOL',
    stageName: 'Halted: Linker Panic',
    challenge: 'Pipeline Halted at Symbol Resolution',
    icon: 'alert-circle',
    accentColor: '#FF3B30',
    choices: [],
    isTerminal: true,
    terminalType: 'HALTED',
    terminalTitle: 'EXECUTION HALTED: Unresolved External Symbol',
    terminalMessage:
      'Zipping files without a Linker leaves external framework calls and math functions undefined. The binary crashed with missing symbol references. Backtrack to resolve the failure.',
    technicalDiagnostic:
      'Linker Crash: ld: 14 unresolved external symbols (libc.so, libgui.so) • Execution vector points to NULL.',
  },
  HALTED_GPU_MISMATCH: {
    id: 'HALTED_GPU_MISMATCH',
    stageName: 'Halted: Architecture Fault',
    challenge: 'Pipeline Halted at Bus Mapping',
    icon: 'alert-circle',
    accentColor: '#FF3B30',
    choices: [],
    isTerminal: true,
    terminalType: 'HALTED',
    terminalTitle: 'EXECUTION HALTED: Bus Architecture Failure',
    terminalMessage:
      'The GPU rasterizer processes vertex buffers and textures, not machine instructions. The OS Loader must allocate virtual memory in RAM and configure the page tables. Backtrack to resolve the failure.',
    technicalDiagnostic:
      'Hardware Bus Error: Attempted DMA of executable text segment to display scanout buffer.',
  },
  HALTED_SIGSEGV: {
    id: 'HALTED_SIGSEGV',
    stageName: 'Halted: Privilege Violation',
    challenge: 'Pipeline Halted at Hardware Boundary',
    icon: 'alert-circle',
    accentColor: '#FF3B30',
    choices: [],
    isTerminal: true,
    terminalType: 'HALTED',
    terminalTitle: 'EXECUTION HALTED: Kernel Panic / SIGSEGV',
    terminalMessage:
      'Segmentation Fault (SIGSEGV)! User Space (Ring 3) processes are hardware-isolated by the MMU from direct MMIO pin writes. All hardware access must use privileged Supervisor Calls (SVC). Backtrack to resolve the failure.',
    technicalDiagnostic:
      'Trap: SIGSEGV (Faulting MMIO addr: 0xFF021000) • Privilege violation: User mode attempted privileged hardware register write.',
  },
  HALTED_STARVATION: {
    id: 'HALTED_STARVATION',
    stageName: 'Halted: CPU Lockup',
    challenge: 'Pipeline Halted at Task Scheduling',
    icon: 'alert-circle',
    accentColor: '#FF3B30',
    choices: [],
    isTerminal: true,
    terminalType: 'HALTED',
    terminalTitle: 'EXECUTION HALTED: Thread Starvation & Freeze',
    terminalMessage:
      'Monopolizing CPU cores permanently starves UI and audio threads. The kernel hardware watchdog timer detected a soft-lockup and terminated the process. Backtrack to resolve the failure.',
    technicalDiagnostic:
      'Kernel Panic: Watchdog soft-lockup on CPU #0 • Thread starvation detected • ANR threshold exceeded (> 5000ms).',
  },
};

interface HistoryItem {
  nodeId: string;
  chosenOptionId?: string;
  chosenText?: string;
}

export const AppExecutionSimulator: React.FC = () => {
  const [history, setHistory] = useState<HistoryItem[]>([
    { nodeId: 'NODE_COMPILATION' },
  ]);
  const [currentIndex, setCurrentIndex] = useState(0);

  const cardFadeAnim = useRef(new Animated.Value(1)).current;
  const cardScaleAnim = useRef(new Animated.Value(1)).current;

  const currentItem = history[currentIndex] || history[0];
  const currentNode = PIPELINE_NODES[currentItem.nodeId] || PIPELINE_NODES.NODE_COMPILATION;

  // Animate node card on change
  useEffect(() => {
    cardFadeAnim.setValue(0.8);
    cardScaleAnim.setValue(0.97);

    Animated.parallel([
      Animated.timing(cardFadeAnim, {
        toValue: 1,
        duration: 200,
        useNativeDriver: true,
      }),
      Animated.spring(cardScaleAnim, {
        toValue: 1,
        friction: 7,
        useNativeDriver: true,
      }),
    ]).start();
  }, [currentIndex, currentItem.nodeId]);

  // Click an option: immediately advances to next node without revealing correctness.
  // If backtracking and selecting a different choice, PRUNE all subsequent steps.
  const handleSelectChoice = (choice: Choice) => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      } catch (e) {}
    }

    // Prune subsequent history steps after currentIndex
    const prunedHistory = history.slice(0, currentIndex);

    // Record chosen option for current node
    prunedHistory.push({
      nodeId: currentNode.id,
      chosenOptionId: choice.id,
      chosenText: choice.text,
    });

    // Advance to chosen next node
    prunedHistory.push({
      nodeId: choice.nextNodeId,
    });

    setHistory(prunedHistory);
    setCurrentIndex(prunedHistory.length - 1);
  };

  // Back Navigation
  const handleBack = () => {
    if (currentIndex > 0) {
      if (Platform.OS !== 'web') {
        try {
          Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
        } catch (e) {}
      }
      setCurrentIndex(currentIndex - 1);
    }
  };

  // Forward Navigation (if available)
  const handleForward = () => {
    if (currentIndex < history.length - 1) {
      if (Platform.OS !== 'web') {
        try {
          Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
        } catch (e) {}
      }
      setCurrentIndex(currentIndex + 1);
    }
  };

  // Jump to specific breadcrumb
  const handleJumpToStep = (index: number) => {
    if (index >= 0 && index < history.length) {
      if (Platform.OS !== 'web') {
        try {
          Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
        } catch (e) {}
      }
      setCurrentIndex(index);
    }
  };

  // Reset entire pipeline
  const handleRestart = () => {
    setHistory([{ nodeId: 'NODE_COMPILATION' }]);
    setCurrentIndex(0);
  };

  const isTerminal = currentNode.isTerminal || currentNode.choices.length === 0;

  return (
    <View style={styles.container}>
      {/* Title & Pipeline Header */}
      <View style={styles.header}>
        <View style={styles.headerTitleRow}>
          <View style={styles.headerBadge}>
            <Ionicons name="git-branch-outline" size={16} color="#FFFFFF" />
          </View>
          <View>
            <Text style={styles.headerTitle}>APP EXECUTION PIPELINE</Text>
            <Text style={styles.headerSubtitle}>INTERACTIVE COMPUTER SCIENCE DECISION TREE</Text>
          </View>
        </View>

        <TouchableOpacity style={styles.restartBtn} onPress={handleRestart} activeOpacity={0.8}>
          <Ionicons name="refresh" size={13} color="#FFA07A" />
          <Text style={styles.restartBtnText}>RESTART</Text>
        </TouchableOpacity>
      </View>

      {/* Visual Execution Breadcrumb Timeline */}
      <View style={styles.timelineCard}>
        <Text style={styles.timelineLabel}>PIPELINE EXECUTION TIMELINE:</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.breadcrumbList}
        >
          {history.map((item, idx) => {
            const node = PIPELINE_NODES[item.nodeId];
            const isCurrent = idx === currentIndex;
            const isTerminalNode = node?.isTerminal;

            return (
              <React.Fragment key={`${item.nodeId}-${idx}`}>
                <TouchableOpacity
                  style={[
                    styles.breadcrumbChip,
                    isCurrent && {
                      borderColor: node?.accentColor || Colors.studioOrange,
                      backgroundColor: 'rgba(221, 86, 34, 0.2)',
                    },
                    isTerminalNode && {
                      borderColor: node.terminalType === 'SUCCESS' ? '#34C759' : '#FF3B30',
                    },
                  ]}
                  onPress={() => handleJumpToStep(idx)}
                  activeOpacity={0.7}
                >
                  <View
                    style={[
                      styles.chipDot,
                      {
                        backgroundColor: isTerminalNode
                          ? node.terminalType === 'SUCCESS'
                            ? '#34C759'
                            : '#FF3B30'
                          : isCurrent
                          ? node?.accentColor
                          : 'rgba(255, 255, 255, 0.4)',
                      },
                    ]}
                  />
                  <Text
                    style={[
                      styles.chipText,
                      isCurrent && { color: '#FFFFFF', fontWeight: '800' },
                    ]}
                    numberOfLines={1}
                  >
                    {node?.stageName || `Node ${idx + 1}`}
                  </Text>
                </TouchableOpacity>

                {idx < history.length - 1 && (
                  <View style={styles.breadcrumbArrow}>
                    <Ionicons name="chevron-forward" size={12} color="rgba(255, 255, 255, 0.3)" />
                  </View>
                )}
              </React.Fragment>
            );
          })}
        </ScrollView>
      </View>

      {/* Main Interactive Stage Box */}
      <Animated.View
        style={[
          styles.stageBox,
          {
            opacity: cardFadeAnim,
            transform: [{ scale: cardScaleAnim }],
            borderColor: currentNode.accentColor || Colors.studioOrange,
          },
        ]}
      >
        {/* Stage Status Bar */}
        <View style={styles.stageStatusRow}>
          <View style={[styles.stageBadge, { backgroundColor: currentNode.accentColor }]}>
            <Ionicons name={currentNode.icon as any} size={15} color="#FFFFFF" />
            <Text style={styles.stageBadgeText}>
              {currentNode.stageNumber
                ? `STAGE ${currentNode.stageNumber} OF 5`
                : currentNode.stageName.toUpperCase()}
            </Text>
          </View>
          {currentNode.subtext && (
            <Text style={styles.subtextTag} numberOfLines={1}>
              {currentNode.subtext}
            </Text>
          )}
        </View>

        {/* ============================================================== */}
        {/* NON-TERMINAL NODE: CHALLENGE & CHOICES                         */}
        {/* ============================================================== */}
        {!isTerminal && (
          <View style={styles.challengeWrap}>
            <Text style={styles.challengeTitle}>{currentNode.challenge}</Text>

            <Text style={styles.selectPrompt}>
              Select the correct computer science operation to advance:
            </Text>

            {/* Decision Choices (1 to 3 options) */}
            <View style={styles.choicesList}>
              {currentNode.choices.map((choice) => (
                <TouchableOpacity
                  key={choice.id}
                  style={styles.choiceCard}
                  onPress={() => handleSelectChoice(choice)}
                  activeOpacity={0.75}
                >
                  <View style={styles.choiceHeaderRow}>
                    <View style={styles.optionPill}>
                      <Text style={styles.optionPillText}>{choice.label}</Text>
                    </View>
                    <Ionicons name="arrow-forward-circle" size={20} color="#FFA07A" />
                  </View>
                  <Text style={styles.choiceBodyText}>{choice.text}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        )}

        {/* ============================================================== */}
        {/* TERMINAL OUTCOME NODE (0 CHOICES)                              */}
        {/* ============================================================== */}
        {isTerminal && (
          <View style={styles.terminalContainer}>
            <View
              style={[
                styles.terminalCard,
                currentNode.terminalType === 'SUCCESS'
                  ? styles.terminalSuccessCard
                  : styles.terminalHaltedCard,
              ]}
            >
              <View style={styles.terminalIconRow}>
                <Ionicons
                  name={
                    currentNode.terminalType === 'SUCCESS'
                      ? 'checkmark-circle'
                      : 'close-circle'
                  }
                  size={42}
                  color={currentNode.terminalType === 'SUCCESS' ? '#34C759' : '#FF3B30'}
                />
                <View style={{ flex: 1 }}>
                  <Text
                    style={[
                      styles.terminalTitle,
                      {
                        color:
                          currentNode.terminalType === 'SUCCESS' ? '#34C759' : '#FF453A',
                      },
                    ]}
                  >
                    {currentNode.terminalTitle}
                  </Text>
                  <Text style={styles.terminalMessage}>{currentNode.terminalMessage}</Text>
                </View>
              </View>

              {/* Technical Diagnostic Terminal Box */}
              <View style={styles.diagnosticBox}>
                <View style={styles.diagnosticHeader}>
                  <Ionicons name="terminal-outline" size={13} color="#00E5FF" />
                  <Text style={styles.diagnosticHeaderText}>KERNEL DIAGNOSTIC LOG</Text>
                </View>
                <Text style={styles.diagnosticText}>{currentNode.technicalDiagnostic}</Text>
              </View>

              {/* Terminal Rewind Action */}
              <View style={styles.terminalActionRow}>
                <TouchableOpacity
                  style={styles.backtrackBtn}
                  onPress={handleBack}
                  activeOpacity={0.8}
                >
                  <Ionicons name="arrow-back" size={16} color="#FFFFFF" />
                  <Text style={styles.backtrackBtnText}>↺ BACKTRACK & CHOOSE ALTERNATE PATH</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        )}
      </Animated.View>

      {/* Persistent Navigation Controls (< Back / Forward >) */}
      <View style={styles.navBar}>
        <TouchableOpacity
          style={[styles.navBtn, currentIndex === 0 && styles.navBtnDisabled]}
          onPress={handleBack}
          disabled={currentIndex === 0}
          activeOpacity={0.8}
        >
          <Ionicons
            name="chevron-back"
            size={16}
            color={currentIndex === 0 ? 'rgba(255, 255, 255, 0.25)' : '#FFFFFF'}
          />
          <Text
            style={[
              styles.navBtnText,
              currentIndex === 0 && { color: 'rgba(255, 255, 255, 0.25)' },
            ]}
          >
            Back
          </Text>
        </TouchableOpacity>

        <Text style={styles.navStatusText}>
          Step {currentIndex + 1} of {history.length}
        </Text>

        <TouchableOpacity
          style={[
            styles.navBtn,
            currentIndex >= history.length - 1 && styles.navBtnDisabled,
          ]}
          onPress={handleForward}
          disabled={currentIndex >= history.length - 1}
          activeOpacity={0.8}
        >
          <Text
            style={[
              styles.navBtnText,
              currentIndex >= history.length - 1 && {
                color: 'rgba(255, 255, 255, 0.25)',
              },
            ]}
          >
            Forward
          </Text>
          <Ionicons
            name="chevron-forward"
            size={16}
            color={
              currentIndex >= history.length - 1 ? 'rgba(255, 255, 255, 0.25)' : '#FFFFFF'
            }
          />
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#12121E',
    borderRadius: 22,
    padding: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 14,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  headerBadge: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: Colors.studioOrange,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitle: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 0.8,
  },
  headerSubtitle: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 8,
    marginTop: 2,
    fontWeight: '700',
  },
  restartBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: 'rgba(221, 86, 34, 0.15)',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(221, 86, 34, 0.35)',
  },
  restartBtnText: {
    ...Typography.tag,
    color: '#FFA07A',
    fontSize: 9,
    fontWeight: '800',
  },
  timelineCard: {
    backgroundColor: '#0D0D14',
    borderRadius: 14,
    padding: 10,
    gap: 8,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
  },
  timelineLabel: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  breadcrumbList: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  breadcrumbChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  chipDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  chipText: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.7)',
    fontSize: 10,
    fontWeight: '600',
  },
  breadcrumbArrow: {
    paddingHorizontal: 2,
  },
  stageBox: {
    backgroundColor: '#181828',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1.5,
    gap: 14,
  },
  stageStatusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  stageBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 12,
  },
  stageBadgeText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  subtextTag: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.55)',
    fontSize: 9,
    maxWidth: '55%',
  },
  challengeWrap: {
    gap: 12,
  },
  challengeTitle: {
    ...Typography.headline,
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '800',
    lineHeight: 23,
  },
  selectPrompt: {
    ...Typography.tag,
    color: '#FFA07A',
    fontSize: 10,
    fontWeight: '700',
  },
  choicesList: {
    gap: 10,
  },
  choiceCard: {
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    gap: 8,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  choiceHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  optionPill: {
    backgroundColor: 'rgba(221, 86, 34, 0.25)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8,
  },
  optionPillText: {
    ...Typography.tag,
    color: '#FFA07A',
    fontSize: 10,
    fontWeight: '800',
  },
  choiceBodyText: {
    ...Typography.body,
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '600',
  },
  terminalContainer: {
    gap: 12,
  },
  terminalCard: {
    borderRadius: 16,
    padding: 16,
    borderWidth: 2,
    gap: 12,
  },
  terminalSuccessCard: {
    backgroundColor: 'rgba(52, 199, 89, 0.1)',
    borderColor: '#34C759',
  },
  terminalHaltedCard: {
    backgroundColor: 'rgba(255, 59, 48, 0.1)',
    borderColor: '#FF3B30',
  },
  terminalIconRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
  },
  terminalTitle: {
    ...Typography.headline,
    fontSize: 16,
    fontWeight: '900',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  terminalMessage: {
    ...Typography.body,
    color: 'rgba(255, 255, 255, 0.9)',
    fontSize: 13,
    lineHeight: 19,
  },
  diagnosticBox: {
    backgroundColor: '#0A0A12',
    borderRadius: 10,
    padding: 10,
    gap: 4,
    borderWidth: 1,
    borderColor: 'rgba(0, 229, 255, 0.3)',
  },
  diagnosticHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  diagnosticHeaderText: {
    ...Typography.tag,
    color: '#00E5FF',
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  diagnosticText: {
    ...Typography.tag,
    color: '#E0E0E0',
    fontSize: 10,
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    lineHeight: 15,
  },
  terminalActionRow: {
    marginTop: 4,
  },
  backtrackBtn: {
    backgroundColor: '#DD5622',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 8,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  backtrackBtnText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  navBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 4,
  },
  navBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    paddingVertical: 9,
    paddingHorizontal: 16,
    borderRadius: 12,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  navBtnDisabled: {
    opacity: 0.4,
  },
  navBtnText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
  },
  navStatusText: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.55)',
    fontSize: 11,
    fontWeight: '600',
  },
});
