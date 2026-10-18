import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Platform,
  Dimensions,
  PixelRatio,
  TouchableOpacity,
  Animated,
} from 'react-native';
import { Colors } from '../theme/colors';
import { Typography } from '../theme/typography';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

interface LiveDeviceStats {
  deviceModel: string;
  deviceBrand: string;
  osName: string;
  osVersion: string;
  platformArch: string;
  cpuCores: number | string;
  screenResolution: string;
  pixelRatio: string;
  windowSize: string;
  colorDepth: string;
  schedulingAlgorithm: string;
}

export const MyDeviceInspector: React.FC = () => {
  const [stats, setStats] = useState<LiveDeviceStats>({
    deviceModel: 'Detecting...',
    deviceBrand: 'Detecting...',
    osName: 'Detecting...',
    osVersion: '',
    platformArch: 'Detecting...',
    cpuCores: 'Detecting...',
    screenResolution: 'Detecting...',
    pixelRatio: 'Detecting...',
    windowSize: 'Detecting...',
    colorDepth: 'Detecting...',
    schedulingAlgorithm: 'Linux CFS / Apple GCD',
  });

  // Playground Slide Simulation state
  const [currentAppOnSlide, setCurrentAppOnSlide] = useState(0);
  const [isSlideAutoPlaying, setIsSlideAutoPlaying] = useState(false);
  const slideAnim = useRef(new Animated.Value(0)).current;

  const PLAYGROUND_APPS = [
    { name: 'Music Player 🎵', priority: 'High Priority (Audio)', timeSlice: '15 ms', color: '#34C759' },
    { name: 'Action Game 🎮', priority: 'Top Priority (Screen)', timeSlice: '16 ms', color: '#FF9500' },
    { name: 'Chat App 💬', priority: 'Normal (Waiting for Msg)', timeSlice: '10 ms', color: '#007AFF' },
    { name: 'Cloud Backup ☁️', priority: 'Low Priority (Background)', timeSlice: '5 ms', color: '#AF52DE' },
  ];

  useEffect(() => {
    // Collect live environment telemetry
    const screen = Dimensions.get('screen');
    const windowDim = Dimensions.get('window');
    const pr = PixelRatio.get();

    let brand = 'Generic Device';
    let model = 'Universal System';
    let os = Platform.OS.toUpperCase();
    let version = String(Platform.Version || '1.0');
    let arch = 'ARM64 / x86_64';
    let cores: number | string = 8;
    let schedAlgo = 'Completely Fair Scheduler (CFS)';
    let depth = '24-bit TrueColor';

    if (Platform.OS === 'web' && typeof window !== 'undefined' && typeof navigator !== 'undefined') {
      const ua = navigator.userAgent;

      // Extract OS and Brand
      if (/Windows/i.test(ua)) {
        brand = 'Microsoft PC';
        os = 'Windows';
        schedAlgo = 'Windows Multithreaded Priority Preemptive Scheduler';
        if (/Win64|x64|WOW64/i.test(ua)) arch = 'x86_64 (64-bit)';
      } else if (/Macintosh|Mac OS X/i.test(ua)) {
        brand = 'Apple Mac';
        os = 'macOS';
        schedAlgo = 'Apple Grand Central Dispatch (GCD) + Mach Thread Scheduler';
        arch = 'ARM64 Apple Silicon';
      } else if (/Android/i.test(ua)) {
        brand = 'Android Device';
        os = 'Android (Linux Kernel)';
        schedAlgo = 'Linux Completely Fair Scheduler (CFS) + EAS (Energy Aware)';
        arch = 'ARMv9 / ARMv8 64-bit';
      } else if (/iPhone|iPad|iPod/i.test(ua)) {
        brand = 'Apple iOS';
        os = 'iOS';
        schedAlgo = 'iOS Grand Central Dispatch (GCD) + XNU Mach Scheduler';
        arch = 'Apple Bionic / Silicon ARM64';
      } else if (/Linux/i.test(ua)) {
        brand = 'Linux Machine';
        os = 'GNU/Linux';
        schedAlgo = 'Linux Completely Fair Scheduler (CFS / EEVDF)';
      }

      // Detect Model
      if (/Chrome/i.test(ua)) {
        model = 'Chrome Web Environment';
      } else if (/Safari/i.test(ua)) {
        model = 'Safari Web Environment';
      } else if (/Firefox/i.test(ua)) {
        model = 'Firefox Web Environment';
      } else if (/Edge/i.test(ua)) {
        model = 'Edge Web Environment';
      }

      // Hardware Concurrency (Cores)
      if (navigator.hardwareConcurrency) {
        cores = navigator.hardwareConcurrency;
      }

      // Color Depth
      if (window.screen && window.screen.colorDepth) {
        depth = `${window.screen.colorDepth}-bit`;
      }
    }

    const screenRes = `${Math.round(screen.width * pr)} × ${Math.round(screen.height * pr)} px`;
    const windowRes = `${Math.round(windowDim.width)} × ${Math.round(windowDim.height)} pt`;

    setStats({
      deviceBrand: brand,
      deviceModel: model,
      osName: os,
      osVersion: version,
      platformArch: arch,
      cpuCores: cores,
      screenResolution: screenRes,
      pixelRatio: `${pr.toFixed(1)}x`,
      windowSize: windowRes,
      colorDepth: depth,
      schedulingAlgorithm: schedAlgo,
    });
  }, []);

  // Slide Animation Loop
  const animateSlide = (nextIndex: number) => {
    slideAnim.setValue(0);
    Animated.timing(slideAnim, {
      toValue: 1,
      duration: 650,
      useNativeDriver: true,
    }).start();

    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      } catch (e) {}
    }

    setCurrentAppOnSlide(nextIndex);
  };

  const nextTurn = () => {
    animateSlide((currentAppOnSlide + 1) % PLAYGROUND_APPS.length);
  };

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isSlideAutoPlaying) {
      interval = setInterval(() => {
        setCurrentAppOnSlide((prev) => {
          const next = (prev + 1) % PLAYGROUND_APPS.length;
          slideAnim.setValue(0);
          Animated.timing(slideAnim, {
            toValue: 1,
            duration: 600,
            useNativeDriver: true,
          }).start();
          return next;
        });
      }, 2000);
    }
    return () => clearInterval(interval);
  }, [isSlideAutoPlaying]);

  const activeSlideApp = PLAYGROUND_APPS[currentAppOnSlide];

  return (
    <View style={styles.container}>
      {/* Top Banner */}
      <View style={styles.topCard}>
        <View style={styles.titleRow}>
          <View style={styles.radarIconBox}>
            <Ionicons name="speedometer" size={20} color="#FFFFFF" />
          </View>
          <View>
            <Text style={styles.cardTitle}>MY DEVICE OS INSPECTOR</Text>
            <Text style={styles.cardSubtitle}>LIVE REAL-TIME HARDWARE & SYSTEM TELEMETRY</Text>
          </View>
        </View>

        <View style={styles.liveIndicatorRow}>
          <View style={styles.pulsingGreenDot} />
          <Text style={styles.liveText}>LIVE SENSORS ACTIVE</Text>
        </View>
      </View>

      {/* Real-Time Hardware & OS Grid */}
      <View style={styles.statsGrid}>
        {/* Device Brand & Model */}
        <View style={styles.statTile}>
          <View style={styles.tileHeader}>
            <Ionicons name="phone-portrait-outline" size={16} color={Colors.studioOrange} />
            <Text style={styles.tileLabel}>DEVICE & BRAND</Text>
          </View>
          <Text style={styles.tileValue}>{stats.deviceBrand}</Text>
          <Text style={styles.tileSub}>{stats.deviceModel}</Text>
        </View>

        {/* Operating System & Version */}
        <View style={styles.statTile}>
          <View style={styles.tileHeader}>
            <Ionicons name="logo-windows" size={16} color="#007AFF" />
            <Text style={styles.tileLabel}>OPERATING SYSTEM</Text>
          </View>
          <Text style={styles.tileValue}>{stats.osName}</Text>
          <Text style={styles.tileSub}>Version: {stats.osVersion || 'Current Active'}</Text>
        </View>

        {/* Platform Architecture / Cores */}
        <View style={styles.statTile}>
          <View style={styles.tileHeader}>
            <Ionicons name="hardware-chip-outline" size={16} color="#FF9500" />
            <Text style={styles.tileLabel}>CPU ARCH & CORES</Text>
          </View>
          <Text style={styles.tileValue}>{stats.cpuCores} Logical Cores</Text>
          <Text style={styles.tileSub}>{stats.platformArch}</Text>
        </View>

        {/* Screen Resolution & Pixel Ratio */}
        <View style={styles.statTile}>
          <View style={styles.tileHeader}>
            <Ionicons name="scan-outline" size={16} color="#34C759" />
            <Text style={styles.tileLabel}>DISPLAY RESOLUTION</Text>
          </View>
          <Text style={styles.tileValue}>{stats.screenResolution}</Text>
          <Text style={styles.tileSub}>Density: {stats.pixelRatio} • {stats.colorDepth}</Text>
        </View>
      </View>

      {/* CPU Scheduling Algorithm Section */}
      <View style={styles.schedulerSection}>
        <View style={styles.schedulerHeader}>
          <View style={styles.slideIconWrap}>
            <Ionicons name="happy" size={20} color="#FFFFFF" />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.schedulerMainTitle}>CPU SCHEDULING ALGORITHM</Text>
            <Text style={styles.schedulerSubtitle}>
              Active: {stats.schedulingAlgorithm}
            </Text>
          </View>
        </View>

        {/* The Kid-Friendly Playground Slide Analogy */}
        <View style={styles.analogyCard}>
          <View style={styles.analogyTitleRow}>
            <Text style={styles.analogyHeading}>🎡 The Playground Slide Rule</Text>
            <Text style={styles.analogyTag}>EASY EXPLANATION</Text>
          </View>

          <Text style={styles.analogyParagraph}>
            Your phone has millions of things to calculate, but only a few CPU Brain Cores.
            How does it decide which app gets to use the CPU?
          </Text>

          <View style={styles.slideRulesList}>
            <View style={styles.ruleItem}>
              <View style={[styles.ruleBullet, { backgroundColor: '#34C759' }]}>
                <Text style={styles.ruleBulletNum}>1</Text>
              </View>
              <View style={styles.ruleTextCol}>
                <Text style={styles.ruleTitle}>The Playground Slide (CPU Core)</Text>
                <Text style={styles.ruleDesc}>
                  There is only one slide, but lots of kids (apps like YouTube, Games, and Messages) want to play.
                </Text>
              </View>
            </View>

            <View style={styles.ruleItem}>
              <View style={[styles.ruleBullet, { backgroundColor: '#007AFF' }]}>
                <Text style={styles.ruleBulletNum}>2</Text>
              </View>
              <View style={styles.ruleTextCol}>
                <Text style={styles.ruleTitle}>The Fair Teacher (Linux CFS / iOS GCD)</Text>
                <Text style={styles.ruleDesc}>
                  A super-fair teacher holds a stopwatch. Each app gets a quick 10-millisecond slide so nobody is left waiting!
                </Text>
              </View>
            </View>

            <View style={styles.ruleItem}>
              <View style={[styles.ruleBullet, { backgroundColor: '#FF9500' }]}>
                <Text style={styles.ruleBulletNum}>3</Text>
              </View>
              <View style={styles.ruleTextCol}>
                <Text style={styles.ruleTitle}>VIP Fast Pass (Priority)</Text>
                <Text style={styles.ruleDesc}>
                  If you are listening to music, the teacher says: "You go first so the song never stutters!"
                </Text>
              </View>
            </View>
          </View>
        </View>

        {/* Interactive Playground Slide Simulation */}
        <View style={styles.slideSimBox}>
          <View style={styles.simControlsHeader}>
            <Text style={styles.simControlsTitle}>WATCH APPS TAKE TURNS ON THE CPU SLIDE:</Text>
            <TouchableOpacity
              style={[styles.simAutoBtn, isSlideAutoPlaying && styles.simAutoBtnActive]}
              onPress={() => setIsSlideAutoPlaying(!isSlideAutoPlaying)}
              activeOpacity={0.8}
            >
              <Ionicons
                name={isSlideAutoPlaying ? 'pause' : 'play'}
                size={12}
                color={isSlideAutoPlaying ? '#FFFFFF' : '#FFA07A'}
              />
              <Text style={[styles.simAutoBtnText, isSlideAutoPlaying && { color: '#FFFFFF' }]}>
                {isSlideAutoPlaying ? 'PAUSE' : 'AUTO TURNS'}
              </Text>
            </TouchableOpacity>
          </View>

          {/* The Visual Slide */}
          <View style={styles.slideTrack}>
            <Animated.View
              style={[
                styles.slidingAppBadge,
                {
                  backgroundColor: activeSlideApp.color,
                  transform: [
                    {
                      translateX: slideAnim.interpolate({
                        inputRange: [0, 0.5, 1],
                        outputRange: [-60, 0, 60],
                      }),
                    },
                    {
                      scale: slideAnim.interpolate({
                        inputRange: [0, 0.5, 1],
                        outputRange: [0.85, 1.1, 1],
                      }),
                    },
                  ],
                },
              ]}
            >
              <Text style={styles.slidingAppText}>{activeSlideApp.name}</Text>
            </Animated.View>
          </View>

          {/* Current App Info */}
          <View style={styles.currentSlideInfo}>
            <View style={styles.slideInfoRow}>
              <Text style={styles.slideInfoLabel}>Currently using CPU Slide:</Text>
              <Text style={[styles.slideInfoVal, { color: activeSlideApp.color }]}>
                {activeSlideApp.name}
              </Text>
            </View>
            <View style={styles.slideInfoRow}>
              <Text style={styles.slideInfoLabel}>Assigned Time Slice:</Text>
              <Text style={styles.slideInfoVal}>{activeSlideApp.timeSlice}</Text>
            </View>
            <View style={styles.slideInfoRow}>
              <Text style={styles.slideInfoLabel}>Schedule Policy:</Text>
              <Text style={styles.slideInfoVal}>{activeSlideApp.priority}</Text>
            </View>
          </View>

          <TouchableOpacity style={styles.nextTurnButton} onPress={nextTurn} activeOpacity={0.8}>
            <Text style={styles.nextTurnButtonText}>GIVE NEXT APP A TURN ➔</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    gap: 14,
  },
  topCard: {
    backgroundColor: '#161624',
    borderRadius: 20,
    padding: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  radarIconBox: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: '#DD5622',
    justifyContent: 'center',
    alignItems: 'center',
  },
  cardTitle: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 1,
  },
  cardSubtitle: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.65)',
    fontSize: 9,
    marginTop: 2,
  },
  liveIndicatorRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(52, 199, 89, 0.15)',
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: 12,
  },
  pulsingGreenDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#34C759',
  },
  liveText: {
    ...Typography.tag,
    color: '#34C759',
    fontSize: 9,
    fontWeight: '800',
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  statTile: {
    flex: 1,
    minWidth: '47%',
    backgroundColor: '#13131E',
    borderRadius: 16,
    padding: 14,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.07)',
    gap: 4,
  },
  tileHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  tileLabel: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.55)',
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  tileValue: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '800',
    marginTop: 2,
  },
  tileSub: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.7)',
    fontSize: 11,
  },
  schedulerSection: {
    backgroundColor: '#141422',
    borderRadius: 20,
    padding: 16,
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    gap: 14,
  },
  schedulerHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  slideIconWrap: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#FF9500',
    justifyContent: 'center',
    alignItems: 'center',
  },
  schedulerMainTitle: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  schedulerSubtitle: {
    ...Typography.tag,
    color: '#FFA07A',
    fontSize: 10,
    fontWeight: '600',
  },
  analogyCard: {
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderRadius: 16,
    padding: 14,
    gap: 10,
  },
  analogyTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  analogyHeading: {
    ...Typography.headline,
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '800',
  },
  analogyTag: {
    ...Typography.tag,
    color: '#FFD700',
    backgroundColor: 'rgba(255, 215, 0, 0.15)',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 10,
    fontSize: 9,
    fontWeight: '800',
  },
  analogyParagraph: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.85)',
    fontSize: 12,
    lineHeight: 18,
  },
  slideRulesList: {
    gap: 10,
    marginTop: 4,
  },
  ruleItem: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 10,
  },
  ruleBullet: {
    width: 22,
    height: 22,
    borderRadius: 11,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 2,
  },
  ruleBulletNum: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
  },
  ruleTextCol: {
    flex: 1,
    gap: 2,
  },
  ruleTitle: {
    ...Typography.bodySmall,
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 12,
  },
  ruleDesc: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.7)',
    fontSize: 11,
    lineHeight: 16,
  },
  slideSimBox: {
    backgroundColor: '#0C0C14',
    borderRadius: 16,
    padding: 14,
    gap: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
  },
  simControlsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  simControlsTitle: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 9,
    fontWeight: '800',
  },
  simAutoBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: 'rgba(221, 86, 34, 0.15)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'rgba(221, 86, 34, 0.3)',
  },
  simAutoBtnActive: {
    backgroundColor: Colors.studioOrange,
    borderColor: Colors.studioOrange,
  },
  simAutoBtnText: {
    ...Typography.tag,
    color: '#FFA07A',
    fontSize: 9,
    fontWeight: '800',
  },
  slideTrack: {
    height: 56,
    backgroundColor: '#19192A',
    borderRadius: 28,
    borderWidth: 2,
    borderColor: 'rgba(255, 255, 255, 0.15)',
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'hidden',
  },
  slidingAppBadge: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.4,
    shadowRadius: 4,
    elevation: 4,
  },
  slidingAppText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '800',
  },
  currentSlideInfo: {
    backgroundColor: '#141420',
    borderRadius: 12,
    padding: 10,
    gap: 4,
  },
  slideInfoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  slideInfoLabel: {
    ...Typography.tag,
    color: 'rgba(255, 255, 255, 0.55)',
    fontSize: 11,
  },
  slideInfoVal: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '700',
  },
  nextTurnButton: {
    backgroundColor: Colors.studioOrange,
    paddingVertical: 11,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  nextTurnButtonText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
});
