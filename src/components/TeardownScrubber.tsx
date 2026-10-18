import React from 'react';
import { View, Text, StyleSheet, Platform, TouchableOpacity } from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Shapes } from '../theme/shapes';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

interface TeardownScrubberProps {
  progress: number; // 0..1
  currentFrame: number;
  totalFrames: number;
  onProgressChange: (newProgress: number) => void;
  onRewind?: () => void;
  onExplode?: () => void;
  isHapticsEnabled?: boolean;
}

export const TeardownScrubber: React.FC<TeardownScrubberProps> = ({
  progress,
  currentFrame,
  totalFrames,
  onProgressChange,
  onRewind,
  onExplode,
  isHapticsEnabled = true,
}) => {
  const handleSliderTouch = (event: any) => {
    const { locationX } = event.nativeEvent;
    // Assume container width of roughly 280-320 or calculate dynamically
  };

  const triggerHaptic = () => {
    if (isHapticsEnabled && Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      } catch (e) {
        // ignore
      }
    }
  };

  const stepForward = () => {
    triggerHaptic();
    onProgressChange(Math.min(1, progress + 0.05));
  };

  const stepBackward = () => {
    triggerHaptic();
    onProgressChange(Math.max(0, progress - 0.05));
  };

  return (
    <View style={styles.container}>
      <View style={styles.headerRow}>
        <View style={styles.badgeRow}>
          <View style={styles.frameBadge}>
            <Text style={styles.frameBadgeText}>
              FRAME {String(currentFrame).padStart(2, '0')} / {totalFrames - 1}
            </Text>
          </View>
          <Text style={styles.percentageText}>{Math.round(progress * 100)}% EXPLODED</Text>
        </View>

        <View style={styles.quickActions}>
          <TouchableOpacity
            style={styles.microButton}
            onPress={() => {
              triggerHaptic();
              onRewind ? onRewind() : onProgressChange(0);
            }}
            accessibilityLabel="Rewind to fully assembled"
          >
            <Ionicons name="refresh" size={14} color={Colors.textLight} />
            <Text style={styles.microButtonText}>ASSEMBLE</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.microButton, styles.microButtonActive]}
            onPress={() => {
              triggerHaptic();
              onExplode ? onExplode() : onProgressChange(1);
            }}
            accessibilityLabel="Fast forward to exploded"
          >
            <Ionicons name="hardware-chip-outline" size={14} color={Colors.studioOrange} />
            <Text style={[styles.microButtonText, { color: Colors.studioOrange }]}>EXPLODE</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Interactive Scrub Track */}
      <View style={styles.trackContainer}>
        <TouchableOpacity
          activeOpacity={1}
          style={styles.trackBackground}
          onPress={(e) => {
            // Calculate proportional scrub position
            triggerHaptic();
            const { locationX } = e.nativeEvent;
            // standard bar length estimate
            const estimatedWidth = 320;
            const newP = Math.max(0, Math.min(1, locationX / estimatedWidth));
            onProgressChange(newP);
          }}
        >
          {/* Milestone markers on track */}
          <View style={[styles.milestoneTick, { left: '22%' }]} />
          <View style={[styles.milestoneTick, { left: '48%' }]} />
          <View style={[styles.milestoneTick, { left: '75%' }]} />

          {/* Active Fill Bar */}
          <View style={[styles.activeTrack, { width: `${Math.round(progress * 100)}%` }]} />
        </TouchableOpacity>

        {/* Scrub Handle */}
        <View
          style={[
            styles.scrubThumb,
            {
              left: `${Math.max(2, Math.min(96, progress * 100))}%`,
            },
          ]}
        >
          <View style={styles.scrubThumbInner} />
        </View>
      </View>

      {/* Step Stepper Buttons */}
      <View style={styles.stepperRow}>
        <TouchableOpacity style={styles.stepButton} onPress={stepBackward}>
          <Ionicons name="play-back" size={16} color={Colors.textLight} />
          <Text style={styles.stepButtonText}>-5% Step</Text>
        </TouchableOpacity>

        <Text style={styles.scrubHint}>Drag or tap anywhere to scrub physical layers</Text>

        <TouchableOpacity style={styles.stepButton} onPress={stepForward}>
          <Text style={styles.stepButtonText}>+5% Step</Text>
          <Ionicons name="play-forward" size={16} color={Colors.textLight} />
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'rgba(20, 20, 28, 0.92)',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    marginHorizontal: 16,
    marginBottom: 8,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  frameBadge: {
    backgroundColor: Colors.studioOrange,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  frameBadgeText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 10,
  },
  percentageText: {
    ...Typography.bodySmall,
    color: Colors.textLight,
    fontWeight: '700',
    fontSize: 12,
  },
  quickActions: {
    flexDirection: 'row',
    gap: 6,
  },
  microButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  microButtonActive: {
    backgroundColor: 'rgba(221, 86, 34, 0.15)',
    borderWidth: 1,
    borderColor: 'rgba(221, 86, 34, 0.4)',
  },
  microButtonText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
  },
  trackContainer: {
    height: 32,
    justifyContent: 'center',
    position: 'relative',
    marginVertical: 4,
  },
  trackBackground: {
    height: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.12)',
    borderRadius: 4,
    position: 'relative',
    overflow: 'hidden',
  },
  activeTrack: {
    height: '100%',
    backgroundColor: Colors.studioOrange,
    borderRadius: 4,
  },
  milestoneTick: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    width: 2,
    backgroundColor: 'rgba(255, 255, 255, 0.4)',
    zIndex: 2,
  },
  scrubThumb: {
    position: 'absolute',
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: Colors.surface,
    marginLeft: -12,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.35,
    shadowRadius: 4,
    elevation: 4,
  },
  scrubThumbInner: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: Colors.studioOrange,
  },
  stepperRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 6,
  },
  stepButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingVertical: 4,
    paddingHorizontal: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    borderRadius: 8,
  },
  stepButtonText: {
    ...Typography.bodySmall,
    color: Colors.textLight,
    fontSize: 11,
    fontWeight: '600',
  },
  scrubHint: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
    fontStyle: 'italic',
    textAlign: 'center',
    flex: 1,
  },
});
