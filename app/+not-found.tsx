import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Link, Stack } from 'expo-router';
import { Colors } from '../src/theme/colors';
import { Typography } from '../src/theme/typography';

export default function NotFoundScreen() {
  return (
    <>
      <Stack.Screen options={{ title: 'Page Not Found' }} />
      <View style={styles.container}>
        <Text style={styles.title}>404 - Not Found</Text>
        <Text style={styles.subtitle}>This hardware route does not exist.</Text>
        <Link href="/teardown" asChild>
          <TouchableOpacity style={styles.linkButton}>
            <Text style={styles.linkText}>Return to Teardown</Text>
          </TouchableOpacity>
        </Link>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.darkBackground,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  title: {
    ...Typography.headline,
    color: Colors.textWhite,
    marginBottom: 8,
  },
  subtitle: {
    ...Typography.body,
    color: Colors.inkLight,
    marginBottom: 20,
  },
  linkButton: {
    backgroundColor: Colors.studioOrange,
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 12,
  },
  linkText: {
    ...Typography.tag,
    color: Colors.textWhite,
  },
});
