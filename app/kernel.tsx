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
import { AppExecutionSimulator } from '../src/components/AppExecutionSimulator';
import { MyDeviceInspector } from '../src/components/MyDeviceInspector';
import { CpuSchedulerVisualizer } from '../src/components/CpuSchedulerVisualizer';
import { MemorySpaceVisualizer } from '../src/components/MemorySpaceVisualizer';
import { BinderTracer } from '../src/components/BinderTracer';
import { SyscallSimulatorView } from '../src/components/SyscallSimulatorView';
import { Ionicons } from '@expo/vector-icons';

type PrimarySectionTab = 'SIMULATOR' | 'DEVICE_OS' | 'ARCH_TIERS';
type ActiveSimTab = 'SYSCALL' | 'CPU' | 'MEMORY' | 'BINDER';

export default function KernelStackExplorerScreen() {
  const router = useRouter();
  const [primaryTab, setPrimaryTab] = useState<PrimarySectionTab>('SIMULATOR');
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
            <Text style={styles.screenHeaderTitle}>OS SOFTWARE LAYER</Text>
            <Text style={styles.screenHeaderSubtitle}>FROM TOUCH TO KERNEL SYSCALL</Text>
          </View>

          <TouchableOpacity
            style={styles.circleButton}
            onPress={() => router.replace('/teardown')}
            accessibilityLabel="Replay teardown"
          >
            <Ionicons name="refresh" size={18} color={Colors.textWhite} />
          </TouchableOpacity>
        </View>

        {/* Primary Feature Tabs Toggle (Simulator | My Device OS | Arch Layers) */}
        <View style={styles.primaryTabsRow}>
          <TouchableOpacity
            style={[
              styles.primaryTabButton,
              primaryTab === 'SIMULATOR' && styles.primaryTabButtonActive,
            ]}
            onPress={() => setPrimaryTab('SIMULATOR')}
            activeOpacity={0.8}
          >
            <Ionicons
              name="play"
              size={14}
              color={primaryTab === 'SIMULATOR' ? '#FFFFFF' : Colors.inkLight}
            />
            <Text
              style={[
                styles.primaryTabText,
                primaryTab === 'SIMULATOR' && styles.primaryTabTextActive,
              ]}
            >
              App Execution
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.primaryTabButton,
              primaryTab === 'DEVICE_OS' && styles.primaryTabButtonActive,
            ]}
            onPress={() => setPrimaryTab('DEVICE_OS')}
            activeOpacity={0.8}
          >
            <Ionicons
              name="speedometer-outline"
              size={14}
              color={primaryTab === 'DEVICE_OS' ? '#FFFFFF' : Colors.inkLight}
            />
            <Text
              style={[
                styles.primaryTabText,
                primaryTab === 'DEVICE_OS' && styles.primaryTabTextActive,
              ]}
            >
              My Device OS
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.primaryTabButton,
              primaryTab === 'ARCH_TIERS' && styles.primaryTabButtonActive,
            ]}
            onPress={() => setPrimaryTab('ARCH_TIERS')}
            activeOpacity={0.8}
          >
            <Ionicons
              name="layers-outline"
              size={14}
              color={primaryTab === 'ARCH_TIERS' ? '#FFFFFF' : Colors.inkLight}
            />
            <Text
              style={[
                styles.primaryTabText,
                primaryTab === 'ARCH_TIERS' && styles.primaryTabTextActive,
              ]}
            >
              OS Layers
            </Text>
          </TouchableOpacity>
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          {/* TAB 1: Simplified 5-Step Process Execution Simulation */}
          {primaryTab === 'SIMULATOR' && (
            <View style={styles.tabSectionWrap}>
              <AppExecutionSimulator />
            </View>
          )}

          {/* TAB 2: My Device OS Inspector (Live Telemetry & CPU Scheduler Playground Analogy) */}
          {primaryTab === 'DEVICE_OS' && (
            <View style={styles.tabSectionWrap}>
              <MyDeviceInspector />
            </View>
          )}

          {/* TAB 3: OS Architecture Hierarchy & Deep Tiers */}
          {primaryTab === 'ARCH_TIERS' && (
            <View style={styles.tabSectionWrap}>
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

              {/* Technical Simulators Hub */}
              <View style={styles.simHubSection}>
                <View style={styles.simHubHeader}>
                  <Ionicons name="play-circle" size={20} color={Colors.studioOrange} />
                  <Text style={styles.simHubTitle}>TECHNICAL LAB SIMULATORS</Text>
                </View>

                <View style={styles.simTabsRow}>
                  <TouchableOpacity
                    style={[styles.simTab, activeSimTab === 'SYSCALL' && styles.simTabActive]}
                    onPress={() => setActiveSimTab('SYSCALL')}
                  >
                    <Text
                      style={[
                        styles.simTabText,
                        activeSimTab === 'SYSCALL' && styles.simTabTextActive,
                      ]}
                    >
                      SYSCALL
                    </Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[styles.simTab, activeSimTab === 'CPU' && styles.simTabActive]}
                    onPress={() => setActiveSimTab('CPU')}
                  >
                    <Text
                      style={[
                        styles.simTabText,
                        activeSimTab === 'CPU' && styles.simTabTextActive,
                      ]}
                    >
                      CPU EAS
                    </Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[styles.simTab, activeSimTab === 'MEMORY' && styles.simTabActive]}
                    onPress={() => setActiveSimTab('MEMORY')}
                  >
                    <Text
                      style={[
                        styles.simTabText,
                        activeSimTab === 'MEMORY' && styles.simTabTextActive,
                      ]}
                    >
                      V-MEMORY
                    </Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[styles.simTab, activeSimTab === 'BINDER' && styles.simTabActive]}
                    onPress={() => setActiveSimTab('BINDER')}
                  >
                    <Text
                      style={[
                        styles.simTabText,
                        activeSimTab === 'BINDER' && styles.simTabTextActive,
                      ]}
                    >
                      BINDER
                    </Text>
                  </TouchableOpacity>
                </View>

                {activeSimTab === 'SYSCALL' && <SyscallSimulatorView />}
                {activeSimTab === 'CPU' && <CpuSchedulerVisualizer />}
                {activeSimTab === 'MEMORY' && <MemorySpaceVisualizer />}
                {activeSimTab === 'BINDER' && <BinderTracer />}
              </View>
            </View>
          )}
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
    fontWeight: '800',
  },
  screenHeaderSubtitle: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 8,
    marginTop: 1,
    fontWeight: '700',
  },
  primaryTabsRow: {
    flexDirection: 'row',
    backgroundColor: '#12121D',
    padding: 6,
    marginHorizontal: 16,
    marginTop: 10,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
    gap: 6,
  },
  primaryTabButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 9,
    borderRadius: 12,
  },
  primaryTabButtonActive: {
    backgroundColor: Colors.studioOrange,
    shadowColor: Colors.studioOrange,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.4,
    shadowRadius: 6,
    elevation: 4,
  },
  primaryTabText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 11,
    fontWeight: '700',
  },
  primaryTabTextActive: {
    color: '#FFFFFF',
    fontWeight: '800',
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 40,
  },
  tabSectionWrap: {
    gap: 14,
  },
  tierSelectorSection: {
    gap: 8,
  },
  sectionHeaderTitle: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 10,
    marginLeft: 4,
    fontWeight: '700',
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
    backgroundColor: 'rgba(221, 86, 34, 0.1)',
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
    fontSize: 12,
    fontWeight: '800',
  },
  tierTextContainer: {
    flex: 1,
    gap: 2,
  },
  tierBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  tierBadgeText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 9,
    fontWeight: '800',
  },
  tierPrivilegeText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
  },
  tierTitleText: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 13,
  },
  tierSubtitleText: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
  },
  activeTierCard: {
    backgroundColor: '#161622',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 12,
  },
  activeTierHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  privilegeTag: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  privilegeTagText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 10,
    fontWeight: '700',
  },
  activeTierTitle: {
    ...Typography.headline,
    color: Colors.textWhite,
    fontSize: 16,
    marginTop: 2,
  },
  subsystemsList: {
    gap: 8,
  },
  subsystemItem: {
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: 12,
    padding: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    gap: 3,
  },
  subsystemTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  subTagBadge: {
    backgroundColor: 'rgba(221, 86, 34, 0.2)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  subTagText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 8,
    fontWeight: '700',
  },
  metricText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
  },
  metricValText: {
    color: Colors.textWhite,
    fontWeight: '700',
  },
  subTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 12,
  },
  subDesc: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
    lineHeight: 15,
  },
  apisContainer: {
    backgroundColor: 'rgba(0, 0, 0, 0.25)',
    borderRadius: 12,
    padding: 10,
    gap: 6,
  },
  apisTitle: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
    fontWeight: '700',
  },
  apiTagsWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  apiTag: {
    backgroundColor: 'rgba(255, 255, 255, 0.07)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  apiTagText: {
    ...Typography.tag,
    color: '#00E5FF',
    fontSize: 10,
    fontWeight: '600',
  },
  simHubSection: {
    backgroundColor: '#161622',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 12,
  },
  simHubHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  simHubTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 13,
    letterSpacing: 0.5,
  },
  simTabsRow: {
    flexDirection: 'row',
    backgroundColor: '#0D0D14',
    padding: 4,
    borderRadius: 12,
    gap: 4,
  },
  simTab: {
    flex: 1,
    paddingVertical: 8,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
  },
  simTabActive: {
    backgroundColor: Colors.studioOrange,
  },
  simTabText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 10,
    fontWeight: '700',
  },
  simTabTextActive: {
    color: Colors.textWhite,
  },
});
