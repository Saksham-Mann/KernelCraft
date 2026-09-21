# rules.md — Coding Guidelines

## 1. Animation Rules (Hard Constraints)

1. **No image-sequence animations.** Frame-by-frame PNG/WebP sequences (e.g., 60 images played back as a flipbook) are strictly forbidden anywhere in the app. They are memory-expensive (every frame is a full decoded bitmap held or repeatedly decoded), janky (decode-on-draw stalls), and don't scale to scroll-scrubbing. Use, in order of preference for the given use case:
   - **Video (All-Intra encoded)** for anything filmed/photoreal and scroll-scrubbed (Hardware Teardown only).
   - **`graphicsLayer`-driven procedural animation** (`Animatable`, scale/alpha/translation/blur) for transitions and UI motion.
   - **Lottie (vector, After Effects-exported)** for small self-contained iconographic loops only.
   - **Compose `Canvas`/vector drawables** for diagrams (syscall diagrams, OS layer illustrations).
2. Never call `seekTo` synchronously on every raw scroll delta callback — always throttle/sample (see techspec.md §2.3). An un-throttled seek loop will visibly stutter and burn battery.
3. All transition animation reads (`graphicsLayer` block) must read from an `Animatable`/`State<Float>`, never recompute from scratch inside the composable body on every recomposition.
4. Prefer `graphicsLayer` properties (`scaleX`, `alpha`, `translationY`, `renderEffect`) over `Modifier.scale`/`Modifier.offset`/`Modifier.alpha` when the value animates every frame — `graphicsLayer` is draw-phase-only and skips layout/measure passes.

## 2. Memory Constraint Rules

1. **One `ExoPlayer` instance alive at a time, app-wide.** Release in `DisposableEffect.onDispose` without exception. Never instantiate a second player "just in case" while another screen's player is still alive.
2. Never extract video frames to `Bitmap` via `MediaMetadataRetriever` in a loop, and never `PixelCopy` the video surface repeatedly for UI purposes — use pre-bundled static stills for any "freeze frame" need (see techspec.md §2.4).
3. Cap in-memory decoded bitmap resolution to actual display density needs — use `BitmapFactory.Options.inSampleSize` or Coil/Compose-native downsampling for any raster illustration assets; never load a 4K source asset to display at 200dp.
4. Video buffer windows (`DefaultLoadControl`) must be explicitly tuned down for scrub-mode playback — do not use ExoPlayer's linear-playback defaults, which over-buffer for a use case that rarely plays linearly forward.
5. Every screen's `ViewModel` must release/cancel any held media, animation jobs, or large in-memory objects in `onCleared()`.
6. Run Macrobenchmark memory profiling on the Hardware Teardown screen before every release — regression here is the highest-risk OOM surface in the app (see prd.md success metric: <250MB peak).

## 3. Jetpack Compose Best Practices

1. **Hoist state, don't smuggle it.** All screen-level state lives in a `StateFlow<UiState>` in a `ViewModel`; composables are stateless renderers of that state plus event callbacks (`onEvent: (Event) -> Unit`).
2. **Use `derivedStateOf` for any value computed from rapidly-changing state** (scroll offset, drag position) that gates *whether* something recomposes, not just *what* it renders — this is mandatory for the parallax OS stack (see techspec.md §4) and the teardown chapter-detection logic.
3. **Read scroll/animation values via `snapshotFlow`, not `LaunchedEffect` polling loops.** Any time you need to react to a continuously-changing `State` (e.g., trigger a seek when `scrollProgress` changes), wrap it in `snapshotFlow { ... }.collect { ... }`.
4. **No business logic in composables.** Composable functions map `UiState → UI` and forward user actions as events; all mapping/decision logic (e.g., "which chapter is this progress value in") lives in the `ViewModel` or a plain Kotlin domain function, unit-testable without Compose.
5. **Stable/Immutable data classes only in `UiState`.** Every field in a `UiState` data class must be a `val`, and collections must be `List`/`ImmutableList` (via `kotlinx.collections.immutable` if churn is heavy) — never `MutableList`/`MutableState` fields inside a state data class, to keep Compose's stability inference intact and avoid unnecessary recomposition.
6. **Key your `LazyColumn`/`LazyRow` items explicitly** (`key = { it.id }`) in the OS Architecture stack and any list — required for correct recomposition and animation identity across scroll.
7. **No nested scrollables without an explicit `nestedScroll` connection contract.** The custom scroll-to-seek behavior on the Teardown screen must implement `NestedScrollConnection` deliberately (don't rely on default Compose scroll nesting behavior, which will fight the custom seek-driving logic).
8. **Preview every screen-level composable** with `@Preview` + fake `UiState` fixtures — required before a PR is considered complete (see tracker.md).
9. **All colors/type/shape come from the theme module** (design.md tokens) — no hardcoded hex values or `sp`/`dp` literals in feature code.
10. **Accessibility is not optional:** every interactive element (circular buttons, cards, pager) needs `contentDescription` / `semantics {}`, minimum 48dp touch targets, and the Syscall Simulator specifically must be TalkBack-navigable in logical step order.

## 4. Code Organization
app/
core/
theme/ // colors, typography, shapes — design.md tokens live here
ui-components/ // CircularIconButton, WhiteBottomSheet, etc.
data/
manifest/ // JSON models + repository
datastore/
feature/
hub/
teardown/
transition/
osstack/
syscallsim/
navigation/
Each `feature/*` module contains its own `UiState`, `ViewModel`, and screen composable(s) only — no cross-feature imports except through `core`/`data`.