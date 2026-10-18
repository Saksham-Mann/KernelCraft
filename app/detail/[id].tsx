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
            <View style={[styles.categoryBadge, { backgroundColor: component.accentColor }]}>
              <Text style={styles.categoryBadgeText}>{component.category}</Text>
            </View>
          </View>

          <View style={{ width: 40 }} />
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          {/* Main Title Card */}
          <View style={styles.titleCard}>
            <Text style={styles.titleText}>{component.telemetry.technicalName}</Text>
            <Text style={[styles.tagText, { color: component.accentColor }]}>
              {component.telemetry.subtitle}
            </Text>

            <View style={styles.summaryBox}>
              <Text style={styles.summaryText}>{component.telemetry.summary}</Text>
            </View>
          </View>

          {/* Component Diagram / Visual Card */}
          <View style={styles.visualCard}>
            <View style={styles.visualHeader}>
              <Ionicons name={component.icon as any} size={20} color={component.accentColor} />
              <Text style={styles.visualTitle}>HARDWARE ARCHITECTURE</Text>
            </View>
            <View style={styles.visualBox}>
              <Ionicons
                name={component.icon as any}
                size={64}
                color={component.accentColor}
              />
              <Text style={styles.archDetailText}>{component.telemetry.architecture}</Text>
            </View>
          </View>

          {/* Technical Specifications Grid */}
          <View style={styles.specsCard}>
            <Text style={styles.sectionHeader}>TECHNICAL SPECIFICATIONS</Text>

            <View style={styles.specRow}>
              <Text style={styles.specKey}>Architecture</Text>
              <Text style={styles.specVal}>{component.telemetry.architecture}</Text>
            </View>

            <View style={styles.specRow}>
              <Text style={styles.specKey}>Bus & Interface</Text>
              <Text style={styles.specVal}>{component.telemetry.interfaceSpec}</Text>
            </View>

            <View style={styles.specRow}>
              <Text style={styles.specKey}>Key Features</Text>
              {component.telemetry.keyFeatures.map((feat, i) => (
                <View key={i} style={styles.featureBulletRow}>
                  <Ionicons name="checkmark-circle" size={14} color={component.accentColor} />
                  <Text style={styles.featureBulletText}>{feat}</Text>
                </View>
              ))}
            </View>
          </View>

          {/* Primary Action Button */}
          <TouchableOpacity style={styles.secondaryButton} onPress={() => router.back()}>
            <Text style={styles.secondaryButtonText}>RETURN TO TEARDOWN</Text>
          </TouchableOpacity>
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
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitleCenter: {
    alignItems: 'center',
  },
  categoryBadge: {
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
  },
  categoryBadgeText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 10,
    fontWeight: '800',
  },
  scrollContent: {
    padding: 18,
    gap: 16,
    paddingBottom: 40,
  },
  titleCard: {
    backgroundColor: '#161622',
    borderRadius: 20,
    padding: 18,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 6,
  },
  titleText: {
    ...Typography.headline,
    color: Colors.textWhite,
    fontSize: 20,
    fontWeight: '800',
  },
  tagText: {
    ...Typography.bodySmall,
    fontWeight: '700',
    fontSize: 13,
  },
  summaryBox: {
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderRadius: 12,
    padding: 12,
    marginTop: 6,
    borderLeftWidth: 3,
    borderLeftColor: Colors.studioOrange,
  },
  summaryText: {
    ...Typography.body,
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '500',
  },
  visualCard: {
    backgroundColor: '#161622',
    borderRadius: 20,
    padding: 18,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 12,
  },
  visualHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  visualTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 13,
    letterSpacing: 0.5,
  },
  visualBox: {
    backgroundColor: '#0D0D14',
    borderRadius: 14,
    padding: 20,
    alignItems: 'center',
    gap: 14,
  },
  archDetailText: {
    ...Typography.body,
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: 13,
    lineHeight: 19,
    textAlign: 'center',
    fontWeight: '600',
  },
  specsCard: {
    backgroundColor: '#161622',
    borderRadius: 20,
    padding: 18,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 12,
  },
  sectionHeader: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  specRow: {
    flexDirection: 'column',
    gap: 4,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.05)',
  },
  specKey: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: 10,
  },
  specVal: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontSize: 13,
    fontWeight: '600',
  },
  featureBulletRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 3,
  },
  featureBulletText: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: 12,
  },
  secondaryButton: {
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 16,
    paddingVertical: 14,
    alignItems: 'center',
  },
  secondaryButtonText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
});
