import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Colors } from '../src/theme/colors';
import { Typography } from '../src/theme/typography';
import { OS_TIERS } from '../src/data/kernelStackData';
import { IsometricSiliconCanvas } from '../src/components/IsometricSiliconCanvas';
import { CpuSchedulerVisualizer } from '../src/components/CpuSchedulerVisualizer';
import { MemorySpaceVisualizer } from '../src/components/MemorySpaceVisualizer';
import { BinderTracer } from '../src/components/BinderTracer';
import { SyscallSimulatorView } from '../src/components/SyscallSimulatorView';
import { Ionicons } from '@expo/vector-icons';

type ActiveSimTab = 'CPU' | 'MEMORY' | 'BINDER' | 'SYSCALL';

export default function KernelStackExplorerScreen() {
  const router = useRouter();
  const [selectedTierIndex, setSelectedTierIndex] = useState(1); // Default to Linux Kernel
  const [activeSimTab, setActiveSimTab] = useState<ActiveSimTab>('SYSCALL');

  const activeTier = OS_TIERS[selectedTierIndex];

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Navigation Bar */}
        <View style={styles.topBar}>
          <TouchableOpacity
            style={styles.circleButton}
            onPress={() => router.replace('/teardown')}
            accessibilityLabel="Return to Teardown"
          >
            <Ionicons name="hardware-chip" size={18} color={Colors.textWhite} />
          </TouchableOpacity>

          <View style={styles.topBarTitleCenter}>
            <Text style={styles.screenHeaderTitle}>KERNEL STACK EXPLORER</Text>
            <Text style={styles.screenHeaderSubtitle}>SILICON TO APPLICATION RUNTIME</Text>
          </View>

          <TouchableOpacity
            style={styles.circleButton}
            onPress={() => router.push('/specs')}
            accessibilityLabel="Open specs"
          >
            <Ionicons name="document-text-outline" size={18} color={Colors.textWhite} />
          </TouchableOpacity>
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          {/* Top 3D Isometric Die & Layer Projection Canvas */}
          <IsometricSiliconCanvas activeTierIndex={selectedTierIndex} />

          {/* Vertical OS Tier Hierarchy Selector */}
          <View style={styles.tierSelectorSection}>
            <Text style={styles.sectionHeaderTitle}>SELECT OPERATING SYSTEM TIER</Text>

            <View style={styles.tierPillsColumn}>
              {OS_TIERS.map((tier) => {
                const isSelected = selectedTierIndex === tier.index;

                return (
                  <TouchableOpacity
                    key={tier.id}
                    style={[styles.tierPill, isSelected && styles.tierPillActive]}
                    onPress={() => setSelectedTierIndex(tier.index)}
                    activeOpacity={0.8}
                  >
                    <View style={styles.tierNumberCircle}>
                      <Text
                        style={[
                          styles.tierNumberText,
                          isSelected && { color: Colors.textWhite },
                        ]}
                      >
                        {tier.index}
                      </Text>
                    </View>

                    <View style={styles.tierTextContainer}>
                      <View style={styles.tierBadgeRow}>
                        <Text style={styles.tierBadgeText}>{tier.badge}</Text>
                        <Text style={styles.tierPrivilegeText}>{tier.privilegeRing}</Text>
                      </View>
                      <Text style={styles.tierTitleText}>{tier.title}</Text>
                      <Text style={styles.tierSubtitleText} numberOfLines={1}>
                        {tier.subtitle}
                      </Text>
                    </View>

                    <Ionicons
                      name={isSelected ? 'chevron-down' : 'chevron-forward'}
                      size={18}
                      color={isSelected ? Colors.studioOrange : Colors.inkLight}
                    />
                  </TouchableOpacity>
                );
              })}
            </View>
          </View>

          {/* Active Tier Deep-Dive Architecture Card */}
          <View style={styles.activeTierCard}>
            <View style={styles.activeTierHeader}>
              <View>
                <View style={styles.privilegeTag}>
                  <Ionicons name="shield-outline" size={12} color={Colors.studioOrange} />
                  <Text style={styles.privilegeTagText}>{activeTier.securityDomain}</Text>
                </View>
                <Text style={styles.activeTierTitle}>{activeTier.title}</Text>
              </View>
            </View>

            {/* Subsystems Breakdown */}
            <View style={styles.subsystemsList}>
              {activeTier.subsystems.map((sub, idx) => (
                <View key={idx} style={styles.subsystemItem}>
                  <View style={styles.subsystemTop}>
                    <View style={styles.subTagBadge}>
                      <Text style={styles.subTagText}>{sub.tag}</Text>
                    </View>
                    <Text style={styles.metricText}>
                      {sub.metricLabel}:{' '}
                      <Text style={styles.metricValText}>{sub.metricValue}</Text>
                    </Text>
                  </View>
                  <Text style={styles.subTitle}>{sub.title}</Text>
                  <Text style={styles.subDesc}>{sub.description}</Text>
                </View>
              ))}
            </View>

            {/* Key APIs & Syscalls */}
            <View style={styles.apisContainer}>
              <Text style={styles.apisTitle}>CRITICAL KERNEL APIS & DIRECTIVES:</Text>
              <View style={styles.apiTagsWrap}>
                {activeTier.keyApis.map((api, i) => (
                  <View key={i} style={styles.apiTag}>
                    <Text style={styles.apiTagText}>{api}</Text>
                  </View>
                ))}
              </View>
            </View>
          </View>

          {/* Interactive Simulation Hub */}
          <View style={styles.simHubSection}>
            <View style={styles.simHubHeader}>
              <Ionicons name="play-circle" size={20} color={Colors.studioOrange} />
              <Text style={styles.simHubTitle}>LIVE INTERACTIVE SIMULATORS</Text>
            </View>

            {/* Simulator Tabs */}
            <View style={styles.simTabsRow}>
              <TouchableOpacity
                style={[styles.simTab, activeSimTab === 'SYSCALL' && styles.simTabActive]}
                onPress={() => setActiveSimTab('SYSCALL')}
              >
                <Ionicons
                  name="terminal-outline"
                  size={14}
                  color={activeSimTab === 'SYSCALL' ? Colors.textWhite : Colors.inkLight}
                />
                <Text style={[styles.simTabText, activeSimTab === 'SYSCALL' && styles.simTabTextActive]}>
                  SYSCALL
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.simTab, activeSimTab === 'CPU' && styles.simTabActive]}
                onPress={() => setActiveSimTab('CPU')}
              >
                <Ionicons
                  name="speedometer-outline"
                  size={14}
                  color={activeSimTab === 'CPU' ? Colors.textWhite : Colors.inkLight}
                />
                <Text style={[styles.simTabText, activeSimTab === 'CPU' && styles.simTabTextActive]}>
                  CPU EAS
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.simTab, activeSimTab === 'MEMORY' && styles.simTabActive]}
                onPress={() => setActiveSimTab('MEMORY')}
              >
                <Ionicons
                  name="layers-outline"
                  size={14}
                  color={activeSimTab === 'MEMORY' ? Colors.textWhite : Colors.inkLight}
                />
                <Text style={[styles.simTabText, activeSimTab === 'MEMORY' && styles.simTabTextActive]}>
                  V-MEMORY
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.simTab, activeSimTab === 'BINDER' && styles.simTabActive]}
                onPress={() => setActiveSimTab('BINDER')}
              >
                <Ionicons
                  name="git-compare-outline"
                  size={14}
                  color={activeSimTab === 'BINDER' ? Colors.textWhite : Colors.inkLight}
                />
                <Text style={[styles.simTabText, activeSimTab === 'BINDER' && styles.simTabTextActive]}>
                  BINDER
                </Text>
              </TouchableOpacity>
            </View>

            {/* Active Simulation View */}
            {activeSimTab === 'SYSCALL' && <SyscallSimulatorView />}
            {activeSimTab === 'CPU' && <CpuSchedulerVisualizer />}
            {activeSimTab === 'MEMORY' && <MemorySpaceVisualizer />}
            {activeSimTab === 'BINDER' && <BinderTracer />}
          </View>
        </ScrollView>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#0A0A10',
  },
  container: {
    flex: 1,
    backgroundColor: '#0A0A10',
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.08)',
  },
  circleButton: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
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
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 8,
    marginTop: 1,
  },
  scrollContent: {
    padding: 16,
    gap: 14,
    paddingBottom: 40,
  },
  tierSelectorSection: {
    gap: 8,
  },
  sectionHeaderTitle: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 10,
    marginLeft: 4,
  },
  tierPillsColumn: {
    gap: 8,
  },
  tierPill: {
    backgroundColor: '#161622',
    borderRadius: 14,
    padding: 12,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.06)',
    gap: 12,
  },
  tierPillActive: {
    borderColor: Colors.studioOrange,
    backgroundColor: 'rgba(221, 86, 34, 0.12)',
  },
  tierNumberCircle: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  tierNumberText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 11,
    fontWeight: '800',
  },
  tierTextContainer: {
    flex: 1,
  },
  tierBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 2,
  },
  tierBadgeText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 8,
  },
  tierPrivilegeText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 8,
  },
  tierTitleText: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 13,
  },
  tierSubtitleText: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 10,
    marginTop: 1,
  },
  activeTierCard: {
    backgroundColor: '#14141E',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 12,
  },
  activeTierHeader: {
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.06)',
    paddingBottom: 8,
  },
  privilegeTag: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginBottom: 4,
  },
  privilegeTagText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 9,
  },
  activeTierTitle: {
    ...Typography.headline,
    color: Colors.textWhite,
    fontSize: 20,
  },
  subsystemsList: {
    gap: 10,
  },
  subsystemItem: {
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  subsystemTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  subTagBadge: {
    backgroundColor: 'rgba(78, 127, 158, 0.2)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  subTagText: {
    ...Typography.tag,
    color: Colors.steelBlue,
    fontSize: 8,
  },
  metricText: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 10,
  },
  metricValText: {
    color: Colors.textWhite,
    fontWeight: '700',
  },
  subTitle: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 12,
    marginBottom: 2,
  },
  subDesc: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
    lineHeight: 15,
  },
  apisContainer: {
    marginTop: 4,
  },
  apisTitle: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
    marginBottom: 6,
  },
  apiTagsWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  apiTag: {
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
  },
  apiTagText: {
    ...Typography.code,
    color: Colors.mustard,
    fontSize: 10,
  },
  simHubSection: {
    marginTop: 8,
  },
  simHubHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 10,
  },
  simHubTitle: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 11,
    letterSpacing: 1,
  },
  simTabsRow: {
    flexDirection: 'row',
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    borderRadius: 12,
    padding: 4,
    marginBottom: 10,
    gap: 4,
  },
  simTab: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    paddingVertical: 8,
    borderRadius: 8,
  },
  simTabActive: {
    backgroundColor: Colors.studioOrange,
  },
  simTabText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
  },
  simTabTextActive: {
    color: Colors.textWhite,
    fontWeight: '800',
  },
});
