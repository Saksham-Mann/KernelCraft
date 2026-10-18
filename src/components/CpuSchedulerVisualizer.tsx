import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { CpuGovernor, generateCoreData } from '../data/kernelStackData';
import { Ionicons } from '@expo/vector-icons';

export const CpuSchedulerVisualizer: React.FC = () => {
  const [governor, setGovernor] = useState<CpuGovernor>('schedutil');
  const [isThrottled, setIsThrottled] = useState(false);

  const cores = generateCoreData(governor, isThrottled);

  return (
    <View style={styles.card}>
      {/* Title and Controls */}
      <View style={styles.headerRow}>
        <View>
          <Text style={styles.cardTitle}>EAS CPU Governor & Cluster Cores</Text>
          <Text style={styles.cardSubtitle}>
            Linux CFS runqueues with Per-Entity Load Tracking (PELT)
          </Text>
        </View>

        <TouchableOpacity
          style={[styles.throttleBadge, isThrottled && styles.throttleBadgeActive]}
          onPress={() => setIsThrottled(!isThrottled)}
        >
          <Ionicons
            name={isThrottled ? 'flame' : 'thermometer-outline'}
            size={14}
            color={isThrottled ? Colors.textWhite : Colors.inkLight}
          />
          <Text style={[styles.throttleText, isThrottled && { color: Colors.textWhite }]}>
            {isThrottled ? '49°C THROTTLED' : '38.5°C NOMINAL'}
          </Text>
        </TouchableOpacity>
      </View>

      {/* Governor Selector Buttons */}
      <View style={styles.governorRow}>
        {(['schedutil', 'performance', 'powersave'] as CpuGovernor[]).map((mode) => (
          <TouchableOpacity
            key={mode}
            style={[styles.governorTab, governor === mode && styles.governorTabActive]}
            onPress={() => setGovernor(mode)}
          >
            <Text
              style={[styles.governorTabText, governor === mode && styles.governorTabTextActive]}
            >
              {mode.toUpperCase()}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Core Telemetry List */}
      <View style={styles.coreList}>
        {cores.map((core) => {
          const clusterColor =
            core.cluster === 'PRIME'
              ? Colors.studioOrange
              : core.cluster === 'PERF'
              ? Colors.steelBlue
              : '#34C759';

          const pct = Math.round((core.currentFreqGhz / core.maxFreqGhz) * 100);

          return (
            <View key={core.index} style={styles.coreItem}>
              <View style={styles.coreHeader}>
                <View style={styles.coreClusterBadge}>
                  <View style={[styles.clusterDot, { backgroundColor: clusterColor }]} />
                  <Text style={styles.coreNameText}>{core.name}</Text>
                  <Text style={[styles.clusterLabel, { color: clusterColor }]}>
                    [{core.cluster}]
                  </Text>
                </View>

                <Text style={styles.freqText}>
                  {core.currentFreqGhz.toFixed(2)} GHz{' '}
                  <Text style={styles.maxFreqText}>/ {core.maxFreqGhz.toFixed(2)}</Text>
                </Text>
              </View>

              {/* Frequency Bar */}
              <View style={styles.barBackground}>
                <View
                  style={[
                    styles.barFill,
                    {
                      width: `${pct}%`,
                      backgroundColor: clusterColor,
                    },
                  ]}
                />
              </View>

              <View style={styles.coreFooter}>
                <Text style={styles.taskText}>
                  Task: <Text style={styles.taskValue}>{core.task}</Text>
                </Text>
                <Text style={styles.loadText}>Load: {core.load}%</Text>
              </View>
            </View>
          );
        })}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#161622',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.09)',
    marginVertical: 6,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  cardTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 16,
  },
  cardSubtitle: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
    marginTop: 2,
  },
  throttleBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  throttleBadgeActive: {
    backgroundColor: '#D32F2F',
  },
  throttleText: {
    ...Typography.tag,
    fontSize: 9,
    color: Colors.inkLight,
  },
  governorRow: {
    flexDirection: 'row',
    backgroundColor: 'rgba(0, 0, 0, 0.35)',
    borderRadius: 10,
    padding: 3,
    marginBottom: 14,
  },
  governorTab: {
    flex: 1,
    paddingVertical: 6,
    alignItems: 'center',
    borderRadius: 8,
  },
  governorTabActive: {
    backgroundColor: Colors.studioOrange,
  },
  governorTabText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 10,
  },
  governorTabTextActive: {
    color: Colors.textWhite,
    fontWeight: '800',
  },
  coreList: {
    gap: 10,
  },
  coreItem: {
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: 10,
    padding: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  coreHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  coreClusterBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  clusterDot: {
    width: 7,
    height: 7,
    borderRadius: 4,
  },
  coreNameText: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 12,
  },
  clusterLabel: {
    ...Typography.tag,
    fontSize: 9,
  },
  freqText: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 12,
  },
  maxFreqText: {
    color: Colors.inkLight,
    fontWeight: '400',
    fontSize: 10,
  },
  barBackground: {
    height: 6,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    borderRadius: 3,
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    borderRadius: 3,
  },
  coreFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 6,
  },
  taskText: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 10,
  },
  taskValue: {
    color: Colors.textWhite,
    fontWeight: '600',
  },
  loadText: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 10,
    fontWeight: '700',
  },
});
