import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Animated,
  TouchableOpacity,
  Platform,
} from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { Colors } from '../src/theme/colors';
import { Typography } from '../src/theme/typography';
import { Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';

export default function SiliconZoomTransitionScreen() {
  const router = useRouter();
  const zoomAnim = useRef(new Animated.Value(1)).current;
  const opacityAnim = useRef(new Animated.Value(1)).current;
  const screenGlowAnim = useRef(new Animated.Value(0)).current;
  const [zoomStage, setZoomStage] = useState('APPROACHING FRONT DISPLAY');

  useEffect(() => {
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      } catch (e) {}
    }

    const stageTimer1 = setTimeout(() => {
      setZoomStage('DIVING THROUGH GLASS PIXELS');
      if (Platform.OS !== 'web') {
        try {
          Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
        } catch (e) {}
      }
    }, 500);

    const stageTimer2 = setTimeout(() => {
      setZoomStage('WAKING UP THE OPERATING SYSTEM');
      if (Platform.OS !== 'web') {
        try {
          Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
        } catch (e) {}
      }
    }, 1100);

    // Zoom animation sequence straight into the front screen glass
    Animated.parallel([
      Animated.timing(zoomAnim, {
        toValue: 6.0,
        duration: 1700,
        useNativeDriver: true,
      }),
      Animated.timing(opacityAnim, {
        toValue: 0.1,
        duration: 1700,
        useNativeDriver: true,
      }),
      Animated.timing(screenGlowAnim, {
        toValue: 1,
        duration: 1200,
        delay: 350,
        useNativeDriver: true,
      }),
    ]).start(() => {
      router.replace('/kernel');
    });

    return () => {
      clearTimeout(stageTimer1);
      clearTimeout(stageTimer2);
    };
  }, []);

  const skipTransition = () => {
    router.replace('/kernel');
  };

  return (
    <View style={styles.container}>
      {/* Zooming Physical Front Screen Frame */}
      <Animated.View
        style={[
          styles.zoomLayer,
          {
            transform: [
              { scale: zoomAnim },
              { translateY: -15 },
            ],
            opacity: opacityAnim,
          },
        ]}
      >
        <Image
          source={require('../assets/teardown_frames/frame_59.webp')}
          style={styles.stillImage}
          contentFit="contain"
        />
      </Animated.View>

      {/* Screen Digital Glow & Pixel Matrix */}
      <Animated.View
        style={[
          styles.digitalGlowLayer,
          {
            opacity: screenGlowAnim,
          },
        ]}
      >
        <View style={styles.pixelGrid}>
          <View style={styles.reticleRing} />
          <View style={styles.sparkleBadge}>
            <Ionicons name="sparkles" size={32} color="#FFA07A" />
            <Text style={styles.glowText}>ENTERING OS SOFTWARE</Text>
          </View>
        </View>
      </Animated.View>

      {/* HUD Telemetry Overlay */}
      <View style={styles.hudOverlay}>
        <View style={styles.topRow}>
          <TouchableOpacity
            style={styles.cancelButton}
            onPress={() => router.back()}
            accessibilityLabel="Cancel zoom"
          >
            <Ionicons name="arrow-back" size={20} color={Colors.textWhite} />
          </TouchableOpacity>

          <View style={styles.magnificationBadge}>
            <Text style={styles.magnificationText}>MAGNIFICATION: 10,000x</Text>
          </View>

          <TouchableOpacity style={styles.skipButton} onPress={skipTransition}>
            <Text style={styles.skipButtonText}>SKIP</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.bottomHud}>
          <View style={styles.stageBadge}>
            <View style={styles.pulseDot} />
            <Text style={styles.stageText}>{zoomStage}</Text>
          </View>
          <Text style={styles.hudSubtitle}>
            Crossing from hardware components into the living OS software layer
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0A0A10',
    overflow: 'hidden',
  },
  zoomLayer: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
  },
  stillImage: {
    width: '100%',
    height: '100%',
  },
  digitalGlowLayer: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(221, 86, 34, 0.25)',
  },
  pixelGrid: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  reticleRing: {
    width: 220,
    height: 220,
    borderRadius: 110,
    borderWidth: 2,
    borderColor: '#FFA07A',
    borderStyle: 'dashed',
    position: 'absolute',
  },
  sparkleBadge: {
    alignItems: 'center',
    gap: 8,
  },
  glowText: {
    ...Typography.subhead,
    color: '#FFFFFF',
    fontWeight: '900',
    letterSpacing: 2,
    fontSize: 16,
  },
  hudOverlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'space-between',
    padding: 20,
    paddingTop: 45,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  cancelButton: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
  },
  magnificationBadge: {
    backgroundColor: 'rgba(0, 0, 0, 0.65)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
  },
  magnificationText: {
    ...Typography.tag,
    color: '#FFA07A',
    fontSize: 10,
    fontWeight: '800',
  },
  skipButton: {
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 16,
  },
  skipButtonText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 11,
    fontWeight: '800',
  },
  bottomHud: {
    alignItems: 'center',
    gap: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    padding: 16,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.15)',
  },
  stageBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: 'rgba(221, 86, 34, 0.35)',
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 12,
  },
  pulseDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#FFA07A',
  },
  stageText: {
    ...Typography.tag,
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  hudSubtitle: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.75)',
    fontSize: 12,
    textAlign: 'center',
  },
});
