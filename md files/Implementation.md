# implementation.md — Phased Development Roadmap

## Phase 1: Asset Prep
**Goal:** All media assets exist, encoded correctly, before any UI code depends on them.

1. Shoot/source the physical teardown footage (single continuous take per track, steady cam/rig, consistent lighting).
2. Edit down to the 5-chapter narrative beats (10–20s final duration per techspec.md).
3. Encode to All-Intra H.264, verify with `ffprobe` that every frame is a keyframe (`ffprobe -show_frames -select_streams v | grep pict_type` should show all `I`).
4. Export a matching static high-res still frame (chip-highlight moment) for the Dive-In Transition handoff.
5. Produce chip/OS-layer illustrations (Hardware, Kernel, Drivers, Syscall Interface, OS Services, Userspace) as vector assets (SVG → Compose-compatible, e.g., via `ImageVector` or bundled as drawables).
6. Author 2–3 small Lottie animations (stepper icon feedback, syscall completion checkmark, idle chip pulse).
7. Write all track content JSON: chapter captions, layer descriptions, syscall step text — validate against the schema (see schema.md) with a quick script before touching UI code.
8. **Exit criteria:** All assets committed to `res/raw`/`assets`, sizes logged, JSON validates against schema.

## Phase 2: Core Scaffold
**Goal:** App shell exists, navigates between empty screens, architecture is in place.

1. Set up single-Activity Compose project, Compose Navigation graph with all 5 routes (stub composables).
2. Set up `ViewModel`s per screen with the `UiState` data classes from schema.md wired to `StateFlow`, no real logic yet.
3. Wire `DataStore` for progress/last-track persistence.
4. Build the JSON manifest loader (`kotlinx.serialization`) and a repository layer that exposes track content to ViewModels.
5. Establish the design system: color tokens, typography scale, shape tokens (large rounded corners), circular button component — all per design.md — as a small internal Compose theme module before any screen-specific UI.
6. **Exit criteria:** App builds, navigates Hub → Teardown → Transition → OS Stack → Syscall Sim → back, with placeholder content, using final theme tokens.

## Phase 3: Scrollytelling UI
**Goal:** The signature scroll-driven experience is fully functional and performant.

1. Build Selection Hub with real track cards + tap micro-interaction.
2. Implement Hardware Teardown: ExoPlayer integration, scroll-to-seek mapping, throttled seek pipeline, chapter caption bottom-sheet, chip-highlight overlay.
3. Implement Dive-In Transition: `graphicsLayer`-based scale/blur/crossfade `Animatable` sequence, static-still handoff.
4. Implement OS Architecture parallax stack: layered `LazyColumn`, `derivedStateOf`-driven parallax offsets, per-layer white bottom-sheets.
5. Instrument Macrobenchmark on the Teardown screen; profile with Layout Inspector / Perfetto; fix any recomposition storms found.
6. Device-matrix manual test (low/mid/high tier) — confirm memory ceiling (<250MB peak) and frame-drop target (<5%) from prd.md.
7. **Exit criteria:** Full scroll journey Hub → Syscall Sim entry point works smoothly on all 3 test-tier devices, memory/perf targets met.

## Phase 4: Interactive Sandbox
**Goal:** Syscall Simulator is complete, polished, and the app is release-ready.

1. Build the 5-step Syscall Simulator state machine (`HorizontalPager` or step-indexed `AnimatedContent`), wire to `SyscallSimUiState`.
2. Build per-step diagrams (privilege-ring flip, dispatch table, arrow-flow animations) using Compose Canvas/vector assets + light Lottie accents.
3. Wire completion state → "Restart"/"Back to Hub" flow, persist "track completed" to DataStore.
4. Full accessibility pass: content descriptions on all interactive elements, minimum touch target sizes (48dp) on circular buttons, TalkBack walkthrough of the Syscall Simulator specifically (since it's the most interaction-dense screen).
5. Full QA pass against tracker.md checklist.
6. Prepare release build: asset-pack decision finalized (base APK vs. Play Asset Delivery per techspec.md §2.2), ProGuard/R8 rules verified, final device-matrix regression pass.
7. **Exit criteria:** All tracker.md items checked, crash-free on device matrix, ready for internal/closed testing track.