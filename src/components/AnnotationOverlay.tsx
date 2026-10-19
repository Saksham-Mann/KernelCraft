import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import Svg, { Line, Circle } from 'react-native-svg';
import { Colors } from '../theme/colors';
import { Typography, FontFamilies } from '../theme/typography';
import { TeardownAnnotation } from '../data/teardownData';
import { Ionicons } from '@expo/vector-icons';

interface AnnotationOverlayProps {
  annotations: TeardownAnnotation[];
  progress: number;
  containerWidth: number;
  containerHeight: number;
  onSelectAnnotation: (annotation: TeardownAnnotation) => void;
  selectedId?: string | null;
}

export const AnnotationOverlay: React.FC<AnnotationOverlayProps> = ({
  annotations,
  progress,
  containerWidth,
  containerHeight,
  onSelectAnnotation,
  selectedId,
}) => {
  if (containerWidth <= 0 || containerHeight <= 0) return null;

  return (
    <View
      pointerEvents="box-none"
      style={[StyleSheet.absoluteFill, styles.overlayRoot]}
    >
      {annotations.map((ann, index) => {
        // Strict boundary check with smooth ramp
        if (progress < ann.startProgress || progress > ann.endProgress) {
          return null;
        }

        const ramp = ann.id === 'screen_display' ? 0.008 : 0.035;
        let alpha = 1.0;
        if (progress < ann.startProgress + ramp) {
          const t = (progress - ann.startProgress) / ramp;
          alpha = t * t * (3 - 2 * t);
        } else if (progress > ann.endProgress - ramp && ann.endProgress < 0.99) {
          const t = (ann.endProgress - progress) / ramp;
          alpha = t * t * (3 - 2 * t);
        }

        const isSelected = selectedId === ann.id;
        const anchorScreenX = ann.anchorX * containerWidth;
        const anchorScreenY = ann.anchorY * containerHeight;

        // Estimated card width per component for boundary constraints & line anchoring
        let estimatedCardWidth = 150;
        let badgeScreenX = 0;
        let badgeScreenY = 0;
        let leaderTargetX = 0;
        let leaderTargetY = 0;

        if (ann.id === 'camera_array') {
          // Camera & ISP (Blue): Left quadrant, placed lower below lenses
          // Pin is at left optical lenses (anchorX: 0.23, anchorY: 0.44)
          // Comfortable margin from left frame, cleanly spaced below RAM
          estimatedCardWidth = 150;
          badgeScreenX = Math.max(16, anchorScreenX - 54);
          badgeScreenY = anchorScreenY + 30;
          leaderTargetX = badgeScreenX + 38;
          leaderTargetY = badgeScreenY + 2;
        } else if (ann.id === 'ram_memory') {
          // RAM (Purple): Right quadrant, anchored strictly downward along Y-axis relative to dot
          // Pin is locked at logic board coordinates (anchorX: 0.60, anchorY: 0.32)
          // Badge is shifted downward relative to dot (+24px), leaving clean vertical & horizontal breathing room
          estimatedCardWidth = 150;
          badgeScreenX = Math.min(containerWidth - 164, anchorScreenX - 18);
          badgeScreenY = anchorScreenY + 24;
          leaderTargetX = anchorScreenX;
          leaderTargetY = badgeScreenY + 2;
        } else if (ann.id === 'cpu_brain') {
          // SoC & Logic (Orange): Top-Center quadrant
          // Pin is on SoC wafer (anchorX: 0.44, anchorY: 0.28)
          // Badge is placed above chip at the top
          estimatedCardWidth = 148;
          badgeScreenX = anchorScreenX - estimatedCardWidth / 2;
          badgeScreenY = anchorScreenY - 56;
          leaderTargetX = badgeScreenX + estimatedCardWidth / 2;
          leaderTargetY = badgeScreenY + 36;
        } else if (ann.id === 'battery_pack') {
          // Battery (Green): Bottom-Center quadrant
          // Pin is on battery pack (anchorX: 0.48, anchorY: 0.68)
          // Badge is placed below battery pack at bottom
          estimatedCardWidth = 168;
          badgeScreenX = anchorScreenX - estimatedCardWidth / 2;
          badgeScreenY = anchorScreenY + 34;
          leaderTargetX = badgeScreenX + estimatedCardWidth / 2;
          leaderTargetY = badgeScreenY + 2;
        } else {
          // AMOLED Screen (Red): Front Display quadrant (only visible when flipped to front)
          // Pin is on front display center (anchorX: 0.50, anchorY: 0.50)
          estimatedCardWidth = 138;
          badgeScreenX = anchorScreenX - estimatedCardWidth / 2;
          badgeScreenY = anchorScreenY - 58;
          leaderTargetX = badgeScreenX + estimatedCardWidth / 2;
          leaderTargetY = badgeScreenY + 38;
        }

        // Strict container boundary constraints so no card ever overflows horizontally or vertically
        const clampedCardWidth = Math.min(estimatedCardWidth, containerWidth - 16);
        badgeScreenX = Math.max(12, Math.min(containerWidth - clampedCardWidth - 12, badgeScreenX));
        badgeScreenY = Math.max(10, Math.min(containerHeight - 48, badgeScreenY));

        const uniqueZIndex = 200 + index * 10;

        return (
          <View
            key={ann.id}
            pointerEvents="box-none"
            style={[
              StyleSheet.absoluteFill,
              {
                opacity: Math.max(0, Math.min(1, alpha)),
                zIndex: uniqueZIndex,
              },
            ]}
          >
            {/* SVG Connecting Leader Line */}
            <Svg
              pointerEvents="none"
              style={[StyleSheet.absoluteFill, { pointerEvents: 'none' }]}
            >
              <Line
                x1={anchorScreenX}
                y1={anchorScreenY}
                x2={leaderTargetX}
                y2={leaderTargetY}
                stroke={ann.accentColor || Colors.studioOrange}
                strokeWidth={isSelected ? 2.5 : 1.5}
                strokeDasharray={isSelected ? undefined : '4, 4'}
              />
              <Circle
                cx={anchorScreenX}
                cy={anchorScreenY}
                r={isSelected ? 6 : 4}
                fill={ann.accentColor || Colors.studioOrange}
              />
            </Svg>

            {/* Hotspot Pulse Dot at Physical Anchor (Min 44x44 generous touch area) */}
            <TouchableOpacity
              activeOpacity={0.7}
              hitSlop={{ top: 16, bottom: 16, left: 16, right: 16 }}
              style={[
                styles.hotspotAnchor,
                {
                  left: anchorScreenX - 22,
                  top: anchorScreenY - 22,
                  borderColor: ann.accentColor || Colors.studioOrange,
                  zIndex: uniqueZIndex + 4,
                },
              ]}
              onPress={() => onSelectAnnotation(ann)}
              accessibilityRole="button"
              accessibilityLabel={`Select component ${ann.name}`}
            >
              <View
                style={[
                  styles.hotspotCenter,
                  { backgroundColor: ann.accentColor || Colors.studioOrange },
                ]}
              />
            </TouchableOpacity>

            {/* Floating Hardware Pill Tag (Fully clickable, high zIndex) */}
            <TouchableOpacity
              activeOpacity={0.8}
              hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
              style={[
                styles.badgeCallout,
                {
                  left: badgeScreenX,
                  top: badgeScreenY,
                  borderColor: isSelected ? ann.accentColor : 'rgba(255, 255, 255, 0.3)',
                  backgroundColor: isSelected
                    ? 'rgba(24, 24, 38, 0.98)'
                    : 'rgba(14, 14, 22, 0.94)',
                  zIndex: uniqueZIndex + 6,
                },
              ]}
              onPress={() => onSelectAnnotation(ann)}
              accessibilityRole="button"
              accessibilityLabel={`View specifications for ${ann.name}`}
            >
              <View style={[styles.badgeIconWrap, { backgroundColor: ann.accentColor }]}>
                <Ionicons name={ann.icon as any} size={13} color="#FFFFFF" />
              </View>
              <View style={styles.badgeTextCol}>
                <Text style={styles.badgeName} numberOfLines={1}>
                  {ann.name}
                </Text>
                <Text style={[styles.badgeTag, { color: ann.accentColor }]} numberOfLines={1}>
                  {ann.tag}
                </Text>
              </View>
            </TouchableOpacity>
          </View>
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
  overlayRoot: {
    zIndex: 100,
    pointerEvents: 'box-none',
  },
  hotspotAnchor: {
    position: 'absolute',
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.65)',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.6,
    shadowRadius: 5,
    elevation: 8,
    pointerEvents: 'auto',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer', pointerEvents: 'auto' } as any) : {}),
  },
  hotspotCenter: {
    width: 14,
    height: 14,
    borderRadius: 7,
  },
  badgeCallout: {
    position: 'absolute',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    minHeight: 36,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    borderWidth: 1.5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.55,
    shadowRadius: 6,
    elevation: 10,
    pointerEvents: 'auto',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer', pointerEvents: 'auto' } as any) : {}),
  },
  badgeIconWrap: {
    width: 22,
    height: 22,
    borderRadius: 11,
    justifyContent: 'center',
    alignItems: 'center',
  },
  badgeTextCol: {
    flexShrink: 1,
    gap: 1,
  },
  badgeName: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontWeight: '600',
    fontSize: 10,
    letterSpacing: 0,
  },
  badgeTag: {
    fontFamily: FontFamilies.sans,
    fontSize: 8,
    fontWeight: '600',
    letterSpacing: 0.1,
  },
});
