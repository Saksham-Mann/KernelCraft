import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import Svg, { Line, Circle } from 'react-native-svg';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { TeardownAnnotation } from '../data/teardownData';

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
    <View style={[StyleSheet.absoluteFill, { pointerEvents: 'box-none' }]}>
      {annotations.map((ann) => {
        // Hermite smoothstep visibility calculation
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

        // Leader line end
        const badgeScreenX = anchorScreenX + ann.leaderLineDx * containerWidth;
        const badgeScreenY = anchorScreenY + ann.leaderLineDy * containerHeight;

        return (
          <View
            key={ann.id}
            style={[
              StyleSheet.absoluteFill,
              { opacity: Math.max(0, Math.min(1, alpha)), pointerEvents: 'box-none' },
            ]}
          >
            {/* SVG Leader Line connecting pin to badge */}
            <Svg style={StyleSheet.absoluteFill} pointerEvents="none">
              <Line
                x1={anchorScreenX}
                y1={anchorScreenY}
                x2={badgeScreenX}
                y2={badgeScreenY}
                stroke={isSelected ? Colors.studioOrange : 'rgba(255, 255, 255, 0.8)'}
                strokeWidth={isSelected ? 2 : 1.5}
                strokeDasharray={isSelected ? undefined : '3, 3'}
              />
              <Circle
                cx={anchorScreenX}
                cy={anchorScreenY}
                r={isSelected ? 6 : 4}
                fill={isSelected ? Colors.studioOrange : Colors.surface}
              />
            </Svg>

            {/* Hotspot Pulse Dot at Anchor */}
            <TouchableOpacity
              activeOpacity={0.8}
              style={[
                styles.hotspotAnchor,
                {
                  left: anchorScreenX - 16,
                  top: anchorScreenY - 16,
                  borderColor: isSelected ? Colors.studioOrange : Colors.surface,
                },
              ]}
              onPress={() => onSelectAnnotation(ann)}
            >
              <View
                style={[
                  styles.hotspotCenter,
                  { backgroundColor: isSelected ? Colors.studioOrange : Colors.surface },
                ]}
              />
            </TouchableOpacity>

            {/* Floating Annotation Badge */}
            <TouchableOpacity
              activeOpacity={0.8}
              style={[
                styles.badgeCallout,
                {
                  left: Math.max(12, Math.min(containerWidth - 160, badgeScreenX - 10)),
                  top: Math.max(60, Math.min(containerHeight - 120, badgeScreenY - 18)),
                  borderColor: isSelected ? Colors.studioOrange : 'rgba(255, 255, 255, 0.25)',
                },
              ]}
              onPress={() => onSelectAnnotation(ann)}
            >
              <View style={styles.badgeCategory}>
                <Text style={styles.badgeCategoryText}>{ann.category}</Text>
              </View>
              <Text style={styles.badgeTitle} numberOfLines={1}>
                {ann.name}
              </Text>
              <Text style={styles.badgeTag} numberOfLines={1}>
                {ann.tag}
              </Text>
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
    width: 32,
    height: 32,
    borderRadius: 16,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.5,
    shadowRadius: 4,
    elevation: 5,
  },
  hotspotCenter: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  badgeCallout: {
    position: 'absolute',
    backgroundColor: 'rgba(20, 20, 26, 0.94)',
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 10,
    borderWidth: 1.5,
    maxWidth: 180,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.4,
    shadowRadius: 6,
    elevation: 6,
  },
  badgeCategory: {
    alignSelf: 'flex-start',
    backgroundColor: Colors.studioOrange,
    paddingHorizontal: 5,
    paddingVertical: 1.5,
    borderRadius: 4,
    marginBottom: 2,
  },
  badgeCategoryText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 9,
  },
  badgeTitle: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 12,
  },
  badgeTag: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 10,
  },
});
