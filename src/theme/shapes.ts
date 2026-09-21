/**
 * KernelCraft Shape System
 *
 * Implements the geometry tokens specified in Design.md (§2 & §6):
 * Large corner radius bottom sheets (32px), solid cards (20px), and circular action buttons.
 */
export const Shapes = {
  sheet: {
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
  },
  card: {
    borderRadius: 20,
  },
  chip: {
    borderRadius: 12,
  },
  pill: {
    borderRadius: 9999,
  },
  circularButton: {
    width: 48,
    height: 48,
    borderRadius: 24,
  },
  circularBadge: {
    width: 28,
    height: 28,
    borderRadius: 14,
  },
} as const;
