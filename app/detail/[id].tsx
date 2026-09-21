import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Colors } from '../../src/theme/colors';
import { Typography } from '../../src/theme/typography';
import { TEARDOWN_ANNOTATIONS } from '../../src/data/teardownData';
import { Ionicons } from '@expo/vector-icons';

export default function ComponentDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();

  const component =
    TEARDOWN_ANNOTATIONS.find((c) => c.id === id) || TEARDOWN_ANNOTATIONS[0];

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Navigation Header */}
        <View style={styles.topBar}>
          <TouchableOpacity
            style={styles.closeButton}
            onPress={() => router.back()}
            accessibilityLabel="Close detail"
          >
            <Ionicons name="close" size={20} color={Colors.textWhite} />
          </TouchableOpacity>

          <View style={styles.headerTitleCenter}>
            <View style={styles.categoryBadge}>
              <Text style={styles.categoryBadgeText}>{component.category}</Text>
            </View>
          </View>

          <View style={{ width: 40 }} />
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          {/* Main Title Card */}
          <View style={styles.titleCard}>
            <Text style={styles.titleText}>{component.name}</Text>
            <Text style={styles.tagText}>{component.tag}</Text>

            <View style={styles.anchorBadgeRow}>
              <Text style={styles.anchorBadgeText}>
                Spatial Anchor: X={Math.round(component.anchorX * 100)}% Y={Math.round(component.anchorY * 100)}%
              </Text>
            </View>
          </View>

          {/* Component Diagram / Architectural Highlight */}
          <View style={styles.visualCard}>
            <View style={styles.visualHeader}>
              <Ionicons name="hardware-chip-outline" size={18} color={Colors.studioOrange} />
              <Text style={styles.visualTitle}>HARDWARE BLUEPRINT</Text>
            </View>
            <View style={styles.visualBox}>
              <Ionicons
                name={
                  component.category === 'SILICON'
                    ? 'hardware-chip'
                    : component.category === 'OPTICS'
                    ? 'camera'
                    : component.category === 'POWER'
                    ? 'battery-charging'
                    : 'thermometer'
                }
                size={54}
                color={Colors.studioOrange}
              />
              <Text style={styles.blueprintLabel}>{component.telemetry.architecture}</Text>
            </View>
          </View>

          {/* Engineering Specifications Grid */}
          <View style={styles.specsCard}>
            <Text style={styles.sectionHeader}>TECHNICAL SPECIFICATIONS</Text>

            <View style={styles.specRow}>
              <Text style={styles.specKey}>Material & Packaging</Text>
              <Text style={styles.specVal}>{component.telemetry.material}</Text>
            </View>

            <View style={styles.specRow}>
              <Text style={styles.specKey}>Bus & Interface</Text>
              <Text style={styles.specVal}>{component.telemetry.busInterface}</Text>
            </View>

            <View style={styles.specRow}>
              <Text style={styles.specKey}>Power Draw</Text>
              <Text style={styles.specVal}>{component.telemetry.powerDraw}</Text>
            </View>

            <View style={styles.specRow}>
              <Text style={styles.specKey}>Operating Frequency</Text>
              <Text style={styles.specVal}>{component.telemetry.operatingFreq}</Text>
            </View>

            <View style={styles.specRow}>
              <Text style={styles.specKey}>Thermal Envelope</Text>
              <Text style={styles.specVal}>{component.telemetry.thermalEnvelope}</Text>
            </View>
          </View>

          {/* Engineering Field Notes */}
          <View style={styles.notesCard}>
            <View style={styles.notesHeader}>
              <Ionicons name="build-outline" size={16} color={Colors.mustard} />
              <Text style={styles.notesTitle}>ENGINEERING FIELD NOTES</Text>
            </View>
            <Text style={styles.notesContent}>{component.telemetry.engineeringNotes}</Text>
          </View>

          {/* Primary Action Button */}
          {component.id === 'soc_processor' ? (
            <TouchableOpacity
              style={styles.actionButton}
              onPress={() => {
                router.push('/transition');
              }}
            >
              <Text style={styles.actionButtonText}>DIVE INTO SILICON DIE & OS STACK</Text>
              <Ionicons name="arrow-forward" size={18} color={Colors.textWhite} />
            </TouchableOpacity>
          ) : (
            <TouchableOpacity style={styles.secondaryButton} onPress={() => router.back()}>
              <Text style={styles.secondaryButtonText}>RETURN TO TEARDOWN</Text>
            </TouchableOpacity>
          )}
        </ScrollView>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#0D0D14',
  },
  container: {
    flex: 1,
    backgroundColor: '#0D0D14',
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.08)',
  },
  closeButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitleCenter: {
    alignItems: 'center',
  },
  categoryBadge: {
    backgroundColor: Colors.studioOrange,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 8,
  },
  categoryBadgeText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 10,
  },
  scrollContent: {
    padding: 16,
    gap: 14,
    paddingBottom: 40,
  },
  titleCard: {
    backgroundColor: '#161622',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
  },
  titleText: {
    ...Typography.headline,
    color: Colors.textWhite,
    fontSize: 22,
  },
  tagText: {
    ...Typography.subhead,
    color: Colors.studioOrange,
    fontSize: 14,
    marginTop: 4,
  },
  anchorBadgeRow: {
    marginTop: 10,
    alignSelf: 'flex-start',
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  anchorBadgeText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
  },
  visualCard: {
    backgroundColor: '#161622',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
  },
  visualHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 12,
  },
  visualTitle: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 10,
  },
  visualBox: {
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
    borderRadius: 12,
    padding: 18,
    alignItems: 'center',
    gap: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  blueprintLabel: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    textAlign: 'center',
    fontWeight: '600',
  },
  specsCard: {
    backgroundColor: '#161622',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 10,
  },
  sectionHeader: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 10,
    marginBottom: 4,
  },
  specRow: {
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.06)',
    paddingBottom: 8,
  },
  specKey: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
    marginBottom: 2,
  },
  specVal: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontSize: 13,
  },
  notesCard: {
    backgroundColor: 'rgba(232, 160, 32, 0.08)',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(232, 160, 32, 0.25)',
  },
  notesHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 8,
  },
  notesTitle: {
    ...Typography.tag,
    color: Colors.mustard,
    fontSize: 10,
  },
  notesContent: {
    ...Typography.body,
    color: Colors.textWhite,
    fontSize: 13,
    lineHeight: 18,
  },
  actionButton: {
    backgroundColor: Colors.studioOrange,
    borderRadius: 16,
    paddingVertical: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    marginTop: 8,
  },
  actionButtonText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 12,
  },
  secondaryButton: {
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    borderRadius: 16,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 8,
  },
  secondaryButtonText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 12,
  },
});
