import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  TouchableOpacity,
  Platform,
  Animated,
  PanResponder,
  Dimensions,
} from 'react-native';
import Svg, { Path } from 'react-native-svg';
import { Image } from 'expo-image';
import { Asset } from 'expo-asset';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { Colors } from '../src/theme/colors';
import { Typography, FontFamilies } from '../src/theme/typography';
import {
  TEARDOWN_FRAME_ASSETS,
  TOTAL_FRAMES,
  TEARDOWN_ANNOTATIONS,
  TeardownAnnotation,
} from '../src/data/teardownData';
import { AnnotationOverlay } from '../src/components/AnnotationOverlay';
import { KernelStackExplorerView } from './kernel';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

/**
 * Hardware Teardown Screen with Seamless OS Scroll Transition
 *
 * Implements:
 * 1. Clean header with ONLY "HARDWARE TEARDOWN" (Plus Jakarta Sans, no subtitles)
 * 2. High-performance PanResponder drag scrubbing for Mobile (iOS/Android) and Web
 * 3. Stable component inspection pins with generous touch hitboxes
 * 4. Front AMOLED display holds comfortably with extended dwell (+2 extra dwell steps)
 * 5. Fluid, continuous bidirectional transition between Teardown and OS layer without glitches
 */
// Sequence Step Constants:
// 60 teardown frames (0..58) + 2 extra dwell steps on frame 59 (steps 59..61) + 1 exit buffer step (step 62)
const DWELL_STEPS = 2; // Extended front display dwell (+1 extra scroll step)
const BUFFER_STEPS = 1; // Exit sequence buffer where red AMOLED pin cleanly disappears before OS transition
const TOTAL_SEQUENCE_STEPS = TOTAL_FRAMES + DWELL_STEPS + BUFFER_STEPS; // 63 total sequence steps (0..62)
const FRAME_59_PROGRESS = (TOTAL_FRAMES - 1) / (TOTAL_SEQUENCE_STEPS - 1); // 59 / 62 ≈ 0.9516
const RED_PIN_END_PROGRESS = (TOTAL_FRAMES - 1 + DWELL_STEPS) / (TOTAL_SEQUENCE_STEPS - 1); // 61 / 62 ≈ 0.9839

export default function TeardownScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();

  // Initial stage / progress check
  const startAtEnd = params?.initialProgress === '1' || params?.stage === 'os';
  const initialStageIsOS = params?.stage === 'os';
  const [activeSection, setActiveSection] = useState<'TEARDOWN' | 'OS'>(
    initialStageIsOS ? 'OS' : 'TEARDOWN'
  );

  // Target progress (user scroll/drag intent) and Interpolated progress (smooth fluid lerp)
  // When starting at end of teardown, land at frame 59 (flat front display)
  const initialProgressVal = startAtEnd ? 1.0 : 0.0;
  const targetProgress = useRef(initialProgressVal);
  const interpolatedProgress = useRef(initialProgressVal);
  const [progress, setProgress] = useState(initialProgressVal);

  const [selectedAnnotation, setSelectedAnnotation] = useState<TeardownAnnotation | null>(null);
  const [viewportSize, setViewportSize] = useState(() => {
    const { width, height } = Dimensions.get('window');
    return {
      width: Math.min(width - 28, 412),
      height: Math.min(height - 120, 780),
    };
  });

  const activeSectionRef = useRef<'TEARDOWN' | 'OS'>(activeSection);
  activeSectionRef.current = activeSection;

  const selectedAnnotationRef = useRef<TeardownAnnotation | null>(null);
  selectedAnnotationRef.current = selectedAnnotation;

  // Transition animation between Teardown (0) and OS Section (1)
  const transitionAnim = useRef(new Animated.Value(initialStageIsOS ? 1 : 0)).current;
  const isTransitioningRef = useRef(false);
  const overscrollAccumulator = useRef(0);

  // Render frame 0 instantly, then progressively preload remaining frames (01 to 59) in background
  const [mountedFramesCount, setMountedFramesCount] = useState<number>(1);

  useEffect(() => {
    let isCancelled = false;

    // Load initial frame immediately (or frame 59 if starting at end)
    const initialFrameAsset = startAtEnd
      ? TEARDOWN_FRAME_ASSETS[TOTAL_FRAMES - 1]
      : TEARDOWN_FRAME_ASSETS[0];
    Asset.loadAsync(initialFrameAsset).catch(() => {});

    // Batched asynchronous background preload of remaining frames 1..59 using requestIdleCallback
    const remainingAssets = TEARDOWN_FRAME_ASSETS.slice(1);
    const BATCH_SIZE = 6;

    const preloadBatch = async (batchStart: number) => {
      if (isCancelled || batchStart >= remainingAssets.length) return;

      const batch = remainingAssets.slice(batchStart, batchStart + BATCH_SIZE);
      try {
        const loaded = await Asset.loadAsync(batch);
        const uris = loaded
          .map((a) => a.localUri || a.uri)
          .filter((u): u is string => Boolean(u));
        if (uris.length > 0) {
          await Image.prefetch(uris, 'memory-disk');
        }
      } catch (e) {}

      if (!isCancelled) {
        setMountedFramesCount((prev) => Math.max(prev, 1 + batchStart + batch.length));
        const nextStart = batchStart + BATCH_SIZE;
        if (nextStart < remainingAssets.length) {
          const scheduleNext =
            typeof (window as any)?.requestIdleCallback === 'function'
              ? (window as any).requestIdleCallback
              : (cb: any) => setTimeout(cb, 50);

          scheduleNext(() => preloadBatch(nextStart), { timeout: 250 });
        }
      }
    };

    // Kick off progressive background preloading after first paint
    const scheduleFirst =
      typeof (window as any)?.requestIdleCallback === 'function'
        ? (window as any).requestIdleCallback
        : (cb: any) => setTimeout(cb, 50);

    scheduleFirst(() => preloadBatch(0), { timeout: 200 });

    return () => {
      isCancelled = true;
    };
  }, []);

  // Continuous smooth physics loop using requestAnimationFrame (60fps/120fps fluid lerp)
  const lastEmittedProgress = useRef(initialProgressVal);

  useEffect(() => {
    if (activeSection !== 'TEARDOWN') return;

    let rafId: number;
    const updatePhysics = () => {
      const target = targetProgress.current;
      const current = interpolatedProgress.current;
      const diff = target - current;

      if (Math.abs(diff) > 0.0003) {
        // Smooth exponential damping (responsive & fluid momentum)
        const next = current + diff * 0.18;
        interpolatedProgress.current = next;

        const nextStep = Math.round(next * (TOTAL_SEQUENCE_STEPS - 1));
        const lastStep = Math.round(lastEmittedProgress.current * (TOTAL_SEQUENCE_STEPS - 1));
        if (nextStep !== lastStep || Math.abs(next - lastEmittedProgress.current) >= 0.002) {
          lastEmittedProgress.current = next;
          setProgress(next);
        }
      } else if (current !== target || lastEmittedProgress.current !== target) {
        interpolatedProgress.current = target;
        lastEmittedProgress.current = target;
        setProgress(target);
      }

      rafId = requestAnimationFrame(updatePhysics);
    };

    rafId = requestAnimationFrame(updatePhysics);
    return () => cancelAnimationFrame(rafId);
  }, [activeSection]);

  // Seamless transition into OS Section (smooth zoom into the glass display)
  const transitionToOS = useCallback(() => {
    if (isTransitioningRef.current) return;
    isTransitioningRef.current = true;

    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      } catch (e) {}
    }

    Animated.timing(transitionAnim, {
      toValue: 1,
      duration: 350,
      useNativeDriver: true,
    }).start(() => {
      setActiveSection('OS');
      isTransitioningRef.current = false;
    });
  }, [transitionAnim]);

  // Seamless transition back up to Hardware Teardown (at final assembled front frame 59)
  const transitionToTeardown = useCallback(() => {
    if (isTransitioningRef.current) return;
    isTransitioningRef.current = true;

    // Reset teardown position to the fully assembled front screen (frame 59 dwell)
    targetProgress.current = 1.0;
    interpolatedProgress.current = 1.0;
    setProgress(1.0);
    setActiveSection('TEARDOWN');
    overscrollAccumulator.current = 0;

    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      } catch (e) {}
    }

    Animated.timing(transitionAnim, {
      toValue: 0,
      duration: 320,
      useNativeDriver: true,
    }).start(() => {
      isTransitioningRef.current = false;
    });
  }, [transitionAnim]);

  // Wheel listener for Web: Smooth scrub through 60 frames and seamless handoff into OS
  useEffect(() => {
    if (Platform.OS === 'web' && typeof window !== 'undefined') {
      const handleWheel = (e: WheelEvent) => {
        if (selectedAnnotationRef.current) {
          // Do not scroll background if a detail modal is open
          return;
        }

        if (activeSectionRef.current === 'TEARDOWN') {
          e.preventDefault();

          const rawDelta = e.deltaY;
          const clampedDelta = Math.max(-80, Math.min(80, rawDelta));
          // Calibrated wheel sensitivity for natural, smooth pacing
          const step = clampedDelta * 0.00035;

          const currentTarget = targetProgress.current;
          const nextTarget = currentTarget + step;

          if (step > 0 && currentTarget >= 0.999 && interpolatedProgress.current >= 0.995) {
            // Reached the end of the buffer step -> downward scroll triggers OS transition
            overscrollAccumulator.current += step;
            if (overscrollAccumulator.current >= 0.025) {
              overscrollAccumulator.current = 0;
              transitionToOS();
              return;
            }
          } else if (step < 0) {
            // Reversing upward: clear overscroll and scrub frames in reverse immediately
            overscrollAccumulator.current = 0;
            const clamped = Math.max(0, Math.min(1.0, nextTarget));
            targetProgress.current = clamped;
          } else {
            const clamped = Math.max(0, Math.min(1.0, nextTarget));
            targetProgress.current = clamped;
          }
        }
      };

      window.addEventListener('wheel', handleWheel, { passive: false });
      return () => {
        window.removeEventListener('wheel', handleWheel);
      };
    }
  }, [transitionToOS]);

  // PanResponder: Native touch & drag gesture system for Mobile (iOS/Android) and Web
  const touchStartProgress = useRef(0);
  const isDragging = useRef(false);

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onStartShouldSetPanResponderCapture: () => false,
      onMoveShouldSetPanResponder: (_, gestureState) => {
        if (activeSectionRef.current !== 'TEARDOWN' || selectedAnnotationRef.current) {
          return false;
        }
        return Math.abs(gestureState.dy) > 4;
      },
      onMoveShouldSetPanResponderCapture: (_, gestureState) => {
        if (activeSectionRef.current !== 'TEARDOWN' || selectedAnnotationRef.current) {
          return false;
        }
        return Math.abs(gestureState.dy) > 4;
      },
      onPanResponderGrant: () => {
        isDragging.current = true;
        touchStartProgress.current = targetProgress.current;
        overscrollAccumulator.current = 0;
      },
      onPanResponderMove: (_, gestureState) => {
        if (activeSectionRef.current !== 'TEARDOWN' || selectedAnnotationRef.current) return;

        // Invert dy: dragging finger upward (negative dy) scrubs forward (increases progress)
        // Dragging finger downward (positive dy) scrubs backward (decreases progress)
        const travelDistance = 500; // Calibrated travel distance for 60 frames
        const deltaProgress = -gestureState.dy / travelDistance;
        const nextTarget = touchStartProgress.current + deltaProgress;

        if (nextTarget > 1.015) {
          overscrollAccumulator.current += 0.005;
          if (overscrollAccumulator.current >= 0.015) {
            overscrollAccumulator.current = 0;
            transitionToOS();
          }
        } else if (nextTarget < 0) {
          targetProgress.current = 0;
        } else {
          targetProgress.current = Math.min(1.0, nextTarget);
        }
      },
      onPanResponderRelease: (_, gestureState) => {
        isDragging.current = false;
        if (targetProgress.current >= 0.999 && gestureState.vy < -0.3) {
          // Fling upward at end of buffer transitions to OS
          transitionToOS();
        } else if (targetProgress.current > 1.0) {
          transitionToOS();
        } else {
          targetProgress.current = Math.max(0, Math.min(1.0, targetProgress.current));
        }
      },
      onPanResponderTerminate: () => {
        isDragging.current = false;
        targetProgress.current = Math.max(0, Math.min(1.0, targetProgress.current));
      },
    })
  ).current;

  // Calculate current sequence step strictly clamped to 0..62 (60 frames + 2 dwell steps + 1 exit buffer step)
  const currentStepIndex = Math.min(
    TOTAL_SEQUENCE_STEPS - 1,
    Math.max(0, Math.round(progress * (TOTAL_SEQUENCE_STEPS - 1)))
  );

  // Clamped hardware frame asset index:
  // Steps 0..58 map to frames 0..58. Steps 59..62 all hold frame 59 (flat front display + extended dwell & buffer).
  const currentFrameIndex = Math.min(TOTAL_FRAMES - 1, currentStepIndex);

  // Click any hardware pin or badge -> open clean modal
  const handleSelectAnnotation = (ann: TeardownAnnotation) => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      } catch (e) {}
    }
    setSelectedAnnotation(ann);
  };

  // Auto-dismiss modal if scrolled away from component visibility range
  useEffect(() => {
    if (selectedAnnotation) {
      if (
        progress < selectedAnnotation.startProgress - 0.08 ||
        progress > selectedAnnotation.endProgress + 0.08
      ) {
        setSelectedAnnotation(null);
      }
    }
  }, [progress, selectedAnnotation]);

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* SECTION 1: HARDWARE TEARDOWN VIEW */}
        <Animated.View
          pointerEvents={activeSection === 'TEARDOWN' ? 'auto' : 'none'}
          style={[
            styles.teardownWrapper,
            {
              opacity: transitionAnim.interpolate({
                inputRange: [0, 0.75, 1],
                outputRange: [1, 0.2, 0],
              }),
              transform: [
                {
                  scale: transitionAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [1, 1.1],
                  }),
                },
              ],
            },
          ]}
          {...panResponder.panHandlers}
        >
          {/* Top Bar Header (Clean title ONLY, vector chevron back button) */}
          <View style={styles.topBar}>
            <TouchableOpacity
              style={styles.circleIconButton}
              onPress={() => router.replace('/')}
              accessibilityRole="button"
              accessibilityLabel="Back to splash"
            >
              <Svg width={22} height={22} viewBox="0 0 24 24" fill="none">
                <Path
                  d="M15 19L8 12L15 5"
                  stroke="#FFFFFF"
                  strokeWidth={3}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </Svg>
            </TouchableOpacity>

            <View style={styles.topBarCenter}>
              <Text style={styles.screenHeaderTitle}>HARDWARE TEARDOWN</Text>
            </View>

            {/* Spacer to keep center title balanced */}
            <View style={{ width: 38, height: 38 }} />
          </View>

          {/* Main Phone Viewport (Fills primary vertical space) */}
          <View
            style={styles.viewportContainer}
            onLayout={(e) => {
              const { width, height } = e.nativeEvent.layout;
              setViewportSize({ width, height });
            }}
          >
            {/* Progressive frame sequence: Initial frame renders instantly, remaining frames preload in background */}
            <View style={styles.frameImage}>
              {TEARDOWN_FRAME_ASSETS.map((asset, idx) => {
                const isMounted =
                  idx < mountedFramesCount ||
                  idx === currentFrameIndex ||
                  Math.abs(idx - currentFrameIndex) <= 2;
                if (!isMounted) return null;
                return (
                  <Image
                    key={idx}
                    source={asset}
                    style={[
                      StyleSheet.absoluteFill,
                      { opacity: idx === currentFrameIndex ? 1 : 0 },
                    ]}
                    contentFit="contain"
                    transition={0}
                    cachePolicy="memory-disk"
                    priority={idx === 0 || idx === currentFrameIndex ? 'high' : 'low'}
                    recyclingKey={`teardown-frame-${idx}`}
                  />
                );
              })}
            </View>

            {/* Floating Hardware Component Hotspots & Pill Tags */}
            <AnnotationOverlay
              annotations={TEARDOWN_ANNOTATIONS}
              progress={progress}
              containerWidth={viewportSize.width}
              containerHeight={viewportSize.height}
              onSelectAnnotation={handleSelectAnnotation}
              selectedId={selectedAnnotation?.id}
            />
          </View>
        </Animated.View>

        {/* SECTION 2: OS SOFTWARE LAYER VIEW (Continuously Coordinated) */}
        <Animated.View
          pointerEvents={activeSection === 'OS' ? 'auto' : 'none'}
          style={[
            StyleSheet.absoluteFill,
            styles.osSectionWrapper,
            {
              opacity: transitionAnim.interpolate({
                inputRange: [0, 0.25, 1],
                outputRange: [0, 0.4, 1],
              }),
              transform: [
                {
                  translateY: transitionAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [35, 0],
                  }),
                },
              ],
            },
          ]}
        >
          <KernelStackExplorerView
            isActive={activeSection === 'OS'}
            onScrollUpToTeardown={transitionToTeardown}
          />
        </Animated.View>

        {/* Component Inspection Detail Modal Overlay */}
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
                      size={20}
                      color="#FFFFFF"
                    />
                  </View>
                  <View style={styles.modalHeaderTitleCol}>
                    <Text style={styles.modalComponentName}>
                      {selectedAnnotation.telemetry.technicalName}
                    </Text>
                    <Text
                      style={[
                        styles.modalComponentSubtitle,
                        { color: selectedAnnotation.accentColor },
                      ]}
                      numberOfLines={2}
                    >
                      {selectedAnnotation.telemetry.subtitle}
                    </Text>
                  </View>
                </View>

                <TouchableOpacity
                  style={styles.modalCloseButton}
                  onPress={() => setSelectedAnnotation(null)}
                  accessibilityRole="button"
                  accessibilityLabel="Close component details"
                >
                  <Svg width={18} height={18} viewBox="0 0 24 24" fill="none">
                    <Path
                      d="M18 6L6 18M6 6l12 12"
                      stroke="#FFFFFF"
                      strokeWidth={2.2}
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </Svg>
                </TouchableOpacity>
              </View>

              {/* How It Works Section */}
              <View style={styles.modalSummaryBox}>
                <Text style={styles.sectionHeaderLabel}>HOW IT WORKS</Text>
                <Text style={styles.modalSummaryText}>
                  {selectedAnnotation.telemetry.summary}
                </Text>
              </View>

              {/* Key Specs Capsule */}
              {selectedAnnotation.telemetry.keySpecs ? (
                <View style={styles.keySpecsBox}>
                  <View style={styles.keySpecsHeaderRow}>
                    <Ionicons
                      name="hardware-chip-outline"
                      size={13}
                      color={selectedAnnotation.accentColor}
                    />
                    <Text style={styles.sectionHeaderLabel}>KEY SPECS</Text>
                  </View>
                  <Text style={styles.keySpecsText}>
                    {selectedAnnotation.telemetry.keySpecs}
                  </Text>
                </View>
              ) : null}

              {/* Close Button */}
              <TouchableOpacity
                style={[
                  styles.modalDismissBtn,
                  { backgroundColor: selectedAnnotation.accentColor },
                ]}
                onPress={() => setSelectedAnnotation(null)}
                activeOpacity={0.85}
              >
                <Text style={styles.modalDismissBtnText}>Got It</Text>
              </TouchableOpacity>
            </View>
          </View>
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
    position: 'relative',
    overflow: 'hidden',
  },
  teardownWrapper: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#DD5622',
    display: 'flex',
    flexDirection: 'column',
  },
  osSectionWrapper: {
    backgroundColor: '#0A0A10',
    zIndex: 50,
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
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.3)',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  topBarCenter: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  screenHeaderTitle: {
    fontFamily: FontFamilies.sans,
    color: Colors.textWhite,
    fontSize: 15,
    letterSpacing: 1,
    fontWeight: '700',
    textAlign: 'center',
  },
  viewportContainer: {
    flex: 1,
    position: 'relative',
    marginHorizontal: 14,
    marginTop: 4,
    marginBottom: 16,
    borderRadius: 28,
    overflow: 'hidden',
    backgroundColor: '#DD5622',
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.18)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  frameImage: {
    width: '100%',
    height: '100%',
    flex: 1,
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
    maxWidth: 400,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.15)',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 12 },
    shadowOpacity: 0.65,
    shadowRadius: 24,
    elevation: 24,
    gap: 14,
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
    width: 42,
    height: 42,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalHeaderTitleCol: {
    flex: 1,
    gap: 2,
  },
  modalComponentName: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '700',
  },
  modalComponentSubtitle: {
    fontFamily: FontFamilies.sans,
    fontSize: 12,
    fontWeight: '600',
    lineHeight: 16,
  },
  modalCloseButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: 'rgba(255, 255, 255, 0.12)',
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 8,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  sectionHeaderLabel: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: 9,
    fontWeight: '600',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  modalSummaryBox: {
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 14,
    padding: 14,
    borderLeftWidth: 3,
    borderLeftColor: Colors.studioOrange,
    gap: 2,
  },
  modalSummaryText: {
    fontFamily: FontFamilies.sans,
    color: '#E5E7EB',
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '400',
  },
  keySpecsBox: {
    backgroundColor: '#0E0E18',
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 3,
  },
  keySpecsHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  keySpecsText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '500',
    lineHeight: 17,
  },
  modalDismissBtn: {
    paddingVertical: 12,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 2,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  modalDismissBtnText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '600',
    letterSpacing: 0.5,
  },
});
