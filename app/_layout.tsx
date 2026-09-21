import React from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { View, StyleSheet, Platform, useWindowDimensions } from 'react-native';
import { Colors } from '../src/theme/colors';

export default function RootLayout() {
  const { width } = useWindowDimensions();
  const isLargeScreen = width > 520;

  return (
    <View style={styles.outerBackdrop}>
      <StatusBar style="light" />
      <View style={[styles.phoneContainer, isLargeScreen && styles.phoneContainerFrame]}>
        <Stack
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: Colors.darkBackground },
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
