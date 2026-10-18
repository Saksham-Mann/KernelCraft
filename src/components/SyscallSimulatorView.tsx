import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { SYSCALL_STEPS } from '../data/syscallData';
import { Ionicons } from '@expo/vector-icons';

export const SyscallSimulatorView: React.FC = () => {
  const [currentStepIndex, setCurrentStepIndex] = useState(0);

  const step = SYSCALL_STEPS[currentStepIndex];
  const isLastStep = currentStepIndex === SYSCALL_STEPS.length - 1;
  const isFirstStep = currentStepIndex === 0;

  const nextStep = () => {
    if (!isLastStep) setCurrentStepIndex(currentStepIndex + 1);
  };

  const prevStep = () => {
    if (!isFirstStep) setCurrentStepIndex(currentStepIndex - 1);
  };

  const reset = () => {
    setCurrentStepIndex(0);
  };

  return (
    <View style={styles.container}>
      {/* Step Indicator Header */}
      <View style={styles.stepHeader}>
        <View style={styles.stepDotsRow}>
          {SYSCALL_STEPS.map((s, idx) => (
            <TouchableOpacity
              key={s.stepIndex}
              style={[
                styles.stepDot,
                idx === currentStepIndex && styles.stepDotActive,
                idx < currentStepIndex && styles.stepDotDone,
              ]}
              onPress={() => setCurrentStepIndex(idx)}
            >
              <Text
                style={[
                  styles.stepDotText,
                  idx === currentStepIndex && { color: Colors.textWhite },
                ]}
              >
                {idx + 1}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* CPU Privilege Ring Indicator */}
        <View
          style={[
            styles.modeBadge,
            step.ringMode === 'KERNEL' ? styles.modeBadgeKernel : styles.modeBadgeUser,
          ]}
        >
          <Ionicons
            name={step.ringMode === 'KERNEL' ? 'shield-checkmark' : 'person-outline'}
            size={12}
            color={Colors.textWhite}
          />
          <Text style={styles.modeBadgeText}>
            {step.ringMode === 'KERNEL' ? 'KERNEL RING 0 (EL1)' : 'USER RING 3 (EL0)'}
          </Text>
        </View>
      </View>

      {/* Step Title & Subtitle */}
      <View style={styles.titleSection}>
        <Text style={styles.stepTitle}>{step.title}</Text>
        <Text style={styles.stepSubtitle}>{step.subtitle}</Text>
      </View>

      {/* Code / Assembly Snippet Card */}
      <View style={styles.codeCard}>
        <View style={styles.codeCardHeader}>
          <Text style={styles.codeCardLang}>
            {step.stepIndex === 0
              ? 'C / USER CODE'
              : step.stepIndex === 1
              ? 'ARM64 ASSEMBLY (LIBC)'
              : step.stepIndex === 2
              ? 'CPU HARDWARE TRAP'
              : step.stepIndex === 3
              ? 'LINUX KERNEL C'
              : 'RETURN EXCEPTION'}
          </Text>
          <View style={styles.statusDot} />
        </View>
        <Text style={styles.codeText}>{step.codeSnippet}</Text>
      </View>

      {/* Conceptual Explanation */}
      <View style={styles.explanationBox}>
        <Ionicons name="hardware-chip-outline" size={16} color={Colors.studioOrange} />
        <Text style={styles.explanationText}>{step.explanation}</Text>
      </View>

      {/* ARM64 CPU Register Inspection Panel */}
      <View style={styles.registersPanel}>
        <Text style={styles.registersTitle}>CPU REGISTERS (ARMv9):</Text>
        <View style={styles.registerRow}>
          <View style={styles.registerCell}>
            <Text style={styles.regName}>X8 (Syscall NR)</Text>
            <Text style={styles.regVal}>{step.registerState.x8}</Text>
          </View>
          <View style={styles.registerCell}>
            <Text style={styles.regName}>X0 (Arg 1 / Ret)</Text>
            <Text style={styles.regVal}>{step.registerState.x0}</Text>
          </View>
          <View style={styles.registerCell}>
            <Text style={styles.regName}>X1 (Arg 2 / Buf)</Text>
            <Text style={styles.regVal} numberOfLines={1}>
              {step.registerState.x1}
            </Text>
          </View>
          <View style={styles.registerCell}>
            <Text style={styles.regName}>X2 (Arg 3 / Count)</Text>
            <Text style={styles.regVal}>{step.registerState.x2}</Text>
          </View>
        </View>
      </View>

      {/* Stepper Navigation Buttons */}
      <View style={styles.controlsRow}>
        <TouchableOpacity
          style={[styles.navButton, isFirstStep && styles.navButtonDisabled]}
          onPress={prevStep}
          disabled={isFirstStep}
        >
          <Ionicons name="arrow-back" size={16} color={isFirstStep ? Colors.inkLight : Colors.textWhite} />
          <Text style={[styles.navButtonText, isFirstStep && { color: Colors.inkLight }]}>BACK</Text>
        </TouchableOpacity>

        {isLastStep ? (
          <TouchableOpacity style={[styles.navButton, styles.restartButton]} onPress={reset}>
            <Ionicons name="refresh" size={16} color={Colors.textWhite} />
            <Text style={styles.navButtonText}>REPLAY SIMULATION</Text>
          </TouchableOpacity>
        ) : (
          <TouchableOpacity style={[styles.navButton, styles.nextButton]} onPress={nextStep}>
            <Text style={styles.navButtonText}>NEXT STEP</Text>
            <Ionicons name="arrow-forward" size={16} color={Colors.textWhite} />
          </TouchableOpacity>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#161622',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.09)',
    marginVertical: 6,
  },
  stepHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  stepDotsRow: {
    flexDirection: 'row',
    gap: 6,
  },
  stepDot: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  stepDotActive: {
    backgroundColor: Colors.studioOrange,
  },
  stepDotDone: {
    backgroundColor: Colors.steelBlue,
  },
  stepDotText: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 10,
    fontWeight: '700',
  },
  modeBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  modeBadgeUser: {
    backgroundColor: Colors.steelBlue,
  },
  modeBadgeKernel: {
    backgroundColor: '#D32F2F',
  },
  modeBadgeText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 9,
  },
  titleSection: {
    marginBottom: 10,
  },
  stepTitle: {
    ...Typography.subhead,
    color: Colors.textWhite,
    fontSize: 16,
  },
  stepSubtitle: {
    ...Typography.bodySmall,
    color: Colors.inkLight,
    fontSize: 12,
    marginTop: 2,
  },
  codeCard: {
    backgroundColor: '#0D0D14',
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    marginBottom: 10,
  },
  codeCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.08)',
    paddingBottom: 4,
  },
  codeCardLang: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 9,
  },
  statusDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#34C759',
  },
  codeText: {
    ...Typography.code,
    color: '#A9B7C6',
    fontSize: 11,
    lineHeight: 16,
  },
  explanationBox: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderRadius: 10,
    padding: 10,
    marginBottom: 10,
  },
  explanationText: {
    ...Typography.bodySmall,
    color: Colors.textWhite,
    fontSize: 12,
    lineHeight: 17,
    flex: 1,
  },
  registersPanel: {
    backgroundColor: 'rgba(0, 0, 0, 0.35)',
    borderRadius: 10,
    padding: 10,
    marginBottom: 12,
  },
  registersTitle: {
    ...Typography.tag,
    color: Colors.inkLight,
    fontSize: 9,
    marginBottom: 6,
  },
  registerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 4,
  },
  registerCell: {
    flex: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderRadius: 6,
    padding: 6,
  },
  regName: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 8,
  },
  regVal: {
    ...Typography.code,
    color: Colors.textWhite,
    fontSize: 10,
    marginTop: 2,
    fontWeight: '700',
  },
  controlsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
  },
  navButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 10,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
  },
  navButtonDisabled: {
    opacity: 0.3,
  },
  nextButton: {
    backgroundColor: Colors.studioOrange,
  },
  restartButton: {
    backgroundColor: Colors.steelBlue,
  },
  navButtonText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 11,
  },
});
