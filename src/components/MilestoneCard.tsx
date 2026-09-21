import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Shapes } from '../theme/shapes';
import { TeardownMilestone, TeardownAnnotation } from '../data/teardownData';
import { Ionicons } from '@expo/vector-icons';

interface MilestoneCardProps {
  milestone: TeardownMilestone;
  selectedAnnotation: TeardownAnnotation | null;
  progress: number;
  onClearSelectedAnnotation: () => void;
  onOpenDetailScreen: (componentId: string) => void;
  onDiveIn: () => void;
  onOpenSpecs: () => void;
}

export const MilestoneCard: React.FC<MilestoneCardProps> = ({
  milestone,
  selectedAnnotation,
  progress,
  onClearSelectedAnnotation,
  onOpenDetailScreen,
  onDiveIn,
  onOpenSpecs,
}) => {
  const showDiveInCta = progress >= 0.88;

  return (
    <View style={styles.sheetContainer}>
      {/* Peek Drag Handle */}
      <View style={styles.dragHandleBar} />

      {selectedAnnotation ? (
        // Component Telemetry Expansion View
        <View style={styles.contentContainer}>
          <View style={styles.headerRow}>
            <View>
              <View style={styles.badgeRow}>
                <View style={styles.categoryBadge}>
                  <Text style={styles.categoryBadgeText}>{selectedAnnotation.category}</Text>
                </View>
                <Text style={styles.tagText}>{selectedAnnotation.tag}</Text>
              </View>
              <Text style={styles.titleText}>{selectedAnnotation.name}</Text>
            </View>

            <TouchableOpacity
              style={styles.closeButton}
              onPress={onClearSelectedAnnotation}
              accessibilityLabel="Close component details"
            >
              <Ionicons name="close" size={20} color={Colors.ink} />
            </TouchableOpacity>
          </View>

          {/* Quick Specs Grid */}
          <View style={styles.specGrid}>
            <View style={styles.specItem}>
              <Text style={styles.specLabel}>Architecture</Text>
              <Text style={styles.specValue} numberOfLines={2}>
                {selectedAnnotation.telemetry.architecture}
              </Text>
            </View>

            <View style={styles.specItem}>
              <Text style={styles.specLabel}>Bus Interface</Text>
              <Text style={styles.specValue} numberOfLines={1}>
                {selectedAnnotation.telemetry.busInterface}
              </Text>
            </View>

            <View style={styles.specItem}>
              <Text style={styles.specLabel}>Power / Thermal</Text>
              <Text style={styles.specValue} numberOfLines={1}>
                {selectedAnnotation.telemetry.powerDraw} • {selectedAnnotation.telemetry.thermalEnvelope}
              </Text>
            </View>
          </View>

          {/* Detail CTA Row */}
          <View style={styles.actionRow}>
            <TouchableOpacity
              style={styles.deepDiveButton}
              onPress={() => onOpenDetailScreen(selectedAnnotation.id)}
            >
              <Text style={styles.deepDiveButtonText}>COMPONENT DEEP DIVE</Text>
              <Ionicons name="arrow-forward" size={16} color={Colors.textWhite} />
            </TouchableOpacity>
          </View>
        </View>
      ) : (
        // Standard Chapter Milestone View
        <View style={styles.contentContainer}>
          <View style={styles.headerRow}>
            <View style={styles.badgeRow}>
              <View style={styles.milestoneBadge}>
                <Text style={styles.milestoneBadgeText}>{milestone.accentTag}</Text>
              </View>
              <Text style={styles.chapterCounter}>
                PHASE {milestone.id === 'assembled' ? '1' : milestone.id === 'back_cover_removed' ? '2' : milestone.id === 'exploded_layers' ? '3' : '4'} / 4
              </Text>
            </View>

            <TouchableOpacity
              style={styles.specsIconButton}
              onPress={onOpenSpecs}
              accessibilityLabel="Open Full Specs Matrix"
            >
              <Ionicons name="document-text-outline" size={18} color={Colors.ink} />
              <Text style={styles.specsIconText}>SPECS</Text>
            </TouchableOpacity>
          </View>

          <Text style={styles.titleText}>{milestone.title}</Text>
          <Text style={styles.descriptionText}>{milestone.description}</Text>

          {/* Dive-In Button (revealed at >= 88% exploded) */}
          {showDiveInCta ? (
            <TouchableOpacity style={styles.diveInCta} onPress={onDiveIn} activeOpacity={0.88}>
              <View style={styles.diveInTextContainer}>
                <Text style={styles.diveInTitle}>DIVE INTO SILICON DIE</Text>
                <Text style={styles.diveInSubtitle}>Enter 4nm SoC Microarchitecture & Kernel Stack</Text>
              </View>
              <View style={styles.diveInIconCircle}>
                <Ionicons name="scan-outline" size={24} color={Colors.textWhite} />
              </View>
            </TouchableOpacity>
          ) : (
            <View style={styles.scrollTipRow}>
              <Ionicons name="swap-vertical" size={16} color={Colors.inkMuted} />
              <Text style={styles.scrollTipText}>
                Scrub forward to disassemble down to the silicon processor die
              </Text>
            </View>
          )}
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  sheetContainer: {
    backgroundColor: Colors.surface,
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
    paddingTop: 12,
    paddingBottom: 24,
    paddingHorizontal: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.15,
    shadowRadius: 10,
    elevation: 10,
  },
  dragHandleBar: {
    width: 44,
    height: 5,
    borderRadius: 3,
    backgroundColor: '#E0E0E0',
    alignSelf: 'center',
    marginBottom: 12,
  },
  contentContainer: {
    gap: 8,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  categoryBadge: {
    backgroundColor: Colors.studioOrange,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  categoryBadgeText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 10,
  },
  milestoneBadge: {
    backgroundColor: Colors.badgeDark,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  milestoneBadgeText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 10,
  },
  tagText: {
    ...Typography.bodySmall,
    color: Colors.inkMuted,
    fontWeight: '600',
  },
  chapterCounter: {
    ...Typography.bodySmall,
    color: Colors.inkMuted,
    fontWeight: '700',
    fontSize: 11,
  },
  titleText: {
    ...Typography.headline,
    color: Colors.ink,
    marginTop: 2,
  },
  descriptionText: {
    ...Typography.body,
    color: Colors.inkMuted,
    marginTop: 2,
  },
  closeButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#F2F2F7',
    justifyContent: 'center',
    alignItems: 'center',
  },
  specsIconButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 14,
    backgroundColor: '#F2F2F7',
  },
  specsIconText: {
    ...Typography.tag,
    color: Colors.ink,
    fontSize: 10,
  },
  specGrid: {
    backgroundColor: '#F8F9FA',
    borderRadius: 16,
    padding: 12,
    gap: 8,
    marginTop: 4,
  },
  specItem: {
    borderBottomWidth: 1,
    borderBottomColor: '#EDEDED',
    paddingBottom: 6,
  },
  specLabel: {
    ...Typography.tag,
    color: Colors.inkMuted,
    fontSize: 9,
    marginBottom: 2,
  },
  specValue: {
    ...Typography.bodySmall,
    color: Colors.ink,
    fontWeight: '600',
  },
  actionRow: {
    marginTop: 6,
  },
  deepDiveButton: {
    backgroundColor: Colors.badgeDark,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
    borderRadius: 14,
  },
  deepDiveButtonText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 12,
  },
  diveInCta: {
    backgroundColor: Colors.studioOrange,
    borderRadius: 20,
    paddingVertical: 14,
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 8,
    shadowColor: Colors.studioOrange,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.35,
    shadowRadius: 8,
    elevation: 6,
  },
  diveInTextContainer: {
    flex: 1,
  },
  diveInTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontWeight: '800',
    fontSize: 15,
  },
  diveInSubtitle: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: 11,
    marginTop: 2,
  },
  diveInIconCircle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255, 255, 255, 0.22)',
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 12,
  },
  scrollTipRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingTop: 8,
  },
  scrollTipText: {
    ...Typography.bodySmall,
    color: Colors.inkMuted,
    fontSize: 12,
    fontStyle: 'italic',
  },
});
