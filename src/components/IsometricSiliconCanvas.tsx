import React from 'react';
import { View, StyleSheet, Text } from 'react-native';
import Svg, { Rect, Polygon, Line, Circle, Text as SvgText } from 'react-native-svg';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';

interface IsometricSiliconCanvasProps {
  activeTierIndex: number;
}

export const IsometricSiliconCanvas: React.FC<IsometricSiliconCanvasProps> = ({
  activeTierIndex,
}) => {
  return (
    <View style={styles.container}>
      <View style={styles.headerRow}>
        <View style={styles.badge}>
          <Text style={styles.badgeText}>DIE FLOORPLAN • 4nm N4P</Text>
        </View>
        <Text style={styles.metricText}>16.2 Billion Transistors</Text>
      </View>

      <Svg viewBox="0 0 360 210" style={styles.svg}>
        {/* Isometric Wafer Base (Diamond projection) */}
        <Polygon
          points="180,25 330,85 180,145 30,85"
          fill="#1A1C28"
          stroke={Colors.steelBlue}
          strokeWidth="1.5"
        />

        {/* Wafer Thickness Edge */}
        <Polygon points="30,85 180,145 180,165 30,105" fill="#12131C" stroke="#2D3045" />
        <Polygon points="180,145 330,85 330,105 180,165" fill="#0C0D14" stroke="#2D3045" />

        {/* Prime Cortex-X4 Cluster (Orange Highlight) */}
        <Polygon
          points="180,45 235,67 195,83 140,61"
          fill={activeTierIndex === 0 ? 'rgba(221, 86, 34, 0.45)' : 'rgba(221, 86, 34, 0.22)'}
          stroke={Colors.studioOrange}
          strokeWidth="1.5"
        />
        <SvgText x="175" y="66" fill="#FFF" fontSize="9" fontWeight="bold" textAnchor="middle">
          CORTEX-X4
        </SvgText>

        {/* Performance A720 Cluster (5 Cores) */}
        <Polygon
          points="238,68 310,97 270,113 198,84"
          fill={activeTierIndex === 0 ? 'rgba(78, 127, 158, 0.45)' : 'rgba(78, 127, 158, 0.22)'}
          stroke={Colors.steelBlue}
          strokeWidth="1.2"
        />
        <SvgText x="254" y="93" fill="#DDD" fontSize="8" fontWeight="600" textAnchor="middle">
          5x A720 PERF
        </SvgText>

        {/* Efficiency A520 Cluster (2 Cores) */}
        <Polygon
          points="80,95 138,72 178,88 120,111"
          fill="rgba(52, 199, 89, 0.22)"
          stroke="#34C759"
          strokeWidth="1.2"
        />
        <SvgText x="130" y="94" fill="#DDD" fontSize="8" fontWeight="600" textAnchor="middle">
          2x A520 EFF
        </SvgText>

        {/* L3 System Cache & Interconnect Ring */}
        <Polygon
          points="142,88 218,88 180,124 104,124"
          fill="rgba(232, 160, 32, 0.22)"
          stroke={Colors.mustard}
          strokeWidth="1.2"
        />
        <SvgText x="160" y="108" fill={Colors.mustard} fontSize="8" fontWeight="bold" textAnchor="middle">
          8MB L3 CACHE
        </SvgText>

        {/* Interconnect Bus Flow Lines */}
        <Line x1="180" y1="124" x2="180" y2="155" stroke={Colors.studioOrange} strokeWidth="2" strokeDasharray="3, 3" />
        <Circle cx="180" cy="155" r="3" fill={Colors.studioOrange} />

        {/* Silicon Layers Floating Projection in 3D */}
        {activeTierIndex > 0 && (
          <>
            <Polygon
              points="180,10 320,65 180,120 40,65"
              fill="none"
              stroke="rgba(255, 255, 255, 0.3)"
              strokeWidth="1"
              strokeDasharray="4, 4"
            />
            <Line x1="180" y1="25" x2="180" y2="10" stroke="rgba(255, 255, 255, 0.5)" strokeWidth="1" />
            <Line x1="330" y1="85" x2="320" y2="65" stroke="rgba(255, 255, 255, 0.5)" strokeWidth="1" />
            <Line x1="30" y1="85" x2="40" y2="65" stroke="rgba(255, 255, 255, 0.5)" strokeWidth="1" />
          </>
        )}
      </Svg>

      <View style={styles.footerLegend}>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: Colors.studioOrange }]} />
          <Text style={styles.legendLabel}>Prime (3.3GHz)</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: Colors.steelBlue }]} />
          <Text style={styles.legendLabel}>Perf (3.15GHz)</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: '#34C759' }]} />
          <Text style={styles.legendLabel}>Eff (2.27GHz)</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: Colors.mustard }]} />
          <Text style={styles.legendLabel}>L3 Coherency</Text>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#12121A',
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    padding: 14,
    marginVertical: 8,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  badge: {
    backgroundColor: 'rgba(221, 86, 34, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: 'rgba(221, 86, 34, 0.4)',
  },
  badgeText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 9,
  },
  metricText: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
    fontWeight: '600',
  },
  svg: {
    width: '100%',
    height: 180,
  },
  footerLegend: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.08)',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  legendLabel: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 10,
  },
});
