import React, { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { View, StyleSheet, Platform, useWindowDimensions } from 'react-native';
import { Colors } from '../src/theme/colors';
import {
  PlusJakartaSans_400Regular,
  PlusJakartaSans_500Medium,
  PlusJakartaSans_600SemiBold,
  PlusJakartaSans_700Bold,
} from '@expo-google-fonts/plus-jakarta-sans';
import {
  JetBrainsMono_400Regular,
  JetBrainsMono_500Medium,
  JetBrainsMono_600SemiBold,
} from '@expo-google-fonts/jetbrains-mono';

import * as Font from 'expo-font';
import Ionicons from '@expo/vector-icons/Ionicons';

export default function RootLayout() {
  const { width } = useWindowDimensions();
  const isLargeScreen = width > 520;

  // Non-blocking Font Loading & Resilience System
  useEffect(() => {
    if (Platform.OS === 'web' && typeof document !== 'undefined') {
      // 1. Ensure Google Fonts stylesheet fallback link is present in head
      const linkId = 'kernelcraft-google-fonts';
      if (!document.getElementById(linkId)) {
        const link = document.createElement('link');
        link.id = linkId;
        link.rel = 'stylesheet';
        link.href =
          'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap';
        document.head.appendChild(link);
      }

      // 2. CSS typography reset with system font fallbacks
      const styleId = 'kernelcraft-typography-reset';
      if (!document.getElementById(styleId)) {
        const style = document.createElement('style');
        style.id = styleId;
        style.innerHTML = `
          :root {
            --font-sans: 'Plus Jakarta Sans', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            --font-mono: 'JetBrains Mono', monospace;
          }
          *, html, body, button, input, textarea, select, [dir="auto"] {
            font-family: var(--font-sans);
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
          }
          .fonts-fallback *, .fonts-fallback [dir="auto"] {
            font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif !important;
          }
        `;
        document.head.appendChild(style);
      }

      // 3. Non-blocking FontFace check wrapped in .catch() so timeout never crashes runtime
      try {
        const doc = document as any;
        if (doc && 'fonts' in doc && doc.fonts?.load) {
          Promise.race([
            doc.fonts.load('1em "Plus Jakarta Sans"'),
            new Promise((_, reject) =>
              setTimeout(() => reject(new Error('Font load timeout')), 3000)
            ),
          ])
            .then(() => {
              doc.documentElement?.classList.add('fonts-loaded');
            })
            .catch((err: any) => {
              console.warn('Font load timeout, falling back to system sans-serif:', err);
              doc.documentElement?.classList.add('fonts-fallback');
            });
        } else if (doc?.documentElement) {
          doc.documentElement.classList.add('fonts-loaded');
        }
      } catch (e) {
        (document as any)?.documentElement?.classList.add('fonts-fallback');
      }
    } else {
      // On native mobile (iOS / Android), load fonts asynchronously with non-blocking .catch()
      Font.loadAsync({
        ...Ionicons.font,
        PlusJakartaSans_400Regular,
        PlusJakartaSans_500Medium,
        PlusJakartaSans_600SemiBold,
        PlusJakartaSans_700Bold,
        JetBrainsMono_400Regular,
        JetBrainsMono_500Medium,
        JetBrainsMono_600SemiBold,
      }).catch((err) => {
        console.warn('Native font load fallback:', err);
      });
    }
  }, []);

  return (
    <View style={styles.outerBackdrop}>
      <StatusBar style="light" />
      <View style={[styles.phoneContainer, isLargeScreen && styles.phoneContainerFrame]}>
        <Stack
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: Colors.darkBackground, flex: 1 },
            animation: 'fade',
          }}
        >
          <Stack.Screen name="index" />
          <Stack.Screen name="teardown" />
          <Stack.Screen
            name="detail/[id]"
            options={{
              presentation: 'modal',
              animation: 'slide_from_bottom',
            }}
          />
          <Stack.Screen
            name="specs"
            options={{
              presentation: 'modal',
              animation: 'slide_from_bottom',
            }}
          />
          <Stack.Screen name="transition" />
          <Stack.Screen name="kernel" />
        </Stack>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  outerBackdrop: {
    flex: 1,
    backgroundColor: '#07070B', // Sleek dark studio backdrop on desktop
    justifyContent: 'center',
    alignItems: 'center',
  },
  phoneContainer: {
    flex: 1,
    width: '100%',
    height: '100%',
    backgroundColor: Colors.darkBackground,
    overflow: 'hidden',
  },
  phoneContainerFrame: {
    maxWidth: 440,
    maxHeight: 920,
    borderRadius: 36,
    borderWidth: 8,
    borderColor: '#1E1E28',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 16 },
    shadowOpacity: 0.6,
    shadowRadius: 30,
    elevation: 20,
  },
});
