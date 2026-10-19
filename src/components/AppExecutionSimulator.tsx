import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Animated,
  Platform,
} from 'react-native';
import { Colors } from '../theme/colors';
import { Typography, FontFamilies } from '../theme/typography';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

export interface StoryChoice {
  id: string;
  type: 'GOOD' | 'RISKY' | 'WRONG';
  text: string;
  feedback?: string;
  failureTitle?: string;
  failureDetails?: string;
}

export interface StoryStage {
  id: number;
  stageNumber: number;
  title: string;
  story: string;
  icon: keyof typeof Ionicons.glyphMap;
  accentColor: string;
  choices: StoryChoice[];
}

const STORY_STAGES: StoryStage[] = [
  {
    id: 1,
    stageNumber: 1,
    title: 'Translating Your Code',
    story:
      "You just finished writing your app. But the phone's chip only understands 1s and 0s, not human words. What do you do?",
    icon: 'code-slash-outline',
    accentColor: '#34C759',
    choices: [
      {
        id: 's1_good',
        type: 'GOOD',
        text: 'Translate the code into machine numbers before installing the app.',
        feedback:
          'Great choice! The app is completely translated and ready to run at top speed.',
      },
      {
        id: 's1_risky',
        type: 'RISKY',
        text: 'Do not translate beforehand; translate each line one-by-one while the user is using the app.',
        feedback:
          'The app works, but it takes forever to open and wastes battery because the phone has to translate on the go.',
      },
      {
        id: 's1_wrong',
        type: 'WRONG',
        text: 'Feed the raw typed words directly into the processor chip.',
        failureTitle: 'Processor Error: Cannot Understand Words',
        failureDetails:
          'The processor chip only reads electric numbers (1s and 0s). It crashed because it tried to read plain English letters.',
      },
    ],
  },
  {
    id: 2,
    stageNumber: 2,
    title: 'Adding Extra Tools & Features',
    story:
      "Your app needs common features like 3D graphics and sound tools built by the phone maker. How do you include them?",
    icon: 'cube-outline',
    accentColor: '#007AFF',
    choices: [
      {
        id: 's2_good',
        type: 'GOOD',
        text: "Borrow the shared tools that are already built into the phone's operating system.",
        feedback:
          'Smart move! Your app stays tiny in size because it shares the tools already stored on the phone.',
      },
      {
        id: 's2_risky',
        type: 'RISKY',
        text: 'Pack an entire duplicate copy of every single tool inside your own app.',
        feedback:
          'The app works, but the file size is massive and takes up too much storage space on the phone.',
      },
      {
        id: 's2_wrong',
        type: 'WRONG',
        text: 'Just pack the code files together without connecting them to any tools at all.',
        failureTitle: 'Missing Tools: App Cannot Find Functions',
        failureDetails:
          'The app tried to draw buttons on the screen, but could not find the drawing tools because you forgot to connect them.',
      },
    ],
  },
  {
    id: 3,
    stageNumber: 3,
    title: 'Loading Into Fast Memory (RAM)',
    story:
      "The user tapped your app icon. The app is currently sitting on the storage drive, but the chip needs it inside fast working memory (RAM). How do you load it?",
    icon: 'flash-outline',
    accentColor: '#AF52DE',
    choices: [
      {
        id: 's3_good',
        type: 'GOOD',
        text: 'Load only the parts the user needs right now, and load the rest as they browse.',
        feedback:
          "The app pops up almost instantly without hogging the phone's memory.",
      },
      {
        id: 's3_risky',
        type: 'RISKY',
        text: 'Force the phone to load the entire app and all its heavy files into memory all at once.',
        feedback:
          'The phone started lagging and had to close background apps because your app hogged all the available memory.',
      },
      {
        id: 's3_wrong',
        type: 'WRONG',
        text: 'Tell the processor chip to run the app directly from the slow storage drive without using RAM.',
        failureTitle: 'Hardware Freeze: Storage Is Too Slow',
        failureDetails:
          'The storage drive is way too slow to keep up with the processor. The chip gave up waiting and stopped the app.',
      },
    ],
  },
  {
    id: 4,
    stageNumber: 4,
    title: 'Asking for Camera Permission',
    story:
      "Your app is running, but now it needs to open the camera lens. The phone has security rules to stop apps from spying. How do you get access?",
    icon: 'camera-outline',
    accentColor: '#FF9500',
    choices: [
      {
        id: 's4_good',
        type: 'GOOD',
        text: "Politely ask the phone's operating system to check permissions and turn on the camera for you.",
        feedback:
          'Permission checked and verified! The system safely turns on the camera feed.',
      },
      {
        id: 's4_risky',
        type: 'RISKY',
        text: 'Use an unverified background bridge to sneak a look at the camera.',
        feedback:
          'The camera turns on, but the video preview is choppy and delayed because it went through an unoptimized route.',
      },
      {
        id: 's4_wrong',
        type: 'WRONG',
        text: 'Attempt to take direct control of the camera wires without asking the operating system.',
        failureTitle: 'Security Block: Access Denied',
        failureDetails:
          "The phone's security guard blocked your app and shut it down because apps are never allowed to touch physical hardware wires directly.",
      },
    ],
  },
  {
    id: 5,
    stageNumber: 5,
    title: "Sharing the Phone's Brain (CPU)",
    story:
      "The camera is live, music is playing in the background, and the user is tapping the screen. All these tasks need the processor at the same time. How do you share it?",
    icon: 'hardware-chip-outline',
    accentColor: '#FF2D55',
    choices: [
      {
        id: 's5_good',
        type: 'GOOD',
        text: 'Give touch responses top priority, and take quick turns with background apps so nothing freezes.',
        feedback:
          'Butter smooth! Screen touches feel instant and background music keeps playing without a hitch.',
      },
      {
        id: 's5_risky',
        type: 'RISKY',
        text: 'Put your app into slow low-power mode to save battery.',
        feedback:
          'The app feels sluggish and delayed because other apps are getting all the processor time.',
      },
      {
        id: 's5_wrong',
        type: 'WRONG',
        text: 'Force your app to hog the processor 100 percent of the time and refuse to let anyone else use it.',
        failureTitle: 'System Crash: App Stopped Responding',
        failureDetails:
          'Your app froze the entire phone by refusing to share the processor. The operating system forced the app to close.',
      },
    ],
  },
];

function shuffleArray<T>(items: T[]): T[] {
  const result = [...items];
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

export const AppExecutionSimulator: React.FC = () => {
  const [currentStageIndex, setCurrentStageIndex] = useState(0);
  const [shuffledChoices, setShuffledChoices] = useState<StoryChoice[]>(() =>
    shuffleArray(STORY_STAGES[0].choices)
  );
  const [failedChoiceIds, setFailedChoiceIds] = useState<string[]>([]);
  const [mode, setMode] = useState<'QUESTION' | 'FAILURE' | 'OUTCOME' | 'FINISH'>('QUESTION');
  const [activeFailureChoice, setActiveFailureChoice] = useState<StoryChoice | null>(null);
  const [selectedOutcome, setSelectedOutcome] = useState<StoryChoice | null>(null);
  const [backtrackCount, setBacktrackCount] = useState(0);
  const [stageDecisions, setStageDecisions] = useState<
    Array<{ stageIndex: number; choice: StoryChoice }>
  >([]);

  // Smooth animation values
  const fadeAnim = useRef(new Animated.Value(1)).current;
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const currentStage = STORY_STAGES[currentStageIndex];

  const animateTransition = (callback: () => void) => {
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 0.1,
        duration: 120,
        useNativeDriver: true,
      }),
      Animated.timing(scaleAnim, {
        toValue: 0.98,
        duration: 120,
        useNativeDriver: true,
      }),
    ]).start(() => {
      callback();
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 200,
          useNativeDriver: true,
        }),
        Animated.timing(scaleAnim, {
          toValue: 1,
          duration: 200,
          useNativeDriver: true,
        }),
      ]).start();
    });
  };

  // Choice Selection
  const handleSelectChoice = (choice: StoryChoice) => {
    if (failedChoiceIds.includes(choice.id)) {
      return;
    }

    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(
          choice.type === 'WRONG'
            ? Haptics.ImpactFeedbackStyle.Heavy
            : Haptics.ImpactFeedbackStyle.Medium
        );
      } catch (e) {}
    }

    if (choice.type === 'WRONG') {
      setBacktrackCount((prev) => prev + 1);
      setFailedChoiceIds((prev) => [...prev, choice.id]);
      setActiveFailureChoice(choice);
      animateTransition(() => {
        setMode('FAILURE');
      });
    } else {
      setSelectedOutcome(choice);
      setStageDecisions((prev) => [
        ...prev.filter((d) => d.stageIndex !== currentStageIndex),
        { stageIndex: currentStageIndex, choice },
      ]);
      animateTransition(() => {
        setMode('OUTCOME');
      });
    }
  };

  // Backtrack from failure
  const handleBacktrack = () => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      } catch (e) {}
    }
    animateTransition(() => {
      setActiveFailureChoice(null);
      setMode('QUESTION');
    });
  };

  // Advance from outcome to next stage or finish
  const handleContinue = () => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      } catch (e) {}
    }

    if (currentStageIndex < STORY_STAGES.length - 1) {
      const nextIndex = currentStageIndex + 1;
      animateTransition(() => {
        setCurrentStageIndex(nextIndex);
        setShuffledChoices(shuffleArray(STORY_STAGES[nextIndex].choices));
        setFailedChoiceIds([]);
        setSelectedOutcome(null);
        setMode('QUESTION');
      });
    } else {
      animateTransition(() => {
        setMode('FINISH');
      });
    }
  };

  // Restart Entire Story Game
  const handleRestart = () => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      } catch (e) {}
    }
    animateTransition(() => {
      setCurrentStageIndex(0);
      setShuffledChoices(shuffleArray(STORY_STAGES[0].choices));
      setFailedChoiceIds([]);
      setMode('QUESTION');
      setActiveFailureChoice(null);
      setSelectedOutcome(null);
      setBacktrackCount(0);
      setStageDecisions([]);
    });
  };

  const optionLabels = ['Option A', 'Option B', 'Option C'];

  return (
    <View style={styles.container}>
      {/* Top Header */}
      <View style={styles.header}>
        <View style={styles.headerTitleRow}>
          <View style={styles.headerBadge}>
            <Ionicons name="play" size={16} color="#FFFFFF" />
          </View>
          <View>
            <Text style={styles.headerTitle}>APP EXECUTION STORY</Text>
            <Text style={styles.headerSubtitle}>INTERACTIVE JOURNEY FROM CODE TO HARDWARE</Text>
          </View>
        </View>

        <TouchableOpacity style={styles.restartBtn} onPress={handleRestart} activeOpacity={0.8}>
          <Ionicons name="refresh" size={13} color="#FFA07A" />
          <Text style={styles.restartBtnText}>RESTART</Text>
        </TouchableOpacity>
      </View>

      {/* 5-Stage Step Indicator */}
      <View style={styles.timelineCard}>
        <View style={styles.timelineHeaderRow}>
          <Text style={styles.timelineLabel}>STORY PROGRESSION</Text>
          <Text style={styles.backtrackCounterText}>
            Backtracks: {backtrackCount}
          </Text>
        </View>

        <View style={styles.stagePillsRow}>
          {STORY_STAGES.map((stg, idx) => {
            const isCompleted = idx < currentStageIndex || mode === 'FINISH';
            const isCurrent = idx === currentStageIndex && mode !== 'FINISH';

            return (
              <View
                key={stg.id}
                style={[
                  styles.stageStepPill,
                  isCurrent && {
                    borderColor: stg.accentColor,
                    backgroundColor: 'rgba(255, 255, 255, 0.08)',
                  },
                  isCompleted && styles.stageStepPillCompleted,
                ]}
              >
                <View
                  style={[
                    styles.stepNumberBadge,
                    isCompleted && styles.stepNumberBadgeCompleted,
                    isCurrent && { backgroundColor: stg.accentColor },
                  ]}
                >
                  {isCompleted ? (
                    <Ionicons name="checkmark" size={11} color="#FFFFFF" />
                  ) : (
                    <Text style={styles.stepNumberText}>{stg.stageNumber}</Text>
                  )}
                </View>
                <Text
                  style={[
                    styles.stageStepLabel,
                    isCurrent && { color: '#FFFFFF', fontWeight: '700' },
                  ]}
                  numberOfLines={1}
                >
                  Stage {stg.stageNumber}
                </Text>
              </View>
            );
          })}
        </View>
      </View>

      {/* Main Animated Content Box */}
      <Animated.View
        style={[
          styles.mainCard,
          {
            opacity: fadeAnim,
            transform: [{ scale: scaleAnim }],
          },
        ]}
      >
        {/* ================================================================ */}
        {/* VIEW 1: QUESTION MODE                                            */}
        {/* ================================================================ */}
        {mode === 'QUESTION' && (
          <View style={styles.stageContent}>
            {/* Stage Title Banner */}
            <View style={styles.stageHeaderRow}>
              <View style={[styles.stageIconBox, { backgroundColor: currentStage.accentColor }]}>
                <Ionicons name={currentStage.icon} size={18} color="#FFFFFF" />
              </View>
              <View style={styles.stageHeaderTextCol}>
                <Text style={styles.stageCategoryTag}>
                  STAGE {currentStage.stageNumber} OF 5
                </Text>
                <Text style={styles.stageTitleText}>{currentStage.title}</Text>
              </View>
            </View>

            {/* Story Card */}
            <View style={styles.storyCard}>
              <Text style={styles.storyCardLabel}>THE CHALLENGE</Text>
              <Text style={styles.storyText}>{currentStage.story}</Text>
            </View>

            <Text style={styles.instructionPrompt}>
              Choose how your app should handle this situation:
            </Text>

            {/* Randomized Choices */}
            <View style={styles.choicesList}>
              {shuffledChoices.map((choice, index) => {
                const isFailed = failedChoiceIds.includes(choice.id);

                return (
                  <TouchableOpacity
                    key={choice.id}
                    style={[
                      styles.choiceCard,
                      isFailed && styles.choiceCardFailed,
                    ]}
                    onPress={() => handleSelectChoice(choice)}
                    activeOpacity={isFailed ? 1 : 0.75}
                  >
                    <View style={styles.choiceTopRow}>
                      <View
                        style={[
                          styles.optionPill,
                          isFailed && styles.optionPillFailed,
                        ]}
                      >
                        <Text
                          style={[
                            styles.optionPillText,
                            isFailed && { color: '#FF453A' },
                          ]}
                        >
                          {optionLabels[index]}
                        </Text>
                      </View>

                      {isFailed ? (
                        <View style={styles.failedStatusBadge}>
                          <Ionicons name="close" size={13} color="#FF453A" />
                          <Text style={styles.failedStatusText}>Crashed the app</Text>
                        </View>
                      ) : (
                        <Ionicons
                          name="chevron-forward-circle-outline"
                          size={20}
                          color="rgba(255, 255, 255, 0.4)"
                        />
                      )}
                    </View>

                    <Text
                      style={[
                        styles.choiceText,
                        isFailed && styles.choiceTextFailed,
                      ]}
                    >
                      {choice.text}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          </View>
        )}

        {/* ================================================================ */}
        {/* VIEW 2: FAILURE SCREEN (MISTAKE / CRASH)                         */}
        {/* ================================================================ */}
        {mode === 'FAILURE' && activeFailureChoice && (
          <View style={styles.failureContainer}>
            <View style={styles.failureCard}>
              <View style={styles.failureIconWrap}>
                <Ionicons name="alert-circle-outline" size={38} color="#FF3B30" />
              </View>

              <View style={styles.failureTextWrap}>
                <View style={styles.failureAlertBadge}>
                  <Text style={styles.failureAlertText}>PROGRAM HALTED</Text>
                </View>

                <Text style={styles.failureTitleText}>
                  {activeFailureChoice.failureTitle}
                </Text>

                <Text style={styles.failureDetailsText}>
                  {activeFailureChoice.failureDetails}
                </Text>
              </View>

              {/* Single Backtrack Button as required */}
              <TouchableOpacity
                style={styles.backtrackButton}
                onPress={handleBacktrack}
                activeOpacity={0.85}
              >
                <Ionicons name="arrow-back" size={18} color="#FFFFFF" />
                <Text style={styles.backtrackButtonText}>
                  Backtrack & Choose Another Option
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        )}

        {/* ================================================================ */}
        {/* VIEW 3: OUTCOME SCREEN (SUCCESS OR RISKY WARNING)                */}
        {/* ================================================================ */}
        {mode === 'OUTCOME' && selectedOutcome && (
          <View style={styles.outcomeContainer}>
            <View
              style={[
                styles.outcomeCard,
                selectedOutcome.type === 'GOOD'
                  ? styles.outcomeCardGood
                  : styles.outcomeCardRisky,
              ]}
            >
              <View style={styles.outcomeHeaderRow}>
                <View
                  style={[
                    styles.outcomeIconWrap,
                    {
                      backgroundColor:
                        selectedOutcome.type === 'GOOD'
                          ? 'rgba(52, 199, 89, 0.2)'
                          : 'rgba(245, 158, 11, 0.2)',
                    },
                  ]}
                >
                  <Ionicons
                    name={
                      selectedOutcome.type === 'GOOD'
                        ? 'checkmark-circle-outline'
                        : 'warning-outline'
                    }
                    size={28}
                    color={selectedOutcome.type === 'GOOD' ? '#34C759' : '#F59E0B'}
                  />
                </View>

                <View style={{ flex: 1 }}>
                  <View
                    style={[
                      styles.outcomeTypePill,
                      {
                        backgroundColor:
                          selectedOutcome.type === 'GOOD' ? '#34C759' : '#F59E0B',
                      },
                    ]}
                  >
                    <Text style={styles.outcomeTypePillText}>
                      {selectedOutcome.type === 'GOOD' ? 'OPTIMAL PATH' : 'SUB-OPTIMAL PATH'}
                    </Text>
                  </View>
                  <Text style={styles.outcomeTitleText}>
                    {selectedOutcome.type === 'GOOD' ? 'Execution Succeeded' : 'Notice: Potential Lag'}
                  </Text>
                </View>
              </View>

              <View style={styles.outcomeTextBox}>
                <Text style={styles.outcomeFeedbackText}>
                  {selectedOutcome.feedback}
                </Text>
              </View>

              <TouchableOpacity
                style={[
                  styles.continueButton,
                  {
                    backgroundColor:
                      selectedOutcome.type === 'GOOD' ? '#34C759' : '#DD5622',
                  },
                ]}
                onPress={handleContinue}
                activeOpacity={0.85}
              >
                <Text style={styles.continueButtonText}>
                  {currentStageIndex === 4
                    ? 'View Final App Report'
                    : selectedOutcome.type === 'GOOD'
                    ? 'Continue to Next Stage'
                    : 'Continue with Warning'}
                </Text>
                <Ionicons name="arrow-forward" size={16} color="#FFFFFF" />
              </TouchableOpacity>
            </View>
          </View>
        )}

        {/* ================================================================ */}
        {/* VIEW 4: FINISH SCREEN (SCORECARD & RESTART)                      */}
        {/* ================================================================ */}
        {mode === 'FINISH' && (
          <View style={styles.finishContainer}>
            {/* Scorecard Header */}
            <View style={styles.scorecardHero}>
              <View style={styles.trophyIconWrap}>
                <Ionicons name="ribbon-outline" size={36} color="#FFA07A" />
              </View>

              {backtrackCount === 0 && (
                <View style={styles.scorecardBadge}>
                  <Text style={styles.scorecardBadgeText}>ZERO BACKTRACKS</Text>
                </View>
              )}

              {backtrackCount >= 1 && backtrackCount <= 2 && (
                <View style={[styles.scorecardBadge, { backgroundColor: '#007AFF' }]}>
                  <Text style={styles.scorecardBadgeText}>
                    {backtrackCount} BACKTRACK{backtrackCount > 1 ? 'S' : ''}
                  </Text>
                </View>
              )}

              {backtrackCount >= 3 && (
                <View style={[styles.scorecardBadge, { backgroundColor: '#F59E0B' }]}>
                  <Text style={styles.scorecardBadgeText}>
                    {backtrackCount} BACKTRACKS
                  </Text>
                </View>
              )}

              <Text style={styles.scorecardTitle}>
                {backtrackCount === 0 && 'Master Builder'}
                {backtrackCount >= 1 && backtrackCount <= 2 && 'Skilled Fixer'}
                {backtrackCount >= 3 && 'App Survivor'}
              </Text>

              <Text style={styles.scorecardSubtitle}>
                {backtrackCount === 0 &&
                  'Your app ran perfectly on the first try without any crashes.'}
                {backtrackCount >= 1 &&
                  backtrackCount <= 2 &&
                  'You ran into minor issues, but successfully resolved them.'}
                {backtrackCount >= 3 &&
                  'You crashed the phone multiple times, but learned how to bring it back to life.'}
              </Text>
            </View>

            {/* Stage Path Breakdown */}
            <View style={styles.recapList}>
              <Text style={styles.recapHeaderLabel}>YOUR APP EXECUTION ROUTE:</Text>
              {STORY_STAGES.map((stg) => {
                const decision = stageDecisions.find((d) => d.stageIndex === stg.id - 1);
                const isGood = decision?.choice.type === 'GOOD';

                return (
                  <View key={stg.id} style={styles.recapItem}>
                    <View
                      style={[
                        styles.recapDot,
                        { backgroundColor: isGood ? '#34C759' : '#F59E0B' },
                      ]}
                    />
                    <View style={styles.recapTextWrap}>
                      <Text style={styles.recapStageTitle}>
                        {stg.stageNumber}. {stg.title}
                      </Text>
                      <Text style={styles.recapChoiceText} numberOfLines={2}>
                        {decision ? decision.choice.text : 'Completed'}
                      </Text>
                    </View>
                    <View
                      style={[
                        styles.recapBadge,
                        {
                          backgroundColor: isGood
                            ? 'rgba(52, 199, 89, 0.15)'
                            : 'rgba(245, 158, 11, 0.15)',
                        },
                      ]}
                    >
                      <Text
                        style={[
                          styles.recapBadgeText,
                          { color: isGood ? '#34C759' : '#F59E0B' },
                        ]}
                      >
                        {isGood ? 'Optimal' : 'Risky'}
                      </Text>
                    </View>
                  </View>
                );
              })}
            </View>

            {/* Restart Button as required */}
            <TouchableOpacity
              style={styles.restartMainBtn}
              onPress={handleRestart}
              activeOpacity={0.85}
            >
              <Ionicons name="refresh" size={18} color="#FFFFFF" />
              <Text style={styles.restartMainBtnText}>Try a Different Path</Text>
            </TouchableOpacity>
          </View>
        )}
      </Animated.View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#12121E',
    borderRadius: 22,
    padding: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 14,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  headerBadge: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: Colors.studioOrange,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitle: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 0.8,
  },
  headerSubtitle: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 9,
    marginTop: 2,
    fontWeight: '600',
  },
  restartBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 14,
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  restartBtnText: {
    fontFamily: FontFamilies.sans,
    color: '#FFA07A',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.5,
  },

  /* Timeline */
  timelineCard: {
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: 14,
    padding: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
    gap: 8,
  },
  timelineHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  timelineLabel: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: 9,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  backtrackCounterText: {
    fontFamily: FontFamilies.mono,
    color: '#FFA07A',
    fontSize: 10,
    fontWeight: '600',
  },
  stagePillsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 6,
  },
  stageStepPill: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingVertical: 6,
    paddingHorizontal: 8,
    borderRadius: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderWidth: 1,
    borderColor: 'transparent',
  },
  stageStepPillCompleted: {
    backgroundColor: 'rgba(52, 199, 89, 0.08)',
    borderColor: 'rgba(52, 199, 89, 0.3)',
  },
  stepNumberBadge: {
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  stepNumberBadgeCompleted: {
    backgroundColor: '#34C759',
  },
  stepNumberText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '700',
  },
  stageStepLabel: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 11,
    fontWeight: '600',
  },

  /* Main Card */
  mainCard: {
    backgroundColor: '#161626',
    borderRadius: 18,
    padding: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },

  /* Question Stage */
  stageContent: {
    gap: 14,
  },
  stageHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  stageIconBox: {
    width: 38,
    height: 38,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  stageHeaderTextCol: {
    flex: 1,
    gap: 2,
  },
  stageCategoryTag: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: 9,
    fontWeight: '700',
    letterSpacing: 0.6,
  },
  stageTitleText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '700',
  },
  storyCard: {
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 14,
    padding: 14,
    borderLeftWidth: 3,
    borderLeftColor: Colors.studioOrange,
    gap: 4,
  },
  storyCardLabel: {
    fontFamily: FontFamilies.sans,
    color: Colors.studioOrange,
    fontSize: 9,
    fontWeight: '700',
    letterSpacing: 0.6,
  },
  storyText: {
    fontFamily: FontFamilies.sans,
    color: '#E5E7EB',
    fontSize: 13.5,
    lineHeight: 20,
    fontWeight: '400',
  },
  instructionPrompt: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.7)',
    fontSize: 12,
    fontWeight: '600',
  },
  choicesList: {
    gap: 10,
  },
  choiceCard: {
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderRadius: 14,
    padding: 14,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 8,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  choiceCardFailed: {
    borderColor: '#EF4444',
    backgroundColor: 'rgba(239, 68, 68, 0.08)',
  },
  choiceTopRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  optionPill: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
  },
  optionPillFailed: {
    backgroundColor: 'rgba(239, 68, 68, 0.2)',
  },
  optionPillText: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 0.4,
  },
  failedStatusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  failedStatusText: {
    fontFamily: FontFamilies.sans,
    color: '#FF453A',
    fontSize: 11,
    fontWeight: '600',
  },
  choiceText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '500',
  },
  choiceTextFailed: {
    color: 'rgba(255, 255, 255, 0.5)',
    textDecorationLine: 'line-through',
  },

  /* Failure Screen */
  failureContainer: {
    paddingVertical: 8,
  },
  failureCard: {
    backgroundColor: 'rgba(239, 68, 68, 0.08)',
    borderRadius: 16,
    padding: 18,
    borderWidth: 1.5,
    borderColor: '#EF4444',
    alignItems: 'center',
    gap: 14,
  },
  failureIconWrap: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: 'rgba(239, 68, 68, 0.15)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  failureTextWrap: {
    alignItems: 'center',
    gap: 8,
  },
  failureAlertBadge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 10,
    backgroundColor: 'rgba(239, 68, 68, 0.25)',
  },
  failureAlertText: {
    fontFamily: FontFamilies.sans,
    color: '#FF453A',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.8,
  },
  failureTitleText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '700',
    textAlign: 'center',
  },
  failureDetailsText: {
    fontFamily: FontFamilies.sans,
    color: '#E5E7EB',
    fontSize: 13,
    lineHeight: 19,
    textAlign: 'center',
    fontWeight: '400',
  },
  backtrackButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#EF4444',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 14,
    marginTop: 4,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  backtrackButtonText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
  },

  /* Outcome Screen */
  outcomeContainer: {
    paddingVertical: 8,
  },
  outcomeCard: {
    borderRadius: 16,
    padding: 18,
    borderWidth: 1.5,
    gap: 14,
  },
  outcomeCardGood: {
    backgroundColor: 'rgba(52, 199, 89, 0.08)',
    borderColor: '#34C759',
  },
  outcomeCardRisky: {
    backgroundColor: 'rgba(245, 158, 11, 0.08)',
    borderColor: '#F59E0B',
  },
  outcomeHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  outcomeIconWrap: {
    width: 46,
    height: 46,
    borderRadius: 23,
    justifyContent: 'center',
    alignItems: 'center',
  },
  outcomeTypePill: {
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8,
    marginBottom: 3,
  },
  outcomeTypePillText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  outcomeTitleText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  outcomeTextBox: {
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
    borderRadius: 12,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
  },
  outcomeFeedbackText: {
    fontFamily: FontFamilies.sans,
    color: '#E5E7EB',
    fontSize: 13.5,
    lineHeight: 20,
    fontWeight: '400',
  },
  continueButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 14,
    marginTop: 2,
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  continueButtonText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '700',
  },

  /* Finish Screen */
  finishContainer: {
    alignItems: 'center',
    gap: 16,
    paddingVertical: 6,
  },
  scorecardHero: {
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 10,
  },
  trophyIconWrap: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: 'rgba(221, 86, 34, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: 'rgba(221, 86, 34, 0.4)',
  },
  scorecardBadge: {
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
    backgroundColor: '#34C759',
  },
  scorecardBadgeText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.8,
  },
  scorecardTitle: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 22,
    fontWeight: '800',
    textAlign: 'center',
  },
  scorecardSubtitle: {
    fontFamily: FontFamilies.sans,
    color: '#D1D5DB',
    fontSize: 13,
    lineHeight: 19,
    textAlign: 'center',
    fontWeight: '400',
  },
  recapList: {
    width: '100%',
    backgroundColor: 'rgba(0, 0, 0, 0.35)',
    borderRadius: 16,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 10,
  },
  recapHeaderLabel: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.5)',
    fontSize: 9,
    fontWeight: '700',
    letterSpacing: 0.6,
  },
  recapItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 6,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.05)',
  },
  recapDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  recapTextWrap: {
    flex: 1,
    gap: 2,
  },
  recapStageTitle: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },
  recapChoiceText: {
    fontFamily: FontFamilies.sans,
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 11,
    lineHeight: 15,
  },
  recapBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 8,
  },
  recapBadgeText: {
    fontFamily: FontFamilies.sans,
    fontSize: 10,
    fontWeight: '700',
  },
  restartMainBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: Colors.studioOrange,
    paddingVertical: 13,
    paddingHorizontal: 26,
    borderRadius: 16,
    width: '100%',
    ...(Platform.OS === 'web' ? ({ cursor: 'pointer' } as any) : {}),
  },
  restartMainBtnText: {
    fontFamily: FontFamilies.sans,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 0.4,
  },
});
