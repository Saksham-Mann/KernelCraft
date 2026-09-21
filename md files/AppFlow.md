# appflow.md — Screen-by-Screen User Journey

## Screen 0: Splash / Cold Start
- Static solid-color (brand crimson) background, KernelCraft logo center.
- Preloads: track manifest JSON, first video's first keyframe (for instant paint).
- Auto-navigates to Selection Hub after asset check (max 1.5s, per PRD).

## Screen 1: Selection Hub
- **Layout:** Vertical list/grid of solid-color track cards (crimson, steel-blue, mustard — matching reference), each with a title + short subtitle, rounded-corner white info strip at bottom of card (mirrors reference product cards).
- **State:** `HubUiState(tracks: List<TrackSummary>, selectedTrackId: String?)`
- **Interaction:** Tap card → scale-down micro-interaction (graphicsLayer scaleX/Y ~0.97) → navigate to Hardware Teardown screen for that track, passing `trackId`.
- **Nav-back:** N/A (root screen).

## Screen 2: Hardware Teardown (Scroll-Scrubbed Video)
- **Layout:** Full-bleed `ExoPlayer` surface (via `AndroidView` + `PlayerView`, `useController=false`). A thin white bottom-sheet peeks in from the bottom with contextual caption text that updates per scroll "chapter."
- **Scroll mechanics:**
  - A `nestedScroll` connection (or `Scrollable` with a custom `ScrollableState`) captures vertical drag delta.
  - Raw scroll offset (px) is normalized to `[0f, 1f]` against a defined "teardown scroll length" (e.g., 3x screen height of virtual scroll distance).
  - Normalized progress → mapped to a target video timestamp: `targetUs = (progress * videoDurationUs).roundToLong()`.
  - `exoPlayer.seekTo(targetUs)` is called, **throttled** via `snapshotFlow` + `debounce`/frame-rate gating (see techspec.md) — not on every raw pixel delta — to avoid seek-storm.
- **Chapters (caption bottom-sheet content), driven by progress ranges:**
  1. 0.0–0.2: "Meet the device" (intact phone)
  2. 0.2–0.45: "Removing the back cover"
  3. 0.45–0.7: "Exposing the motherboard"
  4. 0.7–0.9: "Locating the SoC" — chip visually highlighted (video has a baked-in highlight ring, or Compose overlay pinned via a normalized-coordinate `Offset` synced to progress)
  5. 0.9–1.0: "This chip runs everything" — triggers the "Dive In" CTA (circular white button, dark icon, per design.md) fading in.
- **Exit interaction:** Tapping the "Dive In" CTA (or continued scroll past 1.0 with a rubber-band "pull to dive" gesture) triggers Screen 3.
- **State:** `TeardownUiState(scrollProgress: Float, currentChapter: Chapter, videoDurationUs: Long, isBuffering: Boolean)`

## Screen 3: Dive-In Transition
- **Layout:** No new video. Takes the last-rendered video frame (or a matching static high-res still of the highlighted chip) as a `Bitmap`/texture, and the OS Architecture screen's first layer, cross-fading between them.
- **Mechanics:**
  - `graphicsLayer { scaleX = scale; scaleY = scale; alpha = alphaOut; translationX/Y = ...; renderEffect = blur (API 31+) }` animated via `Animatable` over ~600–900ms, driven by a single `LaunchedEffect` — **not** scroll-driven, to keep it GPU-cheap and deterministic.
  - Chip still scales up from its highlighted screen-position to fill the viewport, alpha-crossfading into the abstract chip illustration that anchors Screen 4's Hardware layer.
- **Interaction:** Fully automatic (no user input) — plays once on entry. Skippable via tap (skips to end state).
- **State:** `TransitionUiState(progress: Float /* animation-driven, 0–1 */)`

## Screen 4: OS Architecture (Parallax Stack)
- **Layout:** Vertically scrollable `Column`/`LazyColumn` of full-viewport-height "layer" sections, background color solid per layer (steel blue → deeper blue → mustard, per reference), each with a large rounded-top-corner white sheet containing layer explanation, a small diagram, and "key terms."
- **Layers (bottom of stack = hardware, top = user-facing), scrolled top-to-bottom as: Hardware → Kernel → Drivers → Syscall Interface → OS Services → Userspace Apps:**
  1. Hardware (CPU/SoC) — carries over the chip illustration from the transition.
  2. Kernel
  3. Device Drivers
  4. System Call Interface ← this layer's white sheet has a "Try it" circular CTA button.
  5. OS Services (scheduler, filesystem, memory manager)
  6. Userspace Applications
- **Parallax mechanics:** Background illustrations per section scroll at `0.5x` the foreground scroll speed via `Modifier.graphicsLayer { translationY = scrollOffsetPx * 0.5f }` read from a shared `LazyListState`/scroll offset, using `derivedStateOf` to avoid full recomposition per frame.
- **Interaction:** Tapping "Try it" CTA on the Syscall Interface layer → navigates to Screen 5.
- **State:** `OsStackUiState(scrollOffsetPx: Float, activeLayerIndex: Int)`

## Screen 5: Syscall Simulator
- **Layout:** White full-screen card (rounded top corners, solid mustard/crimson background peeking above), step indicator (5 dots) at top, large central diagram area, description text, circular "Next"/"Back" buttons bottom.
- **5 steps (tap-driven, not scroll-driven):**
  1. **User code calls `write()`** — app-space diagram highlighted.
  2. **libc wraps the call** — arrow animates from app box to libc box.
  3. **Trap instruction / mode switch** — visual flips from "User Mode" (white) to "Kernel Mode" (dark accent), CPU privilege ring diagram.
  4. **Kernel executes syscall handler** — dispatch table diagram, handler box highlights.
  5. **Return to userspace** — result value flows back, mode flips back to "User Mode."
- **Interaction:** Tap "Next"/"Back" circular buttons or swipe horizontally between steps (`HorizontalPager`). Each step transition is a Compose `AnimatedContent`/`Crossfade`, not video.
- **Completion:** Step 5 shows a "Restart" and "Back to Hub" circular CTA pair.
- **State:** `SyscallSimUiState(currentStep: Int /*0–4*/, steps: List<SyscallStep>, isComplete: Boolean)`

## Global Navigation Notes
- Single-Activity, Compose Navigation (`NavHost`) with routes: `hub`, `teardown/{trackId}`, `transition/{trackId}`, `osStack/{trackId}`, `syscallSim/{trackId}`.
- System back button: from `teardown` → pop to `hub` (with a confirmation micro-animation, since video state is discarded); from `osStack`/`syscallSim` → pop to previous screen, preserving scroll/step position via `rememberSaveable`.