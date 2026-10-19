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
  getMilestoneForProgress,
} from '../src/data/teardownData';
import { AnnotationOverlay } from '../src/components/AnnotationOverlay';
import { TeardownScrubber } from '../src/components/TeardownScrubber';
import { MilestoneCard } from '../src/components/MilestoneCard';
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
 * 4. Front AMOLED display holds comfortably (+1 extra dwell step) before transition
 * 5. Fluid, continuous bidirectional transition between Teardown and OS layer without black buffer glitches
 */
// Sequence Step Constants:
// 60 teardown frames (0..58) + 1 extra dwell step on frame 59 (steps 59..60)
const TOTAL_SEQUENCE_STEPS = TOTAL_FRAMES + 1; // 61 total sequence steps (0..60)
const FRAME_59_PROGRESS = (TOTAL_FRAMES - 1) / (TOTAL_SEQUENCE_STEPS - 1); // 59 / 60 ≈ 0.9833

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

  // Ensure frames are preloaded even on direct navigation to /teardown
  useEffect(() => {
    Asset.loadAsync(TEARDOWN_FRAME_ASSETS).catch(() => {});
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

  const handleProgressChange = useCallback((newProgress: number) => {
    const clamped = Math.max(0, Math.min(1.0, newProgress));
    targetProgress.current = clamped;
    interpolatedProgress.current = clamped;
    lastEmittedProgress.current = clamped;
    setProgress(clamped);
  }, []);

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

          if (step > 0 && currentTarget >= 0.995 && interpolatedProgress.current >= 0.985) {
            // Reached the front screen dwell step -> downward scroll triggers OS transition
            overscrollAccumulator.current += step;
            if (overscrollAccumulator.current >= 0.003) {
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

        if (nextTarget > 1.01) {
          overscrollAccumulator.current += 0.005;
          if (overscrollAccumulator.current >= 0.01) {
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
        if (targetProgress.current >= 0.99 && gestureState.vy < -0.15) {
          // Fling upward at end transitions to OS
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

  // Calculate current sequence step strictly clamped to 0..60 (60 frames + 1 extra dwell step)
  const currentStepIndex = Math.min(
    TOTAL_SEQUENCE_STEPS - 1,
    Math.max(0, Math.round(progress * (TOTAL_SEQUENCE_STEPS - 1)))
  );

  // Clamped hardware frame asset index:
  // Steps 0..58 map to frames 0..58. Steps 59 & 60 both hold frame 59 (flat front display +1 extra dwell step).
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
            {/* Pre-mounted stack of 60 frames kept in memory */}
            <View style={styles.frameImage}>
              {TEARDOWN_FRAME_ASSETS.map((asset, idx) => (
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
                  priority="high"
                  recyclingKey={`teardown-frame-${idx}`}
                />
              ))}
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

          {/* Interactive Hardware Controls & Scrub Progress */}
          <TeardownScrubber
            progress={progress}
            currentFrame={currentFrameIndex}
            totalFrames={TOTAL_FRAMES}
            onProgressChange={handleProgressChange}
            onRewind={() => handleProgressChange(0)}
            onExplode={() => handleProgressChange(1)}
          />

          {/* Milestone Story & Dive In CTA Card */}
          <MilestoneCard
            milestone={getMilestoneForProgress(progress)}
            selectedAnnotation={selectedAnnotation}
            progress={progress}
            onClearSelectedAnnotation={() => setSelectedAnnotation(null)}
            onSelectAnnotation={handleSelectAnnotation}
            allAnnotations={TEARDOWN_ANNOTATIONS}
            onDiveIn={transitionToOS}
          />
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
    marginBottom: 12,
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
});
