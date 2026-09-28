import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated } from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { TeardownMilestone, TeardownAnnotation } from '../data/teardownData';
import { Ionicons } from '@expo/vector-icons';

interface MilestoneCardProps {
  milestone: TeardownMilestone;
  selectedAnnotation: TeardownAnnotation | null;
  progress: number;
  onClearSelectedAnnotation: () => void;
  onSelectAnnotation: (ann: TeardownAnnotation) => void;
  allAnnotations: TeardownAnnotation[];
  onDiveIn: () => void;
}

export const MilestoneCard: React.FC<MilestoneCardProps> = ({
  milestone,
  selectedAnnotation,
  progress,
  onClearSelectedAnnotation,
  onSelectAnnotation,
  allAnnotations,
  onDiveIn,
}) => {
  const isFrontScreenReady = progress >= 0.88;

  // Pulse animation for Dive In button
  const pulseAnim = useRef(new Animated.Value(1)).current;
  const glowAnim = useRef(new Animated.Value(0.4)).current;

  useEffect(() => {
    if (isFrontScreenReady) {
      Animated.loop(
        Animated.parallel([
          Animated.sequence([
            Animated.timing(pulseAnim, {
              toValue: 1.05,
              duration: 900,
              useNativeDriver: true,
            }),
            Animated.timing(pulseAnim, {
              toValue: 1,
              duration: 900,
              useNativeDriver: true,
            }),
          ]),
          Animated.sequence([
            Animated.timing(glowAnim, {
              toValue: 1,
              duration: 900,
              useNativeDriver: true,
            }),
            Animated.timing(glowAnim, {
              toValue: 0.4,
              duration: 900,
              useNativeDriver: true,
            }),
          ]),
        ])
      ).start();
    }
  }, [isFrontScreenReady]);

  // Current visible annotations based on progress
  const visibleAnnotations = allAnnotations.filter(
    (a) => progress >= a.startProgress && progress <= a.endProgress
  );

  return (
    <View style={styles.sheetContainer}>
      <View style={styles.dragHandleBar} />

      {selectedAnnotation ? (
        // Selected Component Card (Simple, Kid-Friendly Explanation)
        <View style={styles.contentContainer}>
          <View style={styles.headerRow}>
            <View style={styles.titleWithIcon}>
              <View
                style={[
                  styles.componentIconCircle,
                  { backgroundColor: selectedAnnotation.accentColor },
                ]}
              >
                <Ionicons
                  name={selectedAnnotation.icon as any}
                  size={20}
                  color={Colors.textWhite}
                />
              </View>
              <View style={styles.headerTitles}>
                <View style={styles.tagBadge}>
                  <Text style={[styles.tagBadgeText, { color: selectedAnnotation.accentColor }]}>
                    {selectedAnnotation.tag.toUpperCase()}
                  </Text>
                </View>
                <Text style={styles.kidTitle}>{selectedAnnotation.telemetry.simpleTitle}</Text>
              </View>
            </View>

            <TouchableOpacity
              style={styles.closeButton}
              onPress={onClearSelectedAnnotation}
              accessibilityLabel="Close component details"
            >
              <Ionicons name="close" size={20} color={Colors.ink} />
            </TouchableOpacity>
          </View>

          {/* Simple Explanation */}
          <View
            style={[
              styles.quoteBox,
              { borderLeftColor: selectedAnnotation.accentColor },
            ]}
          >
            <Text style={styles.simpleExplanation}>
              "{selectedAnnotation.telemetry.simpleExplanation}"
            </Text>
          </View>

          {/* Fun Detail & Metaphor */}
          <Text style={styles.funDetail}>{selectedAnnotation.telemetry.funDetail}</Text>

          <View style={styles.funStatsRow}>
            <View style={styles.funStatChip}>
              <Ionicons name="sparkles" size={13} color={selectedAnnotation.accentColor} />
              <Text style={styles.funStatText}>{selectedAnnotation.telemetry.kidMetaphor}</Text>
            </View>
            <View style={styles.funStatChip}>
              <Ionicons name="flash-outline" size={13} color={selectedAnnotation.accentColor} />
              <Text style={styles.funStatText}>{selectedAnnotation.telemetry.powerOrSpeed}</Text>
            </View>
          </View>
        </View>
      ) : (
        // Milestone Story View (No engineering jargon, no Phase 4/4, no Specs button)
        <View style={styles.contentContainer}>
          <View style={styles.milestoneHeader}>
            <View style={styles.milestoneBadge}>
              <Text style={styles.milestoneEmoji}>{milestone.emoji}</Text>
              <Text style={styles.milestoneBadgeText}>{milestone.accentTag}</Text>
            </View>

            <View style={styles.scrollTip}>
              <Ionicons name="swap-vertical" size={14} color={Colors.studioOrange} />
              <Text style={styles.scrollTipText}>Scroll up/down to explore</Text>
            </View>
          </View>

          <Text style={styles.milestoneTitle}>{milestone.title}</Text>
          <Text style={styles.milestoneDesc}>{milestone.description}</Text>

          {/* Kid-Friendly Component Quick-Picker when components are visible */}
          {visibleAnnotations.length > 0 && !isFrontScreenReady && (
            <View style={styles.quickPartsContainer}>
              <Text style={styles.quickPartsLabel}>Tap any part to see what it does:</Text>
              <View style={styles.quickPartsRow}>
                {visibleAnnotations.map((ann) => (
                  <TouchableOpacity
                    key={ann.id}
                    style={[styles.quickPartButton, { borderColor: ann.accentColor }]}
                    onPress={() => onSelectAnnotation(ann)}
                    activeOpacity={0.75}
                  >
                    <Ionicons name={ann.icon as any} size={14} color={ann.accentColor} />
                    <Text style={styles.quickPartName}>{ann.name}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          )}

          {/* "Dive In: Explore the OS" CTA when user reaches fully assembled front screen */}
          {isFrontScreenReady && (
            <Animated.View
              style={[
                styles.diveInCtaWrap,
                {
                  transform: [{ scale: pulseAnim }],
                  shadowOpacity: glowAnim,
                },
              ]}
            >
              <TouchableOpacity
                style={styles.diveInCtaButton}
                onPress={onDiveIn}
                activeOpacity={0.88}
                accessibilityLabel="Dive In: Explore the OS"
              >
                <View style={styles.diveInTextCol}>
                  <Text style={styles.diveInTitle}>Dive In: Explore the OS</Text>
                  <Text style={styles.diveInSubtitle}>
                    Zoom through the glass screen into the computer brain!
                  </Text>
                </View>
                <View style={styles.diveInIconCircle}>
                  <Ionicons name="arrow-forward" size={22} color="#FFFFFF" />
                </View>
              </TouchableOpacity>
            </Animated.View>
          )}
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  sheetContainer: {
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    paddingTop: 10,
    paddingBottom: 20,
    paddingHorizontal: 18,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.16,
    shadowRadius: 12,
    elevation: 12,
  },
  dragHandleBar: {
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#D1D5DB',
    alignSelf: 'center',
    marginBottom: 10,
  },
  contentContainer: {
    gap: 8,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  titleWithIcon: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  componentIconCircle: {
    width: 38,
    height: 38,
    borderRadius: 19,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitles: {
    flex: 1,
  },
  tagBadge: {
    alignSelf: 'flex-start',
  },
  tagBadgeText: {
    ...Typography.tag,
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  kidTitle: {
    ...Typography.headline,
    fontSize: 17,
    color: Colors.ink,
    fontWeight: '800',
  },
  closeButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#F3F4F6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  quoteBox: {
    backgroundColor: '#F9FAFB',
    borderLeftWidth: 4,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    marginTop: 2,
  },
  simpleExplanation: {
    ...Typography.body,
    color: '#111827',
    fontSize: 14,
    fontWeight: '700',
    lineHeight: 20,
  },
  funDetail: {
    ...Typography.bodySmall,
    color: '#4B5563',
    fontSize: 13,
    lineHeight: 18,
    marginTop: 2,
  },
  funStatsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 4,
  },
  funStatChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: '#F3F4F6',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 20,
  },
  funStatText: {
    ...Typography.tag,
    color: '#374151',
    fontSize: 11,
    fontWeight: '600',
  },
  milestoneHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  milestoneBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: '#FFF4EE',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 14,
  },
  milestoneEmoji: {
    fontSize: 14,
  },
  milestoneBadgeText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 10,
    fontWeight: '800',
  },
  scrollTip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  scrollTipText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 11,
    fontWeight: '600',
  },
  milestoneTitle: {
    ...Typography.headline,
    color: Colors.ink,
    fontSize: 18,
    fontWeight: '800',
  },
  milestoneDesc: {
    ...Typography.body,
    color: '#4B5563',
    fontSize: 13,
    lineHeight: 19,
  },
  quickPartsContainer: {
    marginTop: 4,
    gap: 6,
  },
  quickPartsLabel: {
    ...Typography.tag,
    color: '#6B7280',
    fontSize: 10,
    fontWeight: '600',
  },
  quickPartsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  quickPartButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 12,
    borderWidth: 1.5,
    backgroundColor: '#F9FAFB',
  },
  quickPartName: {
    ...Typography.tag,
    color: '#1F2937',
    fontSize: 11,
    fontWeight: '600',
  },
  diveInCtaWrap: {
    marginTop: 10,
    borderRadius: 18,
    shadowColor: Colors.studioOrange,
    shadowOffset: { width: 0, height: 4 },
    shadowRadius: 10,
    elevation: 8,
  },
  diveInCtaButton: {
    backgroundColor: Colors.studioOrange,
    borderRadius: 18,
    paddingVertical: 14,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  diveInTextCol: {
    flex: 1,
    gap: 2,
  },
  diveInTitle: {
    ...Typography.headline,
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  diveInSubtitle: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.9)',
    fontSize: 11,
    fontWeight: '500',
  },
  diveInIconCircle: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255, 255, 255, 0.25)',
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 10,
  },
});
