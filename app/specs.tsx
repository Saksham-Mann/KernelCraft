import React from 'react';
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
import { SUMMARY_CHIPS, SPEC_CATEGORIES } from '../src/data/specsData';
import { Ionicons } from '@expo/vector-icons';

export default function FullSpecsScreen() {
  const router = useRouter();

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.container}>
        {/* Navigation Bar */}
        <View style={styles.topBar}>
          <TouchableOpacity
            style={styles.circleButton}
            onPress={() => router.back()}
            accessibilityLabel="Close specifications"
          >
            <Ionicons name="close" size={20} color={Colors.textWhite} />
          </TouchableOpacity>

          <View style={styles.topBarTitle}>
            <Text style={styles.headerTitle}>SPECIFICATION MATRIX</Text>
            <Text style={styles.headerSubtitle}>PHYSICAL HARDWARE TELEMETRY</Text>
          </View>

          <View style={{ width: 40 }} />
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          {/* Top Summary Metric Chips */}
          <View style={styles.summaryChipsRow}>
            {SUMMARY_CHIPS.map((chip) => (
              <View key={chip.id} style={styles.summaryChip}>
                <Text style={styles.summaryChipLabel}>{chip.title}</Text>
                <Text style={styles.summaryChipMetric}>{chip.metric}</Text>
              </View>
            ))}
          </View>

          {/* Detailed Categories */}
          {SPEC_CATEGORIES.map((category) => (
            <View key={category.id} style={styles.categoryCard}>
              <View style={styles.categoryHeader}>
                <View>
                  <Text style={styles.categoryTitle}>{category.title}</Text>
                  <Text style={styles.categorySubtitle}>{category.subtitle}</Text>
                </View>
                <View style={styles.categoryBadge}>
                  <Text style={styles.categoryBadgeText}>{category.accentTag}</Text>
                </View>
              </View>

              <View style={styles.specsList}>
                {category.specs.map((item, index) => (
                  <View key={index} style={styles.specItem}>
                    <View style={styles.specHeaderRow}>
                      <Text style={styles.specLabel}>{item.label}</Text>
                      {item.badge && (
                        <View style={styles.itemBadge}>
                          <Text style={styles.itemBadgeText}>{item.badge}</Text>
                        </View>
                      )}
                    </View>

                    <Text style={styles.specValue}>{item.value}</Text>
                    {item.detail && <Text style={styles.specDetail}>{item.detail}</Text>}
                  </View>
                ))}
              </View>
            </View>
          ))}
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
  circleButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  topBarTitle: {
    alignItems: 'center',
  },
  headerTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 14,
    letterSpacing: 1,
  },
  headerSubtitle: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 8,
    marginTop: 1,
  },
  scrollContent: {
    padding: 16,
    gap: 16,
    paddingBottom: 40,
  },
  summaryChipsRow: {
    flexDirection: 'row',
    gap: 8,
  },
  summaryChip: {
    flex: 1,
    backgroundColor: '#161622',
    borderRadius: 12,
    padding: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
    alignItems: 'center',
  },
  summaryChipLabel: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 8,
    marginBottom: 2,
  },
  summaryChipMetric: {
    ...Typography.bodySmall,
    color: Colors.studioOrange,
    fontWeight: '800',
    fontSize: 11,
  },
  categoryCard: {
    backgroundColor: '#161622',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
  },
  categoryHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 14,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.06)',
    paddingBottom: 10,
  },
  categoryTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 16,
  },
  categorySubtitle: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
    marginTop: 2,
  },
  categoryBadge: {
    backgroundColor: 'rgba(221, 86, 34, 0.2)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: 'rgba(221, 86, 34, 0.4)',
  },
  categoryBadgeText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 9,
  },
  specsList: {
    gap: 12,
  },
  specItem: {
    gap: 2,
  },
  specHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  specLabel: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
  },
  itemBadge: {
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  itemBadgeText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 8,
  },
  specValue: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 13,
  },
  specDetail: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
    lineHeight: 15,
  },
});
