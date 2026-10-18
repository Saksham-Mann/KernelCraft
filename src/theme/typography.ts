import { Platform, TextStyle } from 'react-native';

/**
 * KernelCraft Typography Scale
 *
 * Implements the typography tokens specified in Design.md (§4):
 * High hierarchical contrast using clean humanist/geometric sans.
 */
const fontFamily = Platform.select({
  ios: 'System',
  android: 'Roboto',
  default: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
});

const monoFont = Platform.select({
  ios: 'Menlo',
  android: 'monospace',
  default: 'SFMono-Regular, Menlo, Monaco, Consolas, monospace',
});

export const Typography: Record<string, TextStyle> = {
  display: {
    fontFamily,
    fontSize: 34,
    fontWeight: '800',
    lineHeight: 40,
    letterSpacing: -0.5,
  },
  headline: {
    fontFamily,
    fontSize: 24,
    fontWeight: '700',
    lineHeight: 30,
    letterSpacing: -0.3,
  },
  subhead: {
    fontFamily,
    fontSize: 18,
    fontWeight: '600',
    lineHeight: 24,
  },
  body: {
    fontFamily,
    fontSize: 15,
    fontWeight: '400',
    lineHeight: 22,
  },
  bodySmall: {
    fontFamily,
    fontSize: 13,
    fontWeight: '400',
    lineHeight: 18,
  },
  tag: {
    fontFamily,
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  code: {
    fontFamily: monoFont,
    fontSize: 13,
    lineHeight: 18,
  },
  statsNumeral: {
    fontFamily,
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: -0.5,
  },
};
