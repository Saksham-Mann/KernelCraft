import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import Svg, { Line, Circle } from 'react-native-svg';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
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
    <View style={[StyleSheet.absoluteFill, { pointerEvents: 'box-none', zIndex: 100 }]}>
      {annotations.map((ann, index) => {
        // Smoothstep visibility calculation
        if (progress < ann.startProgress || progress > ann.endProgress) {
          return null;
        }

        const ramp = 0.04;
        let alpha = 1.0;
        if (progress < ann.startProgress + ramp) {
          const t = (progress - ann.startProgress) / ramp;
          alpha = t * t * (3 - 2 * t);
        } else if (progress > ann.endProgress - ramp) {
          const t = (ann.endProgress - progress) / ramp;
          alpha = t * t * (3 - 2 * t);
        }

        const isSelected = selectedId === ann.id;
        const anchorScreenX = ann.anchorX * containerWidth;
        const anchorScreenY = ann.anchorY * containerHeight;

        // Smart, distinct placement per component to eliminate hitbox collisions
        let badgeScreenX = 0;
        let badgeScreenY = 0;

        if (ann.id === 'camera_array') {
          // Camera: top-left
          badgeScreenX = Math.max(12, anchorScreenX - 100);
          badgeScreenY = Math.max(30, anchorScreenY - 48);
        } else if (ann.id === 'cpu_brain') {
          // CPU & Logic Board: top-center
          badgeScreenX = Math.max(12, Math.min(containerWidth - 160, anchorScreenX - 70));
          badgeScreenY = Math.max(20, anchorScreenY - 54);
        } else if (ann.id === 'ram_memory') {
          // RAM: right side
          badgeScreenX = Math.min(containerWidth - 150, anchorScreenX + 28);
          badgeScreenY = Math.max(50, anchorScreenY - 20);
        } else if (ann.id === 'battery_pack') {
          // Battery: bottom center-left
          badgeScreenX = Math.max(14, Math.min(containerWidth - 160, anchorScreenX - 80));
          badgeScreenY = Math.min(containerHeight - 75, anchorScreenY + 36);
        } else {
          // Screen / other: center
          badgeScreenX = Math.max(12, Math.min(containerWidth - 160, anchorScreenX - 60));
          badgeScreenY = Math.min(containerHeight - 80, anchorScreenY + 30);
        }

        const leaderTargetX = badgeScreenX + 40;
        const leaderTargetY = badgeScreenY + 22;
        const uniqueZIndex = 110 + index * 10;

        return (
          <View
            key={ann.id}
            style={[
              StyleSheet.absoluteFill,
              {
                opacity: Math.max(0, Math.min(1, alpha)),
                pointerEvents: 'box-none',
                zIndex: uniqueZIndex,
              },
            ]}
          >
            {/* SVG Connecting Line */}
            <Svg style={StyleSheet.absoluteFill} pointerEvents="none">
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

            {/* Hotspot Pulse Dot at Anchor (Min 44x44 touch area) */}
            <TouchableOpacity
              activeOpacity={0.7}
              style={[
                styles.hotspotAnchor,
                {
                  left: anchorScreenX - 22,
                  top: anchorScreenY - 22,
                  borderColor: ann.accentColor || Colors.studioOrange,
                  zIndex: uniqueZIndex + 2,
                },
              ]}
              onPress={() => onSelectAnnotation(ann)}
              accessibilityLabel={`Select ${ann.name}`}
            >
              <View
                style={[
                  styles.hotspotCenter,
                  { backgroundColor: ann.accentColor || Colors.studioOrange },
                ]}
              />
            </TouchableOpacity>

            {/* Floating Hardware Pill Tag (Min 44x44 touch area, high zIndex, pointerEvents auto) */}
            <TouchableOpacity
              activeOpacity={0.8}
              style={[
                styles.badgeCallout,
                {
                  left: badgeScreenX,
                  top: badgeScreenY,
                  borderColor: isSelected ? ann.accentColor : 'rgba(255, 255, 255, 0.3)',
                  backgroundColor: isSelected ? 'rgba(24, 24, 38, 0.98)' : 'rgba(14, 14, 22, 0.92)',
                  zIndex: uniqueZIndex + 5,
                },
              ]}
              onPress={() => onSelectAnnotation(ann)}
              accessibilityLabel={`View specifications for ${ann.name}`}
            >
              <View style={[styles.badgeIconWrap, { backgroundColor: ann.accentColor }]}>
                <Ionicons name={ann.icon as any} size={15} color="#FFFFFF" />
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
  hotspotAnchor: {
    position: 'absolute',
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.6,
    shadowRadius: 5,
    elevation: 8,
    pointerEvents: 'auto',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
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
    gap: 8,
    minHeight: 44,
    minWidth: 120,
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 16,
    borderWidth: 1.5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.5,
    shadowRadius: 8,
    elevation: 10,
    pointerEvents: 'auto',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  badgeIconWrap: {
    width: 26,
    height: 26,
    borderRadius: 13,
    justifyContent: 'center',
    alignItems: 'center',
  },
  badgeTextCol: {
    flexShrink: 1,
    gap: 1,
  },
  badgeName: {
    ...Typography.bodySmall,
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 11,
    letterSpacing: 0.2,
  },
  badgeTag: {
    ...Typography.tag,
    fontSize: 9,
    fontWeight: '700',
  },
});
