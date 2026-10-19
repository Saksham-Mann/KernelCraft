import { Platform, TextStyle } from 'react-native';

/**
 * KernelCraft Global Typography System (Google Web Aesthetic)
 *
 * Implements:
 * 1. Primary Sans: Plus Jakarta Sans (Google Sans aesthetic)
 * 2. Technical Mono: JetBrains Mono (timestamps, Gantt badges, calculations)
 * 3. Standardized Hierarchy:
 *    - Headings: 600 / 700
 *    - Labels & Button Copy: 500 / 600
 *    - Descriptions & Body Text: 400
 */

export const FontFamilies = {
  sans: Platform.select({
    web: "var(--font-sans, 'Plus Jakarta Sans', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif)",
    ios: 'Plus Jakarta Sans, System',
    android: 'Plus Jakarta Sans, Roboto, sans-serif',
    default: "'Plus Jakarta Sans', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
  }),
  mono: Platform.select({
    web: "var(--font-mono, 'JetBrains Mono', monospace)",
    ios: 'JetBrains Mono, Menlo, monospace',
    android: 'JetBrains Mono, monospace',
    default: "'JetBrains Mono', monospace",
  }),
};

// Aliases for convenience
export const fontFamily = FontFamilies.sans;
export const monoFont = FontFamilies.mono;

export const Typography: Record<string, TextStyle> = {
  // Headings (600 / 700)
  display: {
    fontFamily: FontFamilies.sans,
    fontSize: 32,
    fontWeight: '700',
    lineHeight: 38,
    letterSpacing: -0.64, // -0.02em
  },
  headline: {
    fontFamily: FontFamilies.sans,
    fontSize: 22,
    fontWeight: '700',
    lineHeight: 28,
    letterSpacing: -0.3,
  },
  subhead: {
    fontFamily: FontFamilies.sans,
    fontSize: 16,
    fontWeight: '600',
    lineHeight: 22,
    letterSpacing: -0.2,
  },

  // Descriptions & Body Text (400)
  body: {
    fontFamily: FontFamilies.sans,
    fontSize: 14,
    fontWeight: '400',
    lineHeight: 20,
  },
  bodySmall: {
    fontFamily: FontFamilies.sans,
    fontSize: 12,
    fontWeight: '400',
    lineHeight: 17,
  },

  // Labels & Button Copy (500 / 600)
  button: {
    fontFamily: FontFamilies.sans,
    fontSize: 13,
    fontWeight: '600',
    lineHeight: 18,
  },
  label: {
    fontFamily: FontFamilies.sans,
    fontSize: 12,
    fontWeight: '500',
    lineHeight: 16,
  },
  tag: {
    fontFamily: FontFamilies.sans,
    fontSize: 10,
    fontWeight: '600',
    letterSpacing: 0.5,
  },
  statsNumeral: {
    fontFamily: FontFamilies.sans,
    fontSize: 20,
    fontWeight: '700',
    letterSpacing: -0.3,
  },

  // Technical Data, Timestamps, Gantt Badges & Tabular Calculations (JetBrains Mono)
  code: {
    fontFamily: FontFamilies.mono,
    fontSize: 12,
    fontWeight: '400',
    lineHeight: 16,
  },
  monoBadge: {
    fontFamily: FontFamilies.mono,
    fontSize: 10,
    fontWeight: '600',
    letterSpacing: 0.2,
  },
  monoTableCell: {
    fontFamily: FontFamilies.mono,
    fontSize: 10,
    fontWeight: '500',
  },
  timestamp: {
    fontFamily: FontFamilies.mono,
    fontSize: 9,
    fontWeight: '500',
  },
};
