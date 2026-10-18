import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Ionicons } from '@expo/vector-icons';

type BinderStep = 'IDLE' | 'MARSHAL' | 'KERNEL' | 'SERVICE' | 'RETURN';

export const BinderTracer: React.FC = () => {
  const [step, setStep] = useState<BinderStep>('IDLE');
  const [txCount, setTxCount] = useState(1420);

  const triggerTransaction = () => {
    setStep('MARSHAL');
    setTimeout(() => setStep('KERNEL'), 500);
    setTimeout(() => setStep('SERVICE'), 1100);
    setTimeout(() => setStep('RETURN'), 1700);
    setTimeout(() => {
      setStep('IDLE');
      setTxCount((c) => c + 1);
    }, 2400);
  };

  return (
    <View style={styles.card}>
      <View style={styles.headerRow}>
        <View>
          <Text style={styles.cardTitle}>Binder IPC Zero-Copy Tracer</Text>
          <Text style={styles.cardSubtitle}>
            android.os.Binder / BINDER_WRITE_READ single-copy mapping
          </Text>
        </View>

        <TouchableOpacity
          style={[styles.dispatchButton, step !== 'IDLE' && styles.dispatchButtonActive]}
          onPress={triggerTransaction}
          disabled={step !== 'IDLE'}
        >
          <Ionicons
            name={step !== 'IDLE' ? 'sync' : 'paper-plane-outline'}
            size={14}
            color={Colors.textWhite}
          />
          <Text style={styles.dispatchButtonText}>
            {step === 'IDLE' ? 'SEND IPC' : 'TRANSMITTING...'}
          </Text>
        </TouchableOpacity>
      </View>

      {/* Process Flow Diagram */}
      <View style={styles.flowContainer}>
        {/* Client Process */}
        <View
          style={[
            styles.processBox,
            (step === 'MARSHAL' || step === 'RETURN') && styles.processBoxActive,
          ]}
        >
          <Text style={styles.processRole}>CLIENT PROCESS</Text>
          <Text style={styles.processName}>com.kernelcraft</Text>
          <Text style={styles.pidText}>PID: 4821 (User u0_a184)</Text>
          {step === 'MARSHAL' && (
            <View style={styles.bubble}>
              <Text style={styles.bubbleText}>Marshalling Parcel</Text>
            </View>
          )}
        </View>

        {/* Center Kernel Driver Channel */}
        <View style={styles.bridgeChannel}>
          <View style={[styles.channelLine, step === 'KERNEL' && styles.channelLineActive]} />
          <View style={[styles.driverBadge, step === 'KERNEL' && styles.driverBadgeActive]}>
            <Text style={styles.driverBadgeText}>/dev/binder</Text>
            <Text style={styles.driverSubText}>mmap() page</Text>
          </View>
          <View style={[styles.channelLine, step === 'KERNEL' && styles.channelLineActive]} />
        </View>

        {/* Target Server Process */}
        <View
          style={[
            styles.processBox,
            step === 'SERVICE' && styles.processBoxActive,
          ]}
        >
          <Text style={styles.processRole}>TARGET SERVER</Text>
          <Text style={styles.processName}>system_server</Text>
          <Text style={styles.pidText}>PID: 1420 (System UID)</Text>
          {step === 'SERVICE' && (
            <View style={styles.bubble}>
              <Text style={styles.bubbleText}>AIDL Stub Exec</Text>
            </View>
          )}
        </View>
      </View>

      {/* Transaction Diagnostics Footer */}
      <View style={styles.telemetryBar}>
        <View style={styles.metricPair}>
          <Text style={styles.metricLabel}>INTERFACE</Text>
          <Text style={styles.metricValue}>IWindowSession::relayout</Text>
        </View>

        <View style={styles.metricPair}>
          <Text style={styles.metricLabel}>LATENCY</Text>
          <Text style={styles.metricValue}>185 µs</Text>
        </View>

        <View style={styles.metricPair}>
          <Text style={styles.metricLabel}>TX TOTAL</Text>
          <Text style={styles.metricValue}>{txCount}</Text>
        </View>
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
  dispatchButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: Colors.studioOrange,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 10,
  },
  dispatchButtonActive: {
    backgroundColor: Colors.steelBlue,
  },
  dispatchButtonText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 10,
  },
  flowContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: 'rgba(0, 0, 0, 0.25)',
    borderRadius: 14,
    padding: 12,
    marginVertical: 4,
  },
  processBox: {
    flex: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 10,
    padding: 8,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  processBoxActive: {
    borderColor: Colors.studioOrange,
    backgroundColor: 'rgba(221, 86, 34, 0.15)',
  },
  processRole: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 8,
    marginBottom: 2,
  },
  processName: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 11,
  },
  pidText: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 9,
    marginTop: 2,
  },
  bubble: {
    backgroundColor: Colors.studioOrange,
    borderRadius: 4,
    paddingHorizontal: 4,
    paddingVertical: 2,
    marginTop: 4,
    alignSelf: 'flex-start',
  },
  bubbleText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 8,
  },
  bridgeChannel: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 6,
  },
  channelLine: {
    width: 2,
    height: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
  },
  channelLineActive: {
    backgroundColor: Colors.studioOrange,
  },
  driverBadge: {
    backgroundColor: 'rgba(78, 127, 158, 0.25)',
    borderWidth: 1,
    borderColor: Colors.steelBlue,
    borderRadius: 6,
    paddingHorizontal: 6,
    paddingVertical: 3,
    alignItems: 'center',
  },
  driverBadgeActive: {
    backgroundColor: Colors.studioOrange,
    borderColor: Colors.studioOrange,
  },
  driverBadgeText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 9,
  },
  driverSubText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 7,
  },
  telemetryBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: 10,
    padding: 10,
    marginTop: 10,
  },
  metricPair: {
    alignItems: 'center',
  },
  metricLabel: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 8,
    marginBottom: 2,
  },
  metricValue: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontWeight: '700',
    fontSize: 11,
  },
});
