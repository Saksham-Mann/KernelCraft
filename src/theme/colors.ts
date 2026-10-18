/**
 * KernelCraft Color System
 *
 * Implements the design tokens specified in Design.md (§1.2 & §6):
 * Bold, saturated, opaque backgrounds without murky glassmorphism or neon glow.
 */
export const Colors = {
  // Brand & Layer Anchors
  crimson: '#B0223F',        // Hardware Teardown default / brand primary
  steelBlue: '#4E7F9E',      // Physical Silicon, Linux Kernel, Treble HAL
  mustard: '#E8A020',        // Syscall Interface, ART Runtime, Userspace
  studioOrange: '#DD5622',   // Primary energy accent / active border / brand mark
  edgeGlowOrange: '#FF7033', // Subtle edge highlight

  // Surfaces & Backgrounds
  surface: '#FFFFFF',        // Pure white bottom sheets & content cards
  darkBackground: '#0D0D12', // Silicon immersion die & dark mode background
  cardDark: '#181822',       // Dark card background
  cardDarkSelected: '#222230',// Selected card tier background

  // Typography & Content
  ink: '#141414',            // Primary text on white cards
  inkMuted: '#6B6B6B',       // Secondary meta text on white cards
  inkLight: '#9E9EAA',       // Secondary meta text on dark surfaces
  textLight: '#F5F5F7',      // Primary text on dark surfaces
  textWhite: '#FFFFFF',      // Pure white text on solid colored surfaces

  // Action & Status Tokens
  badgeDark: '#1A1A1A',      // Circular action buttons and numeric badges
  badgeGreen: '#34C759',     // Hardware healthy status / verified
  badgeBlue: '#007AFF',      // Interconnect / bus active
  badgeAmber: '#FF9500',     // Throttling / thermal warning
  borderDark: '#2C2C3A',     // Subtle dark borders
  borderLight: '#E5E5EA',    // Light borders for cards
} as const;
