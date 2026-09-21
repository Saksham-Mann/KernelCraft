import React, { useState, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  TouchableOpacity,
  Dimensions,
  Platform,
} from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { Colors } from '../src/theme/colors';
import { Typography } from '../src/theme/typography';
import {
  TEARDOWN_FRAME_ASSETS,
  TOTAL_FRAMES,
  TEARDOWN_ANNOTATIONS,
  getMilestoneForProgress,
  TeardownAnnotation,
} from '../src/data/teardownData';
import { TeardownScrubber } from '../src/components/TeardownScrubber';
import { AnnotationOverlay } from '../src/components/AnnotationOverlay';
import { MilestoneCard } from '../src/components/MilestoneCard';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

export default function TeardownScreen() {
  const router = useRouter();
  const [progress, setProgress] = useState(0.0);
  const [selectedAnnotation, setSelectedAnnotation] = useState<TeardownAnnotation | null>(null);
  const [isAudioEnabled, setIsAudioEnabled] = useState(true);
  const [isHapticsEnabled, setIsHapticsEnabled] = useState(true);
  const [viewportSize, setViewportSize] = useState({ width: 0, height: 0 });

  // Calculate current frame index (0..39)
  const currentFrameIndex = Math.min(
    TOTAL_FRAMES - 1,
    Math.max(0, Math.round(progress * (TOTAL_FRAMES - 1)))
  );

  const currentMilestone = getMilestoneForProgress(progress);

  const handleProgressChange = (newProgress: number) => {
    const clamped = Math.max(0, Math.min(1, newProgress));
    setProgress(clamped);

    // If scrubbed far from selected annotation, auto-deselect
    if (selectedAnnotation) {
      if (clamped < selectedAnnotation.startProgress || clamped > selectedAnnotation.endProgress) {
        setSelectedAnnotation(null);
      }
    }
  };

  const handleSelectAnnotation = (ann: TeardownAnnotation) => {
    if (isHapticsEnabled && Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      } catch (e) {}
    }
    setSelectedAnnotation(selectedAnnotation?.id === ann.id ? null : ann);
  };

  const handleDiveIn = () => {
    if (isHapticsEnabled && Platform.OS !== 'web') {
      try {
        Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      } catch (e) {}
    }
    router.push('/transition');
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Top Control Bar */}
        <View style={styles.topBar}>
          <TouchableOpacity
            style={styles.circleIconButton}
            onPress={() => router.replace('/')}
            accessibilityLabel="Back to splash"
          >
            <Ionicons name="chevron-back" size={20} color={Colors.textWhite} />
          </TouchableOpacity>

          <View style={styles.topBarTitleCenter}>
            <Text style={styles.screenHeaderTitle}>HARDWARE TEARDOWN</Text>
            <Text style={styles.screenHeaderSubtitle}>40-FRAME ULTRA PRECISION</Text>
          </View>

          <View style={styles.topBarActions}>
            <TouchableOpacity
              style={[styles.circleIconButton, !isAudioEnabled && styles.circleIconDisabled]}
              onPress={() => setIsAudioEnabled(!isAudioEnabled)}
              accessibilityLabel="Toggle audio synthesizer"
            >
              <Ionicons
                name={isAudioEnabled ? 'volume-medium' : 'volume-mute'}
                size={18}
                color={Colors.textWhite}
              />
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.circleIconButton, !isHapticsEnabled && styles.circleIconDisabled]}
              onPress={() => setIsHapticsEnabled(!isHapticsEnabled)}
              accessibilityLabel="Toggle haptics"
            >
              <Ionicons
                name={isHapticsEnabled ? 'phone-portrait' : 'phone-portrait-outline'}
                size={18}
                color={Colors.textWhite}
              />
            </TouchableOpacity>
          </View>
        </View>

        {/* Center Frame Viewport */}
        <View
          style={styles.viewportContainer}
          onLayout={(e) => {
            const { width, height } = e.nativeEvent.layout;
            setViewportSize({ width, height });
          }}
        >
          {/* Main Teardown Image Frame */}
          <Image
            source={TEARDOWN_FRAME_ASSETS[currentFrameIndex]}
            style={styles.frameImage}
            contentFit="contain"
            transition={50}
          />

          {/* Spatial Pin & Leader-Line Hotspots */}
          <AnnotationOverlay
            annotations={TEARDOWN_ANNOTATIONS}
            progress={progress}
            containerWidth={viewportSize.width}
            containerHeight={viewportSize.height}
            onSelectAnnotation={handleSelectAnnotation}
            selectedId={selectedAnnotation?.id}
          />
        </View>

        {/* Interactive Scrubber */}
        <TeardownScrubber
          progress={progress}
          currentFrame={currentFrameIndex}
          totalFrames={TOTAL_FRAMES}
          onProgressChange={handleProgressChange}
          onRewind={() => handleProgressChange(0)}
          onExplode={() => handleProgressChange(1)}
          isHapticsEnabled={isHapticsEnabled}
        />

        {/* Milestone & Telemetry Bottom Sheet */}
        <MilestoneCard
          milestone={currentMilestone}
          selectedAnnotation={selectedAnnotation}
          progress={progress}
          onClearSelectedAnnotation={() => setSelectedAnnotation(null)}
          onOpenDetailScreen={(id) => router.push(`/detail/${id}` as any)}
          onDiveIn={handleDiveIn}
          onOpenSpecs={() => router.push('/specs')}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: Colors.crimson,
  },
  container: {
    flex: 1,
    backgroundColor: Colors.crimson,
    justifyContent: 'space-between',
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  circleIconButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.badgeDark,
    justifyContent: 'center',
    alignItems: 'center',
  },
  circleIconDisabled: {
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
    opacity: 0.6,
  },
  topBarTitleCenter: {
    alignItems: 'center',
  },
  screenHeaderTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 14,
    letterSpacing: 1,
  },
  screenHeaderSubtitle: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.7)',
    fontSize: 8,
    marginTop: 1,
  },
  topBarActions: {
    flexDirection: 'row',
    gap: 8,
  },
  viewportContainer: {
    flex: 1,
    position: 'relative',
    marginHorizontal: 12,
    marginVertical: 6,
    borderRadius: 20,
    overflow: 'hidden',
    backgroundColor: '#0D0D12',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  frameImage: {
    width: '100%',
    height: '100%',
  },
});
