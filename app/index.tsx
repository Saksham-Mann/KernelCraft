import React, { useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Animated,
  TouchableOpacity,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { Asset } from 'expo-asset';
import { Image } from 'expo-image';
import { TEARDOWN_FRAME_ASSETS } from '../src/data/teardownData';
import Svg, {
  Defs,
  RadialGradient,
  Stop,
  Rect,
  Circle,
  Line,
} from 'react-native-svg';
import { FontFamilies } from '../src/theme/typography';

/**
 * KernelCraft Initial Splash & Loading Screen
 *
 * Implements:
 * 1. Rich warm gradient: radial-gradient(circle at 50% 35%, #F97316 0%, #EA580C 55%, #C2410C 100%)
 * 2. Frosted glass container (76px x 76px, blur(12px), rounded 22px) with white geometric silicon die SVG
 * 3. Clean Title Case "KernelCraft" (700, 32px, tracking -0.02em) and "From Silicon to Syscall" (400, 15px)
 * 4. Sleek 40px wide horizontal progress pill loading feedback
 * 5. Removal of outdated version badges and manual tap prompts
 */
export default function SplashScreen() {
  const router = useRouter();
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const scaleAnim = useRef(new Animated.Value(0.94)).current;
  const progressAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    let isMounted = true;

    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 650,
        useNativeDriver: true,
      }),
      Animated.spring(scaleAnim, {
        toValue: 1,
        friction: 7,
        tension: 40,
        useNativeDriver: true,
      }),
      Animated.timing(progressAnim, {
        toValue: 1,
        duration: 1800,
        useNativeDriver: false,
      }),
    ]).start();

    const preloadPromise = (async () => {
      try {
        const loadedAssets = await Asset.loadAsync(TEARDOWN_FRAME_ASSETS);
        const uris = loadedAssets
          .map((a) => a.localUri || a.uri)
          .filter((u): u is string => Boolean(u));
        if (uris.length > 0) {
          await Image.prefetch(uris, 'memory-disk');
        }
      } catch (e) {
        console.warn('Teardown frame preload warning:', e);
      }
    })();

    const minSplashDelay = new Promise((resolve) => setTimeout(resolve, 1400));

    Promise.all([preloadPromise, minSplashDelay]).then(() => {
      if (isMounted) {
        router.replace('/teardown');
      }
    });

    return () => {
      isMounted = false;
    };
  }, [router]);

  const handlePress = () => {
    router.replace('/teardown');
  };

  return (
    <TouchableOpacity
      activeOpacity={1}
      style={styles.container}
      onPress={handlePress}
      accessibilityRole="button"
      accessibilityLabel="KernelCraft loading screen"
    >
      {/* Elevated Warm Radial Gradient Background */}
      <Svg
        height="100%"
        width="100%"
        style={StyleSheet.absoluteFill}
        preserveAspectRatio="none"
      >
        <Defs>
          <RadialGradient
            id="warmSplashGradient"
            cx="50%"
            cy="35%"
            r="75%"
            fx="50%"
            fy="35%"
          >
            <Stop offset="0%" stopColor="#F97316" stopOpacity="1" />
            <Stop offset="55%" stopColor="#EA580C" stopOpacity="1" />
            <Stop offset="100%" stopColor="#C2410C" stopOpacity="1" />
          </RadialGradient>
        </Defs>
        <Rect x="0" y="0" width="100%" height="100%" fill="url(#warmSplashGradient)" />
      </Svg>

      {/* Main Brand Content */}
      <Animated.View
        style={[
          styles.content,
          {
            opacity: fadeAnim,
            transform: [{ scale: scaleAnim }],
          },
        ]}
      >
        {/* Brand Icon: Frosted Glass Container with Geometric Silicon Die Emblem */}
        <View style={styles.frostedGlassContainer}>
          <Svg width={38} height={38} viewBox="0 0 38 38" fill="none">
            {/* Outer Silicon Substrate Package */}
            <Rect
              x={7.5}
              y={7.5}
              width={23}
              height={23}
              rx={4.5}
              stroke="#FFFFFF"
              strokeWidth={2}
            />

            {/* Multi-layered Internal Processing Core Quad */}
            <Rect x={11} y={11} width={6} height={6} rx={1.5} fill="#FFFFFF" />
            <Rect x={21} y={11} width={6} height={6} rx={1.5} fill="#FFFFFF" />
            <Rect x={11} y={21} width={6} height={6} rx={1.5} fill="#FFFFFF" />
            <Rect x={21} y={21} width={6} height={6} rx={1.5} fill="#FFFFFF" />

            {/* Central Interconnect Bus Nucleus */}
            <Circle cx={19} cy={19} r={1.8} fill="#FFFFFF" />

            {/* Symmetrical External Bus Pins */}
            {/* Top Pins */}
            <Line x1={14} y1={2} x2={14} y2={7.5} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
            <Line x1={19} y1={1.5} x2={19} y2={7.5} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
            <Line x1={24} y1={2} x2={24} y2={7.5} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />

            {/* Bottom Pins */}
            <Line x1={14} y1={30.5} x2={14} y2={36} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
            <Line x1={19} y1={30.5} x2={19} y2={36.5} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
            <Line x1={24} y1={30.5} x2={24} y2={36} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />

            {/* Left Pins */}
            <Line x1={2} y1={14} x2={7.5} y2={14} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
            <Line x1={1.5} y1={19} x2={7.5} y2={19} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
            <Line x1={2} y1={24} x2={7.5} y2={24} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />

            {/* Right Pins */}
            <Line x1={30.5} y1={14} x2={36} y2={14} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
            <Line x1={30.5} y1={19} x2={36.5} y2={19} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
            <Line x1={30.5} y1={24} x2={36} y2={24} stroke="#FFFFFF" strokeWidth={1.8} strokeLinecap="round" />
          </Svg>
        </View>

        {/* Clean Balanced Title Case */}
        <Text style={styles.title}>KernelCraft</Text>

        {/* Regular Weight Translucent Subtitle */}
        <Text style={styles.subtitle}>From Silicon to Syscall</Text>
      </Animated.View>

      {/* Sleek Minimalist 40px Horizontal Progress Pill Loading Feedback */}
      <View style={styles.loadingContainer}>
        <View style={styles.loadingTrack}>
          <Animated.View
            style={[
              styles.loadingFill,
              {
                width: progressAnim.interpolate({
                  inputRange: [0, 1],
                  outputRange: [0, 40],
                }),
              },
            ]}
          />
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#EA580C',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
    position: 'relative',
    ...(Platform.OS === 'web'
      ? ({
          background:
            'radial-gradient(circle at 50% 35%, #F97316 0%, #EA580C 55%, #C2410C 100%)',
        } as any)
      : {}),
  },
  content: {
    alignItems: 'center',
    width: '100%',
    maxWidth: 380,
    marginBottom: 40,
  },
  frostedGlassContainer: {
    width: 76,
    height: 76,
    borderRadius: 22,
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.25)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
    shadowColor: '#C2410C',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.35,
    shadowRadius: 18,
    elevation: 8,
    ...(Platform.OS === 'web'
      ? ({
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
        } as any)
      : {}),
  },
  title: {
    fontFamily: FontFamilies.sans,
    fontSize: 32,
    fontWeight: '700',
    letterSpacing: -0.64, // -0.02em * 32px tight tracking
    color: '#FFFFFF',
    textAlign: 'center',
  },
  subtitle: {
    fontFamily: FontFamilies.sans,
    fontSize: 15,
    fontWeight: '400',
    color: 'rgba(255, 255, 255, 0.85)',
    textAlign: 'center',
    marginTop: 8,
    letterSpacing: 0.2,
  },
  loadingContainer: {
    position: 'absolute',
    bottom: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loadingTrack: {
    width: 40,
    height: 3,
    borderRadius: 1.5,
    backgroundColor: 'rgba(255, 255, 255, 0.25)',
    overflow: 'hidden',
  },
  loadingFill: {
    height: '100%',
    borderRadius: 1.5,
    backgroundColor: '#FFFFFF',
  },
});
