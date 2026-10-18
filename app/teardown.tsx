import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  TouchableOpacity,
  Platform,
  Animated,
  GestureResponderEvent,
  ScrollView,
} from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { Colors } from '../src/theme/colors';
import { Typography } from '../src/theme/typography';
import {
  TEARDOWN_FRAME_ASSETS,
  TOTAL_FRAMES,
  TEARDOWN_ANNOTATIONS,
  TeardownAnnotation,
} from '../src/data/teardownData';
import { AnnotationOverlay } from '../src/components/AnnotationOverlay';
import { AppExecutionSimulator } from '../src/components/AppExecutionSimulator';
import { MyDeviceInspector } from '../src/components/MyDeviceInspector';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

type ScreenMode = 'TEARDOWN' | 'OS_SIMULATION';
type OsTab = 'SIMULATOR' | 'DEVICE_OS';

export default function TeardownScreen() {
  const router = useRouter();

  // Screen Mode: 'TEARDOWN' or 'OS_SIMULATION'
  const [screenMode, setScreenMode] = useState<ScreenMode>('TEARDOWN');
  const [osTab, setOsTab] = useState<OsTab>('SIMULATOR');

  const [progress, setProgress] = useState(0.0);
  const [selectedAnnotation, setSelectedAnnotation] = useState<TeardownAnnotation | null>(null);
  const [viewportSize, setViewportSize] = useState({ width: 0, height: 0 });

  const progressRef = useRef(0.0);
  progressRef.current = progress;

  // Zoom animation for phone screen container when "Dive In" is clicked
  const zoomScale = useRef(new Animated.Value(1)).current;
  const zoomOpacity = useRef(new Animated.Value(1)).current;
  const osFadeAnim = useRef(new Animated.Value(0)).current;

  // Fade and pulse animation for the single centered "Dive In" button
  const diveInFadeAnim = useRef(new Animated.Value(0)).current;
  const diveInPulseAnim = useRef(new Animated.Value(1)).current;

  // Calculate current frame index (0..59)
  const currentFrameIndex = Math.min(
    TOTAL_FRAMES - 1,
    Math.max(0, Math.round(progress * (TOTAL_FRAMES - 1)))
  );

  // Final assembled front screen reached condition
  const isFinalFrontScreen = currentFrameIndex >= TOTAL_FRAMES - 1 || progress >= 0.95;

  // Trigger fade in / out of Dive In button based on final frame condition
  useEffect(() => {
    if (isFinalFrontScreen) {
      Animated.timing(diveInFadeAnim, {
        toValue: 1,
        duration: 250,
        useNativeDriver: true,
      }).start();

      // Start gentle breathing pulse
      const pulseLoop = Animated.loop(
        Animated.sequence([
          Animated.timing(diveInPulseAnim, {
            toValue: 1.06,
            duration: 850,
            useNativeDriver: true,
          }),
          Animated.timing(diveInPulseAnim, {
            toValue: 1,
            duration: 850,
            useNativeDriver: true,
          }),
        ])
      );
      pulseLoop.start();
      return () => pulseLoop.stop();
    } else {
      Animated.timing(diveInFadeAnim, {
        toValue: 0,
        duration: 150,
        useNativeDriver: true,
      }).start();
    }
  }, [isFinalFrontScreen]);

  // Apply clamped progress bidirectionally with no latching
  const applyProgress = useCallback((newProgress: number) => {
    const clamped = Math.max(0, Math.min(1, newProgress));
    setProgress(clamped);

    // Auto-dismiss modal if scrolled far away from component range
    setSelectedAnnotation((prev) => {
      if (prev && (clamped < prev.startProgress - 0.08 || clamped > prev.endProgress + 0.08)) {
        return null;
      }
      return prev;
    });
  }, []);

  // Web Wheel Listener: Deliberate, dampened, true bidirectional scrubbing
  useEffect(() => {
    if (Platform.OS === 'web' && typeof window !== 'undefined') {
      const handleWheel = (e: WheelEvent) => {
        // Only scrub while on TEARDOWN view and no modal open
        if (screenMode !== 'TEARDOWN') return;

        e.preventDefault();
        // Dampen wheel sensitivity significantly so user scrolls deliberately
        const rawDelta = e.deltaY;
        const clampedDelta = Math.max(-100, Math.min(100, rawDelta));
        const step = clampedDelta * 0.00022;

        const currentP = progressRef.current;
        const nextP = Math.max(0, Math.min(1, currentP + step));

        if (nextP !== currentP) {
          progressRef.current = nextP;
          applyProgress(nextP);
        }
      };

      window.addEventListener('wheel', handleWheel, { passive: false });
      return () => {
        window.removeEventListener('wheel', handleWheel);
      };
    }
  }, [screenMode, applyProgress]);

  // Touch handlers for mobile / tablet drag scrubbing (bidirectional)
  const touchStartY = useRef<number | null>(null);
  const touchStartProgress = useRef<number>(0);
  const hasMovedSignificantly = useRef<boolean>(false);

  const handleTouchStart = (e: GestureResponderEvent) => {
    if (screenMode !== 'TEARDOWN') return;
    touchStartY.current = e.nativeEvent.pageY;
    touchStartProgress.current = progressRef.current;
    hasMovedSignificantly.current = false;
  };

  const handleTouchMove = (e: GestureResponderEvent) => {
    if (screenMode !== 'TEARDOWN' || touchStartY.current === null) return;
    const deltaY = touchStartY.current - e.nativeEvent.pageY;

    if (Math.abs(deltaY) > 6) {
      hasMovedSignificantly.current = true;
    }

    const touchSensitivity = 0.0007;
    const nextP = Math.max(0, Math.min(1, touchStartProgress.current + deltaY * touchSensitivity));

    if (nextP !== progressRef.current) {
      progressRef.current = nextP;
      applyProgress(nextP);
    }
  };

  const handleTouchEnd = () => {
    touchStartY.current = null;
  };

  // Dive In Click Handler: quick zoom-in scale animation then transitions to OS simulation
  const handleDiveIn = () => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      } catch (e) {}
    }

    Animated.parallel([
      Animated.timing(zoomScale, {
        toValue: 3.2,
        duration: 400,
        useNativeDriver: true,
      }),
      Animated.timing(zoomOpacity, {
        toValue: 0,
        duration: 380,
        useNativeDriver: true,
      }),
    ]).start(() => {
      setScreenMode('OS_SIMULATION');
      Animated.timing(osFadeAnim, {
        toValue: 1,
        duration: 300,
        useNativeDriver: true,
      }).start();
    });
  };

  // Return from OS simulation back to hardware teardown
  const handleBackToTeardown = () => {
    zoomScale.setValue(1);
    zoomOpacity.setValue(1);
    osFadeAnim.setValue(0);
    setScreenMode('TEARDOWN');
  };

  // Click any hardware tag -> open clean popup modal
  const handleSelectAnnotation = (ann: TeardownAnnotation) => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      } catch (e) {}
    }
    setSelectedAnnotation(ann);
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View
        style={styles.container}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        {/* ============================================================== */}
        {/* MODE 1: HARDWARE TEARDOWN VIEW                                 */}
        {/* ============================================================== */}
        {screenMode === 'TEARDOWN' && (
          <View style={styles.teardownWrapper}>
            {/* Top Bar Header */}
            <View style={styles.topBar}>
              <TouchableOpacity
                style={styles.circleIconButton}
                onPress={() => router.replace('/')}
                accessibilityLabel="Back to splash"
              >
                <Ionicons name="chevron-back" size={20} color={Colors.textWhite} />
              </TouchableOpacity>

              <View style={styles.topBarCenter}>
                <Text style={styles.screenHeaderTitle}>HARDWARE TEARDOWN</Text>
                <Text style={styles.screenHeaderSubtitle}>
                  {isFinalFrontScreen
                    ? 'FRONT DISPLAY READY • TAP DIVE IN'
                    : progress < 0.2
                    ? 'CLOSED CHASSIS • SCROLL DOWN'
                    : progress < 0.72
                    ? 'TAP ANY COMPONENT TO INSPECT'
                    : 'REASSEMBLING CHASSIS'}
                </Text>
              </View>

              <TouchableOpacity
                style={styles.circleIconButton}
                onPress={() => {
                  progressRef.current = 0;
                  applyProgress(0);
                }}
                accessibilityLabel="Reset to beginning"
              >
                <Ionicons name="refresh" size={18} color={Colors.textWhite} />
              </TouchableOpacity>
            </View>

            {/* Main Phone Viewport (Fills primary vertical space) */}
            <Animated.View
              style={[
                styles.viewportContainer,
                {
                  transform: [{ scale: zoomScale }],
                  opacity: zoomOpacity,
                },
              ]}
              onLayout={(e) => {
                const { width, height } = e.nativeEvent.layout;
                setViewportSize({ width, height });
              }}
            >
              {/* Full Lifecycle Teardown Image Frame (60 frames) */}
              <Image
                source={TEARDOWN_FRAME_ASSETS[currentFrameIndex]}
                style={styles.frameImage}
                contentFit="contain"
                transition={20}
              />

              {/* Floating Hardware Component Hotspots & Pill Tags */}
              <AnnotationOverlay
                annotations={TEARDOWN_ANNOTATIONS}
                progress={progress}
                containerWidth={viewportSize.width}
                containerHeight={viewportSize.height}
                onSelectAnnotation={handleSelectAnnotation}
                selectedId={selectedAnnotation?.id}
              />

              {/* ONLY ONE Single "Dive In: Explore the OS" Button (Centered on front screen) */}
              <Animated.View
                style={[
                  styles.centerDiveInWrapper,
                  {
                    opacity: diveInFadeAnim,
                    transform: [{ scale: diveInPulseAnim }],
                  },
                ]}
                pointerEvents={isFinalFrontScreen ? 'auto' : 'none'}
              >
                <TouchableOpacity
                  style={styles.centerDiveInButton}
                  onPress={handleDiveIn}
                  activeOpacity={0.85}
                  accessibilityLabel="Dive In: Explore the OS"
                >
                  <View style={styles.centerDiveInIconGlow}>
                    <Ionicons name="sparkles" size={20} color="#FFFFFF" />
                  </View>
                  <Text style={styles.centerDiveInText}>Dive In: Explore the OS</Text>
                  <Ionicons name="arrow-forward-circle" size={26} color="#FFFFFF" />
                </TouchableOpacity>
              </Animated.View>

              {/* Gentle Initial Scroll Prompt */}
              {progress < 0.04 && (
                <View style={styles.scrollPromptBadge} pointerEvents="none">
                  <Ionicons name="chevron-down" size={18} color="rgba(255, 255, 255, 0.9)" />
                  <Text style={styles.scrollPromptText}>Scroll down to disassemble</Text>
                </View>
              )}

              {/* Reverse Scroll Indicator when reaching front screen */}
              {isFinalFrontScreen && (
                <View style={styles.reversePromptBadge} pointerEvents="none">
                  <Ionicons name="chevron-up" size={14} color="rgba(255, 255, 255, 0.7)" />
                  <Text style={styles.reversePromptText}>Scroll up to reverse</Text>
                </View>
              )}
            </Animated.View>

            {/* Clean Component Details Popup Modal (Opens when ANY tag is clicked) */}
            {selectedAnnotation && (
              <View style={styles.modalBackdrop}>
                <TouchableOpacity
                  style={StyleSheet.absoluteFill}
                  activeOpacity={1}
                  onPress={() => setSelectedAnnotation(null)}
                />
                <View style={styles.modalCard}>
                  {/* Modal Header */}
                  <View style={styles.modalHeaderRow}>
                    <View style={styles.modalHeaderLeft}>
                      <View
                        style={[
                          styles.modalIconBox,
                          { backgroundColor: selectedAnnotation.accentColor },
                        ]}
                      >
                        <Ionicons
                          name={selectedAnnotation.icon as any}
                          size={22}
                          color="#FFFFFF"
                        />
                      </View>
                      <View style={{ flex: 1 }}>
                        <Text style={styles.modalComponentName}>
                          {selectedAnnotation.telemetry.technicalName}
                        </Text>
                        <Text
                          style={[
                            styles.modalComponentSubtitle,
                            { color: selectedAnnotation.accentColor },
                          ]}
                        >
                          {selectedAnnotation.telemetry.subtitle}
                        </Text>
                      </View>
                    </View>

                    <TouchableOpacity
                      style={styles.modalCloseButton}
                      onPress={() => setSelectedAnnotation(null)}
                      accessibilityLabel="Close component details"
                    >
                      <Ionicons name="close" size={20} color="#FFFFFF" />
                    </TouchableOpacity>
                  </View>

                  {/* Summary */}
                  <View style={styles.modalSummaryBox}>
                    <Text style={styles.modalSummaryText}>
                      {selectedAnnotation.telemetry.summary}
                    </Text>
                  </View>

                  {/* Specifications Grid */}
                  <View style={styles.specsGrid}>
                    <View style={styles.specBox}>
                      <Text style={styles.specLabel}>ARCHITECTURE</Text>
                      <Text style={styles.specValue}>
                        {selectedAnnotation.telemetry.architecture}
                      </Text>
                    </View>
                    <View style={styles.specBox}>
                      <Text style={styles.specLabel}>BUS / INTERFACE</Text>
                      <Text style={styles.specValue}>
                        {selectedAnnotation.telemetry.interfaceSpec}
                      </Text>
                    </View>
                  </View>

                  {/* Key Features Bullets */}
                  <View style={styles.featuresList}>
                    <Text style={styles.featuresHeader}>ENGINEERING CAPABILITIES:</Text>
                    {selectedAnnotation.telemetry.keyFeatures.map((feat, i) => (
                      <View key={i} style={styles.featureRow}>
                        <Ionicons
                          name="checkmark-circle"
                          size={15}
                          color={selectedAnnotation.accentColor}
                        />
                        <Text style={styles.featureText}>{feat}</Text>
                      </View>
                    ))}
                  </View>

                  {/* Close / Dismiss Button */}
                  <TouchableOpacity
                    style={styles.modalDismissBtn}
                    onPress={() => setSelectedAnnotation(null)}
                    activeOpacity={0.8}
                  >
                    <Text style={styles.modalDismissBtnText}>DONE</Text>
                  </TouchableOpacity>
                </View>
              </View>
            )}
          </View>
        )}

        {/* ============================================================== */}
        {/* MODE 2: OS SIMULATION VIEW (REVEALED ON DIVE IN)               */}
        {/* ============================================================== */}
        {screenMode === 'OS_SIMULATION' && (
          <Animated.View style={[styles.osWrapper, { opacity: osFadeAnim }]}>
            {/* OS Navigation Bar */}
            <View style={styles.topBar}>
              <TouchableOpacity
                style={styles.backHardwareButton}
                onPress={handleBackToTeardown}
                accessibilityLabel="Back to Hardware Teardown"
              >
                <Ionicons name="hardware-chip" size={16} color="#FFFFFF" />
                <Text style={styles.backHardwareText}>HARDWARE TEARDOWN</Text>
              </TouchableOpacity>

              <View style={styles.topBarCenter}>
                <Text style={styles.screenHeaderTitle}>OS SOFTWARE LAYER</Text>
                <Text style={styles.screenHeaderSubtitle}>LIVING SYSTEM RUNTIME</Text>
              </View>

              <TouchableOpacity
                style={styles.circleIconButton}
                onPress={() => router.push('/kernel')}
                accessibilityLabel="Open Full Kernel Stack"
              >
                <Ionicons name="layers-outline" size={18} color={Colors.textWhite} />
              </TouchableOpacity>
            </View>

            {/* Segmented Feature Toggle */}
            <View style={styles.osTabPillsRow}>
              <TouchableOpacity
                style={[styles.osTabPill, osTab === 'SIMULATOR' && styles.osTabPillActive]}
                onPress={() => setOsTab('SIMULATOR')}
                activeOpacity={0.8}
              >
                <Ionicons
                  name="git-branch"
                  size={14}
                  color={osTab === 'SIMULATOR' ? '#FFFFFF' : Colors.inkLight}
                />
                <Text
                  style={[
                    styles.osTabPillText,
                    osTab === 'SIMULATOR' && styles.osTabPillTextActive,
                  ]}
                >
                  Execution Pipeline (Decision Tree)
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.osTabPill, osTab === 'DEVICE_OS' && styles.osTabPillActive]}
                onPress={() => setOsTab('DEVICE_OS')}
                activeOpacity={0.8}
              >
                <Ionicons
                  name="speedometer-outline"
                  size={14}
                  color={osTab === 'DEVICE_OS' ? '#FFFFFF' : Colors.inkLight}
                />
                <Text
                  style={[
                    styles.osTabPillText,
                    osTab === 'DEVICE_OS' && styles.osTabPillTextActive,
                  ]}
                >
                  My Device OS
                </Text>
              </TouchableOpacity>
            </View>

            {/* OS Simulation Content */}
            <ScrollView
              contentContainerStyle={styles.osScrollContent}
              showsVerticalScrollIndicator={false}
            >
              {osTab === 'SIMULATOR' && <AppExecutionSimulator />}
              {osTab === 'DEVICE_OS' && <MyDeviceInspector />}
            </ScrollView>
          </Animated.View>
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#DD5622',
  },
  container: {
    flex: 1,
    backgroundColor: '#DD5622',
  },
  teardownWrapper: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    position: 'relative',
  },
  osWrapper: {
    flex: 1,
    backgroundColor: '#0A0A10',
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    zIndex: 10,
  },
  circleIconButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(0, 0, 0, 0.35)',
    justifyContent: 'center',
    alignItems: 'center',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  topBarCenter: {
    alignItems: 'center',
  },
  screenHeaderTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 14,
    letterSpacing: 1,
    fontWeight: '800',
  },
  screenHeaderSubtitle: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: 9,
    marginTop: 2,
    fontWeight: '700',
  },
  backHardwareButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  backHardwareText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '800',
  },
  viewportContainer: {
    flex: 1,
    position: 'relative',
    marginHorizontal: 14,
    marginTop: 4,
    marginBottom: 16,
    borderRadius: 28,
    overflow: 'hidden',
    backgroundColor: '#0A0A10',
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.18)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  frameImage: {
    width: '100%',
    height: '100%',
  },
  centerDiveInWrapper: {
    position: 'absolute',
    alignSelf: 'center',
    bottom: '12%',
    borderRadius: 32,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.6,
    shadowRadius: 18,
    elevation: 20,
    zIndex: 9999,
  },
  centerDiveInButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: '#DD5622',
    borderWidth: 2,
    borderColor: '#FFA07A',
    paddingVertical: 16,
    paddingHorizontal: 26,
    borderRadius: 32,
    pointerEvents: 'auto',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  centerDiveInIconGlow: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: 'rgba(255, 255, 255, 0.25)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  centerDiveInText: {
    ...Typography.headline,
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
    letterSpacing: 0.5,
  },
  scrollPromptBadge: {
    position: 'absolute',
    bottom: 24,
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.65)',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 22,
    gap: 3,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.15)',
  },
  scrollPromptText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
  },
  reversePromptBadge: {
    position: 'absolute',
    bottom: 16,
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 16,
    gap: 2,
  },
  reversePromptText: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.7)',
    fontSize: 9,
    fontWeight: '600',
  },
  modalBackdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0, 0, 0, 0.72)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
    zIndex: 10000,
  },
  modalCard: {
    backgroundColor: '#161624',
    borderRadius: 24,
    padding: 20,
    width: '100%',
    maxWidth: 420,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.15)',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.6,
    shadowRadius: 20,
    elevation: 20,
    gap: 12,
    zIndex: 10001,
  },
  modalHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  modalHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    flex: 1,
  },
  modalIconBox: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalComponentName: {
    ...Typography.headline,
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '800',
  },
  modalComponentSubtitle: {
    ...Typography.tag,
    fontSize: 10,
    fontWeight: '700',
    marginTop: 2,
  },
  modalCloseButton: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: 'rgba(255, 255, 255, 0.12)',
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 8,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  modalSummaryBox: {
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 12,
    padding: 12,
    borderLeftWidth: 3,
    borderLeftColor: Colors.studioOrange,
  },
  modalSummaryText: {
    ...Typography.body,
    color: '#E5E7EB',
    fontSize: 13,
    lineHeight: 19,
  },
  specsGrid: {
    flexDirection: 'row',
    gap: 8,
  },
  specBox: {
    flex: 1,
    backgroundColor: '#0E0E18',
    borderRadius: 12,
    padding: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 3,
  },
  specLabel: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  specValue: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
    lineHeight: 14,
  },
  featuresList: {
    gap: 6,
  },
  featuresHeader: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  featureRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
  },
  featureText: {
    ...Typography.bodySmall,
    color: '#D1D5DB',
    fontSize: 11,
    lineHeight: 16,
    flex: 1,
  },
  modalDismissBtn: {
    backgroundColor: Colors.studioOrange,
    paddingVertical: 12,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 4,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  modalDismissBtnText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  osTabPillsRow: {
    flexDirection: 'row',
    backgroundColor: '#141422',
    padding: 6,
    marginHorizontal: 16,
    marginTop: 6,
    marginBottom: 10,
    borderRadius: 16,
    gap: 8,
  },
  osTabPill: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 12,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  osTabPillActive: {
    backgroundColor: Colors.studioOrange,
    shadowColor: Colors.studioOrange,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.4,
    shadowRadius: 6,
    elevation: 4,
  },
  osTabPillText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 11,
    fontWeight: '700',
  },
  osTabPillTextActive: {
    color: '#FFFFFF',
    fontWeight: '800',
  },
  osScrollContent: {
    padding: 16,
    gap: 16,
    paddingBottom: 40,
  },
});
