# design.md — UI/UX Design System

> Visual language reference: `image_13cee8.jpg`. This system deliberately rejects current AI-generated-UI defaults — **no glassmorphism, no "liquid glass" translucency/blur-everything, no dark-mode neon/cyberpunk aesthetic.** The target feeling is bold, warm, educational, and physical — closer to a well-designed consumer wellness/e-commerce app than a developer tool.

## 1. Color System

### 1.1 Principle
Each conceptual "layer" of the app owns a **solid, saturated, opaque background color** — no gradients-as-crutch, no glass panels. Backgrounds shift discretely as the user moves between OS layers, reinforcing "you are now in a different part of the system."

### 1.2 Palette (from reference)
| Token | Hex (approx) | Usage |
|---|---|---|
| `color.crimson` | `#B0223F` | Hardware Teardown default / "Sleep" track equivalent |
| `color.steelBlue` | `#4E7F9E` | OS Architecture upper layers / brand/about surfaces |
| `color.mustard` | `#E8A020` | Syscall Interface / interactive layer, high-energy CTA moments |
| `color.surface` | `#FFFFFF` | All content cards/bottom-sheets — always stark white, always opaque |
| `color.ink` | `#141414` | Primary text on white, primary icon fill on solid backgrounds |
| `color.inkMuted` | `#6B6B6B` | Secondary text |
| `color.onAccentPrimary` | `#FFFFFF` | Text/icons on solid color backgrounds |
| `color.badgeDark` | `#1A1A1A` | Filled circular badges (e.g., quantity/step indicators) |

**Rule:** Never use these colors at less than full opacity for a background. Opacity/blur is reserved only for very short-lived transition states (Dive-In Transition), never for static UI chrome.

### 1.3 Per-Layer Color Mapping
- Selection Hub cards: each track gets one palette color as its full card background.
- Hardware Teardown: crimson.
- OS Architecture layers: steel blue (Hardware/Kernel/Drivers) transitioning to mustard (Syscall Interface/OS Services/Userspace) — a single discrete hue shift roughly at the halfway point of the stack, not a continuous gradient.
- Syscall Simulator: mustard background frame, white content card.

## 2. Surfaces & Shape
- **Cards / bottom-sheets:** Large corner radius — `24dp` minimum, `32dp` for the primary content bottom-sheets — top corners only when the sheet is anchored to the bottom of a solid-color section (matches reference product cards).
- Cards are **always opaque white**, sitting with clear contrast against the solid background — no drop-shadow-heavy elevation trickery; separation comes from color contrast, not shadow.
- Shape token: `shape.sheet = RoundedCornerShape(topStart = 32dp, topEnd = 32dp)`, `shape.card = RoundedCornerShape(20dp)`.

## 3. UI Elements

### 3.1 Buttons
- **Primary interactive element = circular icon button**, 48–56dp diameter.
- Two variants only:
  - **Solid dark:** `color.badgeDark` fill, white icon — used for primary actions (back nav, cart/count badges, "Dive In").
  - **Outline:** transparent fill, white or ink stroke depending on background — used for secondary actions.
- No pill-shaped text buttons as primary CTAs except where a label is unavoidable (e.g., "Restart" on Syscall completion) — even then, keep them minimal: white/dark filled rounded-rect, no shadow, no gradient.
- Quantity-style circular badges (e.g., step counter "1/5", chapter counter) mirror the reference's numbered circle badges (30/60/90 pattern) — solid-fill circle, white bold number.

### 3.2 Iconography
- Simple, geometric, single-weight line or solid icons (matches the reference's dashed-circle logo mark and simple back-chevron/person/cart icons). No skeuomorphism, no multi-color icon sets.

### 3.3 Bottom Navigation / Chips
- Section toggles (e.g., "Products/Contact" or "Relax/Sleep" equivalents — track/chapter selectors) render as simple rounded-rect chips, outline or soft-fill, sans-serif label, no icons unless essential.

## 4. Typography
- Typeface: a clean geometric-humanist sans-serif (e.g., **Inter** or **Manrope** as the production substitute for the reference's serif-adjacent display face on "Powerful Boost" — note the reference mixes a friendly serif for hero moments with sans for UI; KernelCraft standardizes on sans throughout for consistency and legibility at small sizes on educational content).
- **Scale:**
  | Token | Size / Weight | Usage |
  |---|---|---|
  | `type.display` | 34sp / Bold | Hero moments ("Powerful Boost"-equivalent screen titles) |
  | `type.headline` | 24sp / SemiBold | Card/sheet titles ("Sleep 30 Dissolvable Wafers"-equivalent — layer/track titles) |
  | `type.body` | 16sp / Regular | Explanatory body text in white sheets |
  | `type.bodySmall` | 14sp / Regular | Secondary/meta text ("250 mg"-equivalent) |
  | `type.priceEquivalent` | 20sp / Bold | Emphasis numerals — repurposed for key stats/step counters |
- High hierarchical contrast is mandatory: never place `type.body` and `type.headline` at similar visual weight on the same card — the reference's strong title/price/meta size jump is the model to replicate.

## 5. Motion Principles
- Card tap: scale to 0.97 over 100ms, spring back — matches the "friendly, tactile" feel, not a flashy bounce.
- Screen-level solid-color background transitions (between OS layers): instantaneous or very short (~150ms) crossfade at the section boundary — deliberately discrete, not a slow gradient morph, to reinforce "distinct system layer."
- Dive-In Transition is the one place a longer (~600–900ms), more expressive animation is allowed (scale + blur + crossfade) — it's the single "wow" moment; everything else stays crisp and fast.
- **Explicitly forbidden:** frosted-glass blur on static chrome, neon glow/emissive effects, animated gradient backgrounds, particle effects, skeuomorphic shadows/bevels.

## 6. Componentized Tokens (for `core/theme/`)
```kotlin
object KernelCraftColors {
    val crimson = Color(0xFFB0223F)
    val steelBlue = Color(0xFF4E7F9E)
    val mustard = Color(0xFFE8A020)
    val surface = Color(0xFFFFFFFF)
    val ink = Color(0xFF141414)
    val inkMuted = Color(0xFF6B6B6B)
    val onAccentPrimary = Color(0xFFFFFFFF)
    val badgeDark = Color(0xFF1A1A1A)
}

object KernelCraftShapes {
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val card = RoundedCornerShape(20.dp)
    val circularButton = CircleShape
}

// Typography: define via androidx.compose.material3.Typography using the tokens in §4,
// mapped to Manrope/Inter FontFamily loaded via res/font.
```

## 7. Accessibility Notes
- Solid-color backgrounds must meet WCAG AA contrast against `onAccentPrimary`/white text/icons at all times — verify each palette color against `color.surface`'s white text usage and against `color.ink` on white cards.
- Circular buttons: minimum 48dp tap target even if visual icon is smaller (use padding, not a larger icon, to hit target size).