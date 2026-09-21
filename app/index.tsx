import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet, Animated, TouchableOpacity, Image } from 'react-native';
import { useRouter } from 'expo-router';
import { Colors } from '../src/theme/colors';
import { Typography } from '../src/theme/typography';
import { Ionicons } from '@expo/vector-icons';

export default function SplashScreen() {
  const router = useRouter();
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const scaleAnim = useRef(new Animated.Value(0.92)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 700,
        useNativeDriver: true,
      }),
      Animated.spring(scaleAnim, {
        toValue: 1,
        friction: 6,
        useNativeDriver: true,
      }),
    ]).start();

    // Auto-advance after 1.8s
    const timer = setTimeout(() => {
      router.replace('/teardown');
    }, 1800);

    return () => clearTimeout(timer);
  }, []);

  const handlePress = () => {
    router.replace('/teardown');
  };

  return (
    <TouchableOpacity activeOpacity={1} style={styles.container} onPress={handlePress}>
      <Animated.View
        style={[
          styles.content,
          {
            opacity: fadeAnim,
            transform: [{ scale: scaleAnim }],
          },
        ]}
      >
        {/* Brand Icon Mark */}
        <View style={styles.logoContainer}>
          <Image
            source={require('../assets/chip_highlight_still.webp')}
            style={styles.logoImage}
            resizeMode="cover"
          />
          <View style={styles.logoBadge}>
            <Ionicons name="hardware-chip-outline" size={24} color={Colors.textWhite} />
          </View>
        </View>

        {/* Title and Tagline */}
        <Text style={styles.title}>KERNELCRAFT</Text>
        <Text style={styles.subtitle}>FROM SILICON TO SYSCALL</Text>

        <View style={styles.badgeRow}>
          <View style={styles.versionBadge}>
            <Text style={styles.versionText}>v1.0.0 • HARDWARE & OS EXPLORER</Text>
          </View>
        </View>

        <View style={styles.bottomPrompt}>
          <Text style={styles.promptText}>Tap to start exploration</Text>
          <Ionicons name="arrow-forward" size={16} color="rgba(255, 255, 255, 0.7)" />
        </View>
      </Animated.View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.studioOrange,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  content: {
    alignItems: 'center',
    width: '100%',
    maxWidth: 380,
  },
  logoContainer: {
    width: 100,
    height: 100,
    borderRadius: 50,
    overflow: 'hidden',
    backgroundColor: 'rgba(0, 0, 0, 0.2)',
    borderWidth: 3,
    borderColor: 'rgba(255, 255, 255, 0.4)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
    position: 'relative',
  },
  logoImage: {
    width: '100%',
    height: '100%',
    opacity: 0.85,
  },
  logoBadge: {
    position: 'absolute',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    ...Typography.display,
    color: Colors.textWhite,
    letterSpacing: 2,
    textAlign: 'center',
  },
  subtitle: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.9)',
    letterSpacing: 3,
    fontWeight: '700',
    marginTop: 6,
    textAlign: 'center',
  },
  badgeRow: {
    marginTop: 18,
  },
  versionBadge: {
    backgroundColor: 'rgba(0, 0, 0, 0.25)',
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 12,
  },
  versionText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 10,
  },
  bottomPrompt: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 60,
  },
  promptText: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.75)',
    fontSize: 12,
  },
});
