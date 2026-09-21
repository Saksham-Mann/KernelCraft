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
  const dieOpacityAnim = useRef(new Animated.Value(0)).current;
  const [zoomStage, setZoomStage] = useState('ENTERING MACRO OPTICS');

  useEffect(() => {
    // Progressive haptic escalation
    if (Platform.OS !== 'web') {
      try {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      } catch (e) {}
    }

    const stageTimer1 = setTimeout(() => {
      setZoomStage('FOCUSING 4nm FINFET DIE');
      if (Platform.OS !== 'web') {
        try {
          Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
        } catch (e) {}
      }
    }, 600);

    const stageTimer2 = setTimeout(() => {
      setZoomStage('LOCKING SILICON & KERNEL BUS');
      if (Platform.OS !== 'web') {
        try {
          Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
        } catch (e) {}
      }
    }, 1200);

    // Zoom animation sequence
    Animated.parallel([
      Animated.timing(zoomAnim, {
        toValue: 5.5,
        duration: 1800,
        useNativeDriver: true,
      }),
      Animated.timing(opacityAnim, {
        toValue: 0.2,
        duration: 1800,
        useNativeDriver: true,
      }),
      Animated.timing(dieOpacityAnim, {
        toValue: 1,
        duration: 1400,
        delay: 500,
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
      {/* Zooming Physical Board Viewport */}
      <Animated.View
        style={[
          styles.zoomLayer,
          {
            transform: [
              { scale: zoomAnim },
              // Anchor zoom at SoC coordinates (around center-top of board)
              { translateX: -10 },
              { translateY: 40 },
            ],
            opacity: opacityAnim,
          },
        ]}
      >
        <Image
          source={require('../assets/chip_highlight_still.webp')}
          style={styles.stillImage}
          contentFit="contain"
        />
      </Animated.View>

      {/* Crossfading Macro Die Wireframe */}
      <Animated.View
        style={[
          styles.dieLayer,
          {
            opacity: dieOpacityAnim,
          },
        ]}
      >
        <View style={styles.reticleRing} />
        <View style={styles.reticleCrosshairH} />
        <View style={styles.reticleCrosshairV} />
      </Animated.View>

      {/* Optical Vignette Frame */}
      <View style={styles.vignetteOverlay} pointerEvents="none" />

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
            <Text style={styles.magnificationText}>MAGNIFICATION: 25,000x</Text>
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
            Entering Cortex-X4 Prime Core and Linux Kernel Stack
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#050508',
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
  dieLayer: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
  },
  reticleRing: {
    width: 180,
    height: 180,
    borderRadius: 90,
    borderWidth: 2,
    borderColor: Colors.studioOrange,
    borderStyle: 'dashed',
  },
  reticleCrosshairH: {
    position: 'absolute',
    width: 240,
    height: 1,
    backgroundColor: 'rgba(221, 86, 34, 0.5)',
  },
  reticleCrosshairV: {
    position: 'absolute',
    height: 240,
    width: 1,
    backgroundColor: 'rgba(221, 86, 34, 0.5)',
  },
  vignetteOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'transparent',
    borderWidth: 30,
    borderColor: 'rgba(5, 5, 8, 0.8)',
  },
  hudOverlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'space-between',
    padding: 20,
    paddingTop: 50,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  cancelButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  magnificationBadge: {
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  magnificationText: {
    ...Typography.tag,
    color: Colors.studioOrange,
    fontSize: 10,
  },
  skipButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
  },
  skipButtonText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 11,
  },
  bottomHud: {
    alignItems: 'center',
    gap: 6,
    marginBottom: 30,
  },
  stageBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: 'rgba(221, 86, 34, 0.25)',
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.studioOrange,
  },
  pulseDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: Colors.studioOrange,
  },
  stageText: {
    ...Typography.tag,
    color: Colors.textWhite,
    fontSize: 11,
  },
  hudSubtitle: {
    ...Typography.bodySmall,
    color: 'rgba(255, 255, 255, 0.7)',
    fontSize: 12,
    textAlign: 'center',
  },
});
