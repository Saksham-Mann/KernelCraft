import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Ionicons } from '@expo/vector-icons';

export const MemorySpaceVisualizer: React.FC = () => {
  const [activeSegment, setActiveSegment] = useState<'stack' | 'heap' | 'zram' | 'kernel'>('heap');

  return (
    <View style={styles.card}>
      <View style={styles.headerRow}>
        <View>
          <Text style={styles.cardTitle}>Virtual Memory Space & ZRAM</Text>
          <Text style={styles.cardSubtitle}>
            Linux 4GB 3:1 Address Split (3GB Userspace / 1GB Kernel)
          </Text>
        </View>

        <View style={styles.ratioBadge}>
          <Text style={styles.ratioBadgeText}>2.85x ZRAM LZ4</Text>
        </View>
      </View>

      {/* Memory Block Visualizer */}
      <View style={styles.memoryMapContainer}>
        {/* Kernel Space (Top 1GB) */}
        <TouchableOpacity
          activeOpacity={0.8}
          style={[styles.memBlock, styles.kernelBlock, activeSegment === 'kernel' && styles.memBlockActive]}
          onPress={() => setActiveSegment('kernel')}
        >
          <View style={styles.memBlockHeader}>
            <Text style={styles.memBlockTitle}>0xC0000000 - 0xFFFFFFFF (1 GB)</Text>
            <Text style={styles.memTag}>KERNEL SPACE (RING 0)</Text>
          </View>
          <Text style={styles.memBlockDesc}>Page tables, Slab Allocator, Kernel Code & Driver MMIO</Text>
        </TouchableOpacity>

        {/* Stack */}
        <TouchableOpacity
          activeOpacity={0.8}
          style={[styles.memBlock, styles.stackBlock, activeSegment === 'stack' && styles.memBlockActive]}
          onPress={() => setActiveSegment('stack')}
        >
          <View style={styles.memBlockHeader}>
            <Text style={styles.memBlockTitle}>User Stack (Grows Down)</Text>
            <Text style={styles.memTag}>LOCAL FRAMES</Text>
          </View>
          <Text style={styles.memBlockDesc}>Function call stack frames, local variables, return addresses</Text>
        </TouchableOpacity>

        {/* Heap & Mmap */}
        <TouchableOpacity
          activeOpacity={0.8}
          style={[styles.memBlock, styles.heapBlock, activeSegment === 'heap' && styles.memBlockActive]}
          onPress={() => setActiveSegment('heap')}
        >
          <View style={styles.memBlockHeader}>
            <Text style={styles.memBlockTitle}>ART Runtime Heap & mmap()</Text>
            <Text style={styles.memTag}>DYN ALLOC</Text>
          </View>
          <Text style={styles.memBlockDesc}>Concurrent Copying GC objects, Bitmap hardware buffers, DEX code</Text>
        </TouchableOpacity>

        {/* ZRAM Swap Compressed Block */}
        <TouchableOpacity
          activeOpacity={0.8}
          style={[styles.memBlock, styles.zramBlock, activeSegment === 'zram' && styles.memBlockActive]}
          onPress={() => setActiveSegment('zram')}
        >
          <View style={styles.memBlockHeader}>
            <Text style={styles.memBlockTitle}>ZRAM Compressed Swap (RAM-backed)</Text>
            <Text style={styles.memTag}>COMPRESSED</Text>
          </View>
          <Text style={styles.memBlockDesc}>1.8 GB anonymous pages compressed into 630 MB RAM partition</Text>
        </TouchableOpacity>
      </View>

      {/* Detail Inspector Card */}
      <View style={styles.inspectorCard}>
        <View style={styles.inspectorHeader}>
          <Ionicons name="information-circle" size={16} color={Colors.studioOrange} />
          <Text style={styles.inspectorTitle}>
            {activeSegment === 'kernel'
              ? 'Kernel Memory Isolation'
              : activeSegment === 'stack'
              ? 'Thread Execution Stack'
              : activeSegment === 'heap'
              ? 'Managed ART Object Space'
              : 'ZRAM Swap Mechanics'}
          </Text>
        </View>
        <Text style={styles.inspectorText}>
          {activeSegment === 'kernel'
            ? 'Protected by ARM MMU Page Table translation entries (TTBR1_EL1). Attempting to dereference a kernel pointer from unprivileged userspace immediately generates a SIGSEGV / synchronous Translation Fault.'
            : activeSegment === 'stack'
            ? 'Each Android thread receives an isolated 1MB virtual stack with a guard page. Guard pages cause an uncatchable StackOverflowError crash if unbounded recursion exhausts the boundary.'
            : activeSegment === 'heap'
            ? 'Android Runtime (ART) uses a two-space generational concurrent copying collector. New allocations happen in the RosAlloc bump-pointer arena without stop-the-world pauses.'
            : 'When physical RAM reaches low thresholds, the Linux kswapd daemon passes anonymous memory through LZ4 compression into /dev/block/zram0, preventing sluggish flash wear and keeping background apps alive.'}
        </Text>
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
  ratioBadge: {
    backgroundColor: 'rgba(52, 199, 89, 0.18)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(52, 199, 89, 0.4)',
  },
  ratioBadgeText: {
    ...Typography.tag,
    color: '#34C759',
    fontSize: 9,
  },
  memoryMapContainer: {
    gap: 8,
    marginBottom: 12,
  },
  memBlock: {
    borderRadius: 10,
    padding: 10,
    borderWidth: 1.5,
  },
  memBlockActive: {
    borderColor: Colors.studioOrange,
    backgroundColor: 'rgba(221, 86, 34, 0.15)',
  },
  kernelBlock: {
    backgroundColor: 'rgba(78, 127, 158, 0.15)',
    borderColor: 'rgba(78, 127, 158, 0.35)',
  },
  stackBlock: {
    backgroundColor: 'rgba(232, 160, 32, 0.12)',
    borderColor: 'rgba(232, 160, 32, 0.3)',
  },
  heapBlock: {
    backgroundColor: 'rgba(52, 199, 89, 0.12)',
    borderColor: 'rgba(52, 199, 89, 0.3)',
  },
  zramBlock: {
    backgroundColor: 'rgba(255, 112, 51, 0.12)',
    borderColor: 'rgba(255, 112, 51, 0.3)',
  },
  memBlockHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  memBlockTitle: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 11,
  },
  memTag: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 8,
  },
  memBlockDesc: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 10,
  },
  inspectorCard: {
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
    borderRadius: 12,
    padding: 12,
    borderLeftWidth: 3,
    borderLeftColor: Colors.studioOrange,
  },
  inspectorHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 4,
  },
  inspectorTitle: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 12,
  },
  inspectorText: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 11,
    lineHeight: 16,
  },
});
