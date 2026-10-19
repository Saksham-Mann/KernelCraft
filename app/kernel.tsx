import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Colors } from '../src/theme/colors';
import { Typography, FontFamilies } from '../src/theme/typography';
import { AppExecutionSimulator } from '../src/components/AppExecutionSimulator';
import { MyDeviceInspector } from '../src/components/MyDeviceInspector';
import { Ionicons } from '@expo/vector-icons';
import Svg, { Path } from 'react-native-svg';

type PrimarySectionTab = 'ARCH_TIERS' | 'SIMULATOR' | 'DEVICE_OS';

interface OperatingSystemTier {
  id: number;
  tierNumber: number;
  name: string;
  subtitle: string;
  tag: string;
  accentColor: string;
  badgeBg: string;
  badgeTextColor: string;
  whatItDoes: string;
  realWorldExample: string;
}

const OS_LAYERS: OperatingSystemTier[] = [
  {
    id: 1,
    tierNumber: 1,
    name: 'Apps & User Interface',
    subtitle: 'What you see and touch',
    tag: 'Safe Zone',
    accentColor: '#34C759',
    badgeBg: 'rgba(52, 199, 89, 0.16)',
    badgeTextColor: '#34C759',
    whatItDoes:
      'This is where all your apps live, like chat, games, and web browsers. Apps are kept in their own private bubble so if one app freezes or crashes, it cannot bring down your phone.',
    realWorldExample:
      'When you tap a button, the app cannot draw on the screen by itself - it asks the system to draw the change.',
  },
  {
    id: 2,
    tierNumber: 2,
    name: 'System Framework & Services',
    subtitle: "The phone's master organizer",
    tag: 'Android / System',
    accentColor: '#007AFF',
    badgeBg: 'rgba(0, 122, 255, 0.16)',
    badgeTextColor: '#38BDF8',
    whatItDoes:
      'The central organizer. It manages notifications, routes sound to your speakers, checks battery levels, and handles security popups.',
    realWorldExample:
      'When an app asks for permission to see your photo gallery, this layer pops up the permission prompt.',
  },
  {
    id: 3,
    tierNumber: 3,
    name: 'The Kernel',
    subtitle: 'The engine room & security guard',
    tag: 'Protected Core',
    accentColor: '#FF9500',
    badgeBg: 'rgba(255, 149, 0, 0.16)',
    badgeTextColor: '#FF9500',
    whatItDoes:
      'The engine room and bodyguard of your phone. The kernel is the only program allowed to touch the actual hardware. It divides CPU time between apps and manages your memory.',
    realWorldExample:
      'If multiple apps need the processor at the same instant, the kernel gives each one a turn for a few milliseconds.',
  },
  {
    id: 4,
    tierNumber: 4,
    name: 'Physical Silicon & Chips',
    subtitle: 'The raw computing hardware',
    tag: 'Bare Metal',
    accentColor: '#AF52DE',
    badgeBg: 'rgba(175, 82, 222, 0.16)',
    badgeTextColor: '#C084FC',
    whatItDoes:
      'The real physical microchips made of billions of microscopic electric switches. They turn kernel instructions into raw computing power.',
    realWorldExample:
      'This is the physical board and processor you disassembled in the Hardware Teardown.',
  },
];

interface TouchTraceStep {
  step: number;
  title: string;
  desc: string;
  color: string;
  time: string;
}

const TOUCH_TRACE_STEPS: TouchTraceStep[] = [
  {
    step: 1,
    title: 'Touch on Glass (UI)',
    desc: 'Screen detects finger coordinates',
    color: '#34C759', // Green
    time: '3 ms',
  },
  {
    step: 2,
    title: 'Permission Check (System)',
    desc: 'System framework verifies notification permission',
    color: '#007AFF', // Blue
    time: '7 ms',
  },
  {
    step: 3,
    title: 'Kernel Driver (Kernel)',
    desc: 'Kernel schedules notification interrupt',
    color: '#FF9500', // Amber
    time: '11 ms',
  },
  {
    step: 4,
    title: 'Hardware Alert (Hardware)',
    desc: 'Silicon delivers alert to device notification shade',
    color: '#AF52DE', // Purple
    time: '14 ms',
  },
];

interface SarcasticNotification {
  title: string;
  body: string;
}

const SARCASTIC_NOTIFICATIONS: SarcasticNotification[] = [
  {
    title: 'Bank Alert',
    body: '482910 is the OTP for a very legit purchase of 100 rubber ducks. Do not share.',
  },
  {
    title: 'Food Delivery',
    body: 'Your delivery driver is eating one of your fries at a red light.',
  },
  {
    title: 'Security Alert',
    body: 'Someone in your house just opened the fridge at 2:00 AM.',
  },
  {
    title: 'Battery Alert',
    body: '1 percent remaining. Time to say your final goodbyes or run to the charger.',
  },
  {
    title: 'Screen Time',
    body: 'You have used your phone for 9 hours today. Go touch some grass.',
  },
];

export interface KernelStackExplorerProps {
  onScrollUpToTeardown?: () => void;
  isActive?: boolean;
}

export function KernelStackExplorerView({
  onScrollUpToTeardown,
  isActive = true,
}: KernelStackExplorerProps = {}) {
  const router = useRouter();
  const scrollViewRef = useRef<ScrollView>(null);
  const scrollYRef = useRef<number>(0);
  const [primaryTab, setPrimaryTab] = useState<PrimarySectionTab>('ARCH_TIERS');

  // Accordion state: only one tier expanded at a time, or null if all closed
  const [expandedTierId, setExpandedTierId] = useState<number | null>(1);

  // "Trace a Touch" Simulation state
  const [simStep, setSimStep] = useState<number>(0);
  const [simRunning, setSimRunning] = useState<boolean>(false);
  const [elapsedMs, setElapsedMs] = useState<number>(0);
  const [simComplete, setSimComplete] = useState<boolean>(false);
  const [alertIndex, setAlertIndex] = useState<number>(0);
  const [activeAlert, setActiveAlert] = useState<SarcasticNotification | null>(null);
  const [permissionDenied, setPermissionDenied] = useState<boolean>(false);
  const timerRefs = useRef<NodeJS.Timeout[]>([]);

  const clearSimTimers = () => {
    timerRefs.current.forEach((t) => clearTimeout(t));
    timerRefs.current = [];
  };

  useEffect(() => {
    return () => clearSimTimers();
  }, []);

  const handleStartSim = async () => {
    if (simRunning) return;
    clearSimTimers();

    // Select the notification for this run
    const currentAlert = SARCASTIC_NOTIFICATIONS[alertIndex];
    // Advance index for the next run
    setAlertIndex((prev) => (prev + 1) % SARCASTIC_NOTIFICATIONS.length);

    let isPermissionGranted = false;
    let isPermissionDenied = false;

    // Check / request notification permission
    if (typeof window !== 'undefined' && 'Notification' in window) {
      if (Notification.permission === 'granted') {
        isPermissionGranted = true;
      } else if (Notification.permission === 'denied') {
        isPermissionDenied = true;
      } else {
        try {
          const permission = await Notification.requestPermission();
          if (permission === 'granted') {
            isPermissionGranted = true;
          } else if (permission === 'denied') {
            isPermissionDenied = true;
          }
        } catch {
          isPermissionDenied = true;
        }
      }
    } else {
      isPermissionDenied = true;
    }

    setPermissionDenied(isPermissionDenied);
    setActiveAlert(null);
    setSimComplete(false);
    setSimRunning(true);
    setSimStep(1);
    setElapsedMs(3);

    const t2 = setTimeout(() => {
      setSimStep(2);
      setElapsedMs(7);
    }, 400);

    const t3 = setTimeout(() => {
      setSimStep(3);
      setElapsedMs(11);
    }, 800);

    const t4 = setTimeout(() => {
      setSimStep(4);
      setElapsedMs(14);
      setSimComplete(true);
      setSimRunning(false);
      setActiveAlert(currentAlert);

      // Dispatch authentic native system notification on Step 4 completion
      if (
        typeof window !== 'undefined' &&
        'Notification' in window &&
        Notification.permission === 'granted'
      ) {
        try {
          new Notification(currentAlert.title, {
            body: currentAlert.body,
            tag: 'kernelcraft-sim-alert',
            renotify: true,
          } as any);
        } catch (e) {
          console.warn('Native notification dispatch failed:', e);
        }
      }
    }, 1200);

    timerRefs.current = [t2, t3, t4];
  };

  const handleToggleTier = (id: number) => {
    setExpandedTierId((prev) => (prev === id ? null : id));
  };

  const handleTabChange = (tab: PrimarySectionTab) => {
    setPrimaryTab(tab);
    scrollViewRef.current?.scrollTo({ y: 0, animated: false });
  };

  useEffect(() => {
    if (
      Platform.OS === 'web' &&
      typeof window !== 'undefined' &&
      onScrollUpToTeardown &&
      isActive
    ) {
      const handleWheel = (e: WheelEvent) => {
        if (scrollYRef.current <= 2 && e.deltaY < -15) {
          onScrollUpToTeardown();
        }
      };
      window.addEventListener('wheel', handleWheel, { passive: true });
      return () => window.removeEventListener('wheel', handleWheel);
    }
  }, [onScrollUpToTeardown, isActive]);

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Navigation Bar */}
        <View style={styles.topBar}>
          <TouchableOpacity
            style={styles.circleButton}
            onPress={() => router.replace('/')}
            accessibilityRole="button"
            accessibilityLabel="Exit to home"
          >
            <Svg width={20} height={20} viewBox="0 0 24 24" fill="none">
              <Path
                d="M15 19L8 12L15 5"
                stroke="#FFFFFF"
                strokeWidth={3}
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </Svg>
          </TouchableOpacity>

          <View style={styles.topBarTitleCenter}>
            <Text style={styles.screenHeaderTitle}>OS SOFTWARE LAYER</Text>
          </View>

          {/* Spacer to keep center title balanced */}
          <View style={{ width: 38, height: 38 }} />
        </View>

        {/* Primary Feature Tabs Toggle (1st: OS Layers | 2nd: App Execution | 3rd: My Device OS) */}
        <View style={styles.primaryTabsRow}>
          <TouchableOpacity
            style={[
              styles.primaryTabButton,
              primaryTab === 'ARCH_TIERS' && styles.primaryTabButtonActive,
            ]}
            onPress={() => handleTabChange('ARCH_TIERS')}
            activeOpacity={0.8}
          >
            <Ionicons
              name="layers-outline"
              size={14}
              color={primaryTab === 'ARCH_TIERS' ? '#FFFFFF' : Colors.inkLight}
            />
            <Text
              style={[
                styles.primaryTabText,
                primaryTab === 'ARCH_TIERS' && styles.primaryTabTextActive,
              ]}
            >
              OS Layers
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.primaryTabButton,
              primaryTab === 'SIMULATOR' && styles.primaryTabButtonActive,
            ]}
            onPress={() => handleTabChange('SIMULATOR')}
            activeOpacity={0.8}
          >
            <Ionicons
              name="play"
              size={14}
              color={primaryTab === 'SIMULATOR' ? '#FFFFFF' : Colors.inkLight}
            />
            <Text
              style={[
                styles.primaryTabText,
                primaryTab === 'SIMULATOR' && styles.primaryTabTextActive,
              ]}
            >
              App Execution
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.primaryTabButton,
              primaryTab === 'DEVICE_OS' && styles.primaryTabButtonActive,
            ]}
            onPress={() => handleTabChange('DEVICE_OS')}
            activeOpacity={0.8}
          >
            <Ionicons
              name="speedometer-outline"
              size={14}
              color={primaryTab === 'DEVICE_OS' ? '#FFFFFF' : Colors.inkLight}
            />
            <Text
              style={[
                styles.primaryTabText,
                primaryTab === 'DEVICE_OS' && styles.primaryTabTextActive,
              ]}
            >
              My Device OS
            </Text>
          </TouchableOpacity>
        </View>

        <ScrollView
          ref={scrollViewRef}
          style={styles.scrollView}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
          nestedScrollEnabled={true}
          bounces={true}
          alwaysBounceVertical={true}
          scrollEventThrottle={16}
          onScroll={(e) => {
            scrollYRef.current = e.nativeEvent.contentOffset.y;
          }}
          onScrollEndDrag={(e) => {
            if (
              isActive &&
              e.nativeEvent.contentOffset.y <= -20 &&
              onScrollUpToTeardown
            ) {
              onScrollUpToTeardown();
            }
          }}
          keyboardShouldPersistTaps="handled"
        >
          {/* TAB 1: HOW AN OPERATING SYSTEM WORKS (Inline Collapsible Accordions & Trace a Touch Simulation) */}
          {primaryTab === 'ARCH_TIERS' && (
            <View style={styles.tabSectionWrap}>
              {/* Section Header */}
              <View style={styles.sectionHeaderRow}>
                <Text style={styles.sectionHeaderTitle}>
                  HOW AN OPERATING SYSTEM WORKS
                </Text>
              </View>

              {/* The 4 Inline Accordion Stack Cards */}
              <View style={styles.stackCardsContainer}>
                {OS_LAYERS.map((layer) => {
                  const isExpanded = expandedTierId === layer.id;

                  return (
                    <View
                      key={layer.id}
                      style={[
                        styles.tierAccordionCard,
                        isExpanded && [
                          styles.tierAccordionCardExpanded,
                          { borderColor: layer.accentColor },
                        ],
                      ]}
                    >
                      {/* Header Row (Tappable Trigger) */}
                      <TouchableOpacity
                        style={styles.tierHeaderRow}
                        onPress={() => handleToggleTier(layer.id)}
                        activeOpacity={0.8}
                        accessibilityRole="button"
                        accessibilityLabel={`Toggle Tier ${layer.tierNumber}: ${layer.name}`}
                      >
                        {/* Left: Tier Number Pill */}
                        <View
                          style={[
                            styles.tierNumberPill,
                            isExpanded && {
                              backgroundColor: layer.accentColor,
                            },
                          ]}
                        >
                          <Text
                            style={[
                              styles.tierNumberPillText,
                              isExpanded && styles.tierNumberPillTextActive,
                            ]}
                          >
                            Tier {layer.tierNumber}
                          </Text>
                        </View>

                        {/* Middle: Name & Subtitle */}
                        <View style={styles.tierInfoBlock}>
                          <Text style={styles.tierNameText}>{layer.name}</Text>
                          <Text style={styles.tierSubtitleText}>
                            {layer.subtitle}
                          </Text>
                        </View>

                        {/* Right: Tag Pill & Animated Chevron */}
                        <View style={styles.tierRightBlock}>
                          <View
                            style={[
                              styles.tierTagPill,
                              { backgroundColor: layer.badgeBg },
                            ]}
                          >
                            <Text
                              style={[
                                styles.tierTagPillText,
                                { color: layer.badgeTextColor },
                              ]}
                            >
                              {layer.tag}
                            </Text>
                          </View>
                          <View
                            style={[
                              styles.chevronBox,
                              Platform.OS === 'web' &&
                                ({
                                  transform: isExpanded
                                    ? 'rotate(90deg)'
                                    : 'rotate(0deg)',
                                  transition: 'transform 0.25s ease',
                                } as any),
                            ]}
                          >
                            <Ionicons
                              name={
                                Platform.OS === 'web'
                                  ? 'chevron-forward'
                                  : isExpanded
                                  ? 'chevron-down'
                                  : 'chevron-forward'
                              }
                              size={16}
                              color={isExpanded ? layer.accentColor : '#64748B'}
                            />
                          </View>
                        </View>
                      </TouchableOpacity>

                      {/* Inline Expanded Drawer */}
                      {isExpanded && (
                        <View style={styles.accordionDrawer}>
                          <View style={styles.drawerDivider} />

                          {/* What It Does Section */}
                          <View style={styles.drawerSection}>
                            <Text style={styles.drawerSectionLabel}>
                              WHAT IT DOES
                            </Text>
                            <Text style={styles.drawerBodyText}>
                              {layer.whatItDoes}
                            </Text>
                          </View>

                          {/* Real-World Example Section */}
                          <View
                            style={[
                              styles.drawerExampleBox,
                              { borderLeftColor: layer.accentColor },
                            ]}
                          >
                            <Text
                              style={[
                                styles.drawerExampleLabel,
                                { color: layer.accentColor },
                              ]}
                            >
                              REAL-WORLD EXAMPLE
                            </Text>
                            <Text style={styles.drawerExampleText}>
                              {layer.realWorldExample}
                            </Text>
                          </View>
                        </View>
                      )}
                    </View>
                  );
                })}
              </View>

              {/* INTERACTIVE: WATCH A TAP TRAVEL Simulation Card */}
              <View style={styles.simCardContainer}>
                {/* Header & Subtitle */}
                <View style={styles.simCardHeader}>
                  <Text style={styles.simCardTitle}>WATCH A TAP TRAVEL</Text>
                  <Text style={styles.simCardSubtitle}>
                    See how an electrical tap on glass reaches the physical chip.
                  </Text>
                </View>

                {/* Primary Trigger Button (Full Width) */}
                <TouchableOpacity
                  style={[
                    styles.simTriggerBtn,
                    simRunning && styles.simTriggerBtnRunning,
                  ]}
                  onPress={handleStartSim}
                  activeOpacity={0.8}
                  disabled={simRunning}
                  accessibilityRole="button"
                  accessibilityLabel="Tap to receive notification"
                >
                  <Ionicons
                    name="notifications"
                    size={16}
                    color={simRunning ? '#94A3B8' : '#FFFFFF'}
                  />
                  <Text
                    style={[
                      styles.simTriggerBtnText,
                      simRunning && styles.simTriggerBtnTextRunning,
                    ]}
                  >
                    {simRunning ? 'SIGNAL IN TRANSIT...' : 'TAP TO RECEIVE NOTIFICATION'}
                  </Text>
                </TouchableOpacity>

                {/* Mini Vertical Stepper Representing the 4 Layers */}
                <View style={styles.stepperContainer}>
                  {TOUCH_TRACE_STEPS.map((step, idx) => {
                    const isLit = simStep >= step.step;
                    const isCurrent = simStep === step.step;
                    const isLast = idx === TOUCH_TRACE_STEPS.length - 1;

                    return (
                      <View key={step.step} style={styles.stepItemWrapper}>
                        <View style={styles.stepIndicatorCol}>
                          {/* Glowing Accent Node */}
                          <View
                            style={[
                              styles.stepNodeCircle,
                              isLit && [
                                styles.stepNodeCircleLit,
                                {
                                  backgroundColor: step.color,
                                  borderColor: step.color,
                                },
                              ],
                              isCurrent &&
                                !simComplete && [
                                  styles.stepNodeCircleCurrent,
                                  Platform.OS === 'web' &&
                                    ({
                                      boxShadow: `0 0 14px ${step.color}`,
                                    } as any),
                                ],
                            ]}
                          >
                            {/* Checkmark shows for passed steps OR when simulation completes (including Step 4) */}
                            {isLit && (!isCurrent || simComplete) ? (
                              <Ionicons name="checkmark" size={14} color="#FFFFFF" />
                            ) : (
                              <Text
                                style={[
                                  styles.stepNodeNumber,
                                  isLit && styles.stepNodeNumberLit,
                                ]}
                              >
                                {step.step}
                              </Text>
                            )}
                          </View>

                          {/* Connecting Vertical Track */}
                          {!isLast && (
                            <View
                              style={[
                                styles.stepTrackLine,
                                simStep > step.step && {
                                  backgroundColor: step.color,
                                },
                              ]}
                            />
                          )}
                        </View>

                        {/* Step Text Info */}
                        <View
                          style={[
                            styles.stepContentCol,
                            isLit && [
                              styles.stepContentColLit,
                              { borderLeftColor: step.color },
                            ],
                          ]}
                        >
                          <View style={styles.stepTitleRow}>
                            <Text
                              style={[
                                styles.stepTitleText,
                                isLit && styles.stepTitleTextLit,
                              ]}
                            >
                              {step.title}
                            </Text>
                            {isLit && (
                              <View
                                style={[
                                  styles.stepTimeBadge,
                                  { backgroundColor: 'rgba(255, 255, 255, 0.08)' },
                                ]}
                              >
                                <Text
                                  style={[
                                    styles.stepTimeBadgeText,
                                    { color: step.color },
                                  ]}
                                >
                                  {step.time}
                                </Text>
                              </View>
                            )}
                          </View>
                          <Text
                            style={[
                              styles.stepDescText,
                              isLit && styles.stepDescTextLit,
                            ]}
                          >
                            {step.desc}
                          </Text>
                        </View>
                      </View>
                    );
                  })}
                </View>

                {/* Bottom Bar: Total Trip Time Badge & Status Readout */}
                <View style={styles.simFooter}>
                  {/* Elapsed Time Badge */}
                  <View style={styles.simTimeBadgeBox}>
                    <Ionicons name="timer-outline" size={14} color="#FF9500" />
                    <Text style={styles.simTimeBadgeText}>
                      {simComplete
                        ? 'Total Trip Time: 14 ms'
                        : simRunning
                        ? `Total Trip Time: ${elapsedMs} ms`
                        : 'Total Trip Time: 0 ms'}
                    </Text>
                  </View>

                  {/* Brief Status Readout Below Stepper */}
                  <View
                    style={[
                      styles.simStatusBox,
                      simComplete && styles.simStatusBoxComplete,
                    ]}
                  >
                    <Ionicons
                      name={
                        simComplete
                          ? 'checkmark-circle-outline'
                          : simRunning
                          ? 'radio-button-on'
                          : 'information-circle-outline'
                      }
                      size={15}
                      color={
                        simComplete
                          ? '#34C759'
                          : simRunning
                          ? '#FF9500'
                          : '#94A3B8'
                      }
                    />
                    <Text
                      style={[
                        styles.simStatusText,
                        simComplete && styles.simStatusTextComplete,
                      ]}
                    >
                      {simComplete
                        ? 'Touch signal successfully processed through all layers to silicon.'
                        : simRunning
                        ? 'Tracing electrical tap through operating system layers...'
                        : 'Press "TAP TO RECEIVE NOTIFICATION" to trace the signal flow.'}
                    </Text>
                  </View>

                  {/* Permission Denied In-App Notice */}
                  {permissionDenied && simComplete && (
                    <View style={styles.permissionDeniedNotice}>
                      <Ionicons
                        name="alert-circle-outline"
                        size={16}
                        color="#FF9500"
                      />
                      <Text style={styles.permissionDeniedText}>
                        System notification permission was denied. Displaying in-app alert only.
                      </Text>
                    </View>
                  )}

                  {/* On-Screen Sarcastic Notification Toast Card */}
                  {activeAlert && simComplete && (
                    <View style={styles.onScreenToastCard}>
                      <View style={styles.toastHeaderRow}>
                        <View style={styles.toastBadgeRow}>
                          <Ionicons
                            name="notifications"
                            size={13}
                            color="#38BDF8"
                          />
                          <Text style={styles.toastBadgeText}>
                            DEVICE NOTIFICATION
                          </Text>
                        </View>
                        <Text style={styles.toastTimestampText}>Just now</Text>
                      </View>
                      <Text style={styles.toastTitleText}>
                        {activeAlert.title}
                      </Text>
                      <Text style={styles.toastBodyText}>
                        {activeAlert.body}
                      </Text>
                    </View>
                  )}
                </View>
              </View>
            </View>
          )}

          {/* TAB 2: Simplified 5-Step Process Execution Simulation */}
          {primaryTab === 'SIMULATOR' && (
            <View style={styles.tabSectionWrap}>
              <AppExecutionSimulator />
            </View>
          )}

          {/* TAB 3: My Device OS Inspector */}
          {primaryTab === 'DEVICE_OS' && (
            <View style={styles.tabSectionWrap}>
              <MyDeviceInspector />
            </View>
          )}
        </ScrollView>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    height: '100%',
    backgroundColor: '#12151B',
  },
  container: {
    flex: 1,
    height: '100%',
    backgroundColor: '#12151B',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.08)',
  },
  circleButton: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  topBarTitleCenter: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  screenHeaderTitle: {
    fontFamily: FontFamilies.sans,
    color: Colors.textWhite,
    fontSize: 14,
    letterSpacing: 1,
    fontWeight: '700',
  },
  primaryTabsRow: {
    flexDirection: 'row',
    backgroundColor: '#161A22',
    padding: 6,
    marginHorizontal: 16,
    marginTop: 10,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
    gap: 6,
  },
  primaryTabButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 9,
    borderRadius: 12,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  primaryTabButtonActive: {
    backgroundColor: Colors.studioOrange,
    shadowColor: Colors.studioOrange,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.4,
    shadowRadius: 6,
    elevation: 4,
  },
  primaryTabText: {
    fontFamily: FontFamilies.sans,
    color: Colors.inkLight,
    fontSize: 11,
    fontWeight: '700',
  },
  primaryTabTextActive: {
    color: '#FFFFFF',
    fontWeight: '600',
  },
  scrollView: {
    flex: 1,
    width: '100%',
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 60,
    flexGrow: 1,
  },
  tabSectionWrap: {
    gap: 14,
  },
  sectionHeaderRow: {
    paddingHorizontal: 4,
  },
  sectionHeaderTitle: {
    fontFamily: FontFamilies.sans,
    color: '#94A3B8',
    fontSize: 11,
    letterSpacing: 1.2,
    fontWeight: '700',
  },
  stackCardsContainer: {
    gap: 10,
  },
  tierAccordionCard: {
    backgroundColor: '#161A22',
    borderRadius: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.07)',
    overflow: 'hidden',
    ...(Platform.OS === 'web' ? ({ transition: 'all 0.25s ease' } as any) : {}),
  },
  tierAccordionCardExpanded: {
    backgroundColor: '#1A1F29',
    shadowColor: 'rgba(0, 0, 0, 0.4)',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 3,
  },
  tierHeaderRow: {
    paddingVertical: 14,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  tierNumberPill: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  tierNumberPillText: {
    fontFamily: FontFamilies.sans,
    color: '#94A3B8',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  tierNumberPillTextActive: {
    color: '#FFFFFF',
  },
  tierInfoBlock: {
    flex: 1,
    gap: 2,
  },
  tierNameText: {
    fontFamily: FontFamilies.sans,
    color: '#F8FAFC',
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 0.2,
  },
  tierSubtitleText: {
    fontFamily: FontFamilies.sans,
    color: '#94A3B8',
    fontSize: 12,
    lineHeight: 16,
  },
  tierRightBlock: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  tierTagPill: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  tierTagPillText: {
    fontFamily: FontFamilies.sans,
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
  chevronBox: {
    width: 20,
    height: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  accordionDrawer: {
    paddingHorizontal: 16,
    paddingBottom: 16,
    gap: 12,
  },
  drawerDivider: {
    height: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.07)',
    marginBottom: 4,
  },
  drawerSection: {
    gap: 6,
  },
  drawerSectionLabel: {
    fontFamily: FontFamilies.sans,
    color: '#64748B',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
  },
  drawerBodyText: {
    fontFamily: FontFamilies.sans,
    color: '#E2E8F0',
    fontSize: 13.5,
    lineHeight: 20,
  },
  drawerExampleBox: {
    backgroundColor: '#12151B',
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    borderLeftWidth: 3.5,
    gap: 4,
  },
  drawerExampleLabel: {
    fontFamily: FontFamilies.sans,
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
  },
  drawerExampleText: {
    fontFamily: FontFamilies.sans,
    color: '#CBD5E1',
    fontSize: 13,
    lineHeight: 19,
  },
  simCardContainer: {
    backgroundColor: '#161A22',
    borderRadius: 20,
    padding: 18,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 16,
    marginTop: 8,
  },
  simCardHeader: {
    gap: 4,
  },
  simCardTitle: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
  simCardSubtitle: {
    fontFamily: FontFamilies.sans,
    color: '#94A3B8',
    fontSize: 12,
    lineHeight: 17,
  },
  simTriggerBtn: {
    width: '100%',
    backgroundColor: Colors.studioOrange,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 13,
    paddingHorizontal: 16,
    borderRadius: 14,
    shadowColor: Colors.studioOrange,
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.35,
    shadowRadius: 8,
    elevation: 4,
    ...(Platform.OS === 'web'
      ? ({ cursor: 'pointer', transition: 'all 0.2s ease' } as any)
      : {}),
  },
  simTriggerBtnRunning: {
    backgroundColor: '#334155',
    shadowOpacity: 0,
  },
  simTriggerBtnText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.8,
  },
  simTriggerBtnTextRunning: {
    color: '#94A3B8',
  },
  stepperContainer: {
    backgroundColor: '#12151B',
    borderRadius: 16,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    gap: 4,
  },
  stepItemWrapper: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
  },
  stepIndicatorCol: {
    alignItems: 'center',
    width: 28,
  },
  stepNodeCircle: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    alignItems: 'center',
    justifyContent: 'center',
    ...(Platform.OS === 'web'
      ? ({ transition: 'all 0.25s ease' } as any)
      : {}),
  },
  stepNodeCircleLit: {
    borderWidth: 0,
  },
  stepNodeCircleCurrent: {
    transform: [{ scale: 1.08 }],
  },
  stepNodeNumber: {
    fontFamily: FontFamilies.sans,
    color: '#64748B',
    fontSize: 11,
    fontWeight: '700',
  },
  stepNodeNumberLit: {
    color: '#FFFFFF',
  },
  stepTrackLine: {
    width: 2,
    height: 24,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    marginVertical: 2,
    ...(Platform.OS === 'web'
      ? ({ transition: 'background-color 0.25s ease' } as any)
      : {}),
  },
  stepContentCol: {
    flex: 1,
    paddingVertical: 3,
    gap: 2,
  },
  stepContentColLit: {
    opacity: 1,
  },
  stepTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  stepTitleText: {
    fontFamily: FontFamilies.sans,
    color: '#64748B',
    fontSize: 13,
    fontWeight: '700',
    ...(Platform.OS === 'web'
      ? ({ transition: 'color 0.2s ease' } as any)
      : {}),
  },
  stepTitleTextLit: {
    color: '#F8FAFC',
  },
  stepTimeBadge: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  stepTimeBadgeText: {
    fontFamily: FontFamilies.sans,
    fontSize: 10,
    fontWeight: '700',
  },
  stepDescText: {
    fontFamily: FontFamilies.sans,
    color: '#475569',
    fontSize: 11.5,
    lineHeight: 16,
    ...(Platform.OS === 'web'
      ? ({ transition: 'color 0.2s ease' } as any)
      : {}),
  },
  stepDescTextLit: {
    color: '#94A3B8',
  },
  simFooter: {
    gap: 10,
  },
  simTimeBadgeBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 10,
    backgroundColor: 'rgba(255, 149, 0, 0.08)',
    borderWidth: 1,
    borderColor: 'rgba(255, 149, 0, 0.2)',
    alignSelf: 'flex-start',
  },
  simTimeBadgeText: {
    fontFamily: FontFamilies.sans,
    color: '#FF9500',
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
  simStatusBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#12151B',
    borderRadius: 12,
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
  },
  simStatusBoxComplete: {
    borderColor: 'rgba(52, 199, 89, 0.3)',
    backgroundColor: 'rgba(52, 199, 89, 0.06)',
  },
  simStatusText: {
    fontFamily: FontFamilies.sans,
    color: '#94A3B8',
    fontSize: 12,
    lineHeight: 17,
    flex: 1,
  },
  simStatusTextComplete: {
    color: '#E2E8F0',
    fontWeight: '500',
  },
  permissionDeniedNotice: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: 'rgba(255, 149, 0, 0.08)',
    borderRadius: 12,
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 149, 0, 0.25)',
  },
  permissionDeniedText: {
    fontFamily: FontFamilies.sans,
    color: '#FFB84D',
    fontSize: 12,
    lineHeight: 17,
    flex: 1,
    fontWeight: '500',
  },
  onScreenToastCard: {
    backgroundColor: '#12151B',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1.5,
    borderColor: 'rgba(0, 122, 255, 0.35)',
    borderLeftWidth: 4,
    borderLeftColor: '#007AFF',
    gap: 6,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 3,
  },
  toastHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  toastBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  toastBadgeText: {
    fontFamily: FontFamilies.sans,
    color: '#38BDF8',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.8,
  },
  toastTimestampText: {
    fontFamily: FontFamilies.sans,
    color: '#64748B',
    fontSize: 10,
    fontWeight: '500',
  },
  toastTitleText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 0.2,
  },
  toastBodyText: {
    fontFamily: FontFamilies.sans,
    color: '#CBD5E1',
    fontSize: 12.5,
    lineHeight: 18,
  },
});

export default KernelStackExplorerView;
