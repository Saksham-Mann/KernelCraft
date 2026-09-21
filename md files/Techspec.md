# techspec.md — Technical Specification

## 1. Platform & Baseline
- **Language:** Kotlin, 100% Jetpack Compose (no XML layouts except the unavoidable `PlayerView` bridge).
- **Min SDK:** 29 (Android 10) — required for reliable `MediaCodec` seek performance; blur `RenderEffect` (API 31+) gated behind a feature check with a graceful alpha-only fallback below 31.
- **Target/Compile SDK:** latest stable.
- **Architecture pattern:** MVI-lite — unidirectional `UiState` data classes (see schema.md) held in `ViewModel`s, exposed via `StateFlow`, consumed via `collectAsStateWithLifecycle()`.

## 2. Scroll-Scrubbed Video (Hardware Teardown) — Core Technical Challenge

### 2.1 Why All-Intra Encoding Is Mandatory
Standard video codecs (H.264/H.265 with GOP structures of I/P/B frames) require decoding a chain of preceding frames to render an arbitrary frame — seeking to frame 500 may require decoding frames 480–500 first. This makes scroll-scrubbing (which demands **random-access, low-latency seeks** on every scroll delta) stutter badly or decode-storm the CPU.

**All-Intra encoding** (every frame is an I-frame / keyframe) makes every frame independently decodable — `seekTo(timestamp)` is near O(1) regardless of position. This is the same principle video editors use for scrubbing timelines.

### 2.2 Encoding Requirements
- Codec: H.264 High Profile, **all-intra** (`-g 1` in ffmpeg terms — GOP size of 1).
- Resolution: cap at 1080p (do not exceed device-target resolution; upscale in the compositor if needed, not in the source).
- Bitrate: All-intra inflates file size significantly vs. inter-frame encoding — budget accordingly (a 15s all-intra 1080p clip may be 40–80MB vs. ~5MB with standard GOP). Mitigate via:
  - Keep teardown clips short (10–20s of source footage is plenty given scroll maps the whole range).
  - Use CRF-based quality targeting (e.g., CRF 20–23) rather than fixed high bitrate.
  - Consider downscaling to 720p for the teardown clip specifically if 1080p all-intra size is prohibitive — the video is a backdrop, not the focal reading content.
- Container: MP4/fMP4, audio track stripped entirely (silent scrubbing — no audio sync burden).
- Delivery: bundle in `res/raw` or app-bundle asset pack if size grows large per track (>15MB → move to a Play Asset Delivery on-demand pack per track to avoid bloating base APK).

### 2.3 ExoPlayer Integration
```kotlin
val exoPlayer = remember {
    ExoPlayer.Builder(context)
        .setSeekParameters(SeekParameters.EXACT) // all-intra makes EXACT cheap
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = false // scroll drives playback, not autoplay
            prepare()
        }
}
```
- `AndroidView { PlayerView(context).apply { player = exoPlayer; useController = false; resizeMode = RESIZE_MODE_ZOOM } }`.
- **Seek throttling:** Do not call `seekTo` on every raw scroll pixel. Use a `snapshotFlow { scrollProgress }` → `sample(16.millis)` (roughly one frame at 60fps) or a `MutableSharedFlow` with `conflate()` → collect in a `LaunchedEffect` and call `seekTo`. This coalesces bursty scroll deltas into at most one seek per frame.
- Release the player in `DisposableEffect { onDispose { exoPlayer.release() } }` — critical, since a leaked `ExoPlayer`/`MediaCodec` instance is a common OOM source.

### 2.4 Memory Management (OOM Avoidance)
- **One `ExoPlayer` instance alive at a time.** Never keep the Teardown screen's player instantiated once the user has navigated to OS Architecture — release on `onDispose`.
- Decoder buffer tuning: use `DefaultLoadControl` with reduced buffer-ahead targets (`setBufferDurationsMs` lowered — since we scrub rather than play linearly, large forward-buffering is wasted memory).
- Avoid `Bitmap` extraction from every frame — no `MediaMetadataRetriever` frame-grabbing loops. Let `MediaCodec` (via ExoPlayer) render directly to the `SurfaceView`/`TextureView` GPU surface; never round-trip frames through app-side `Bitmap`s.
- For the Dive-In Transition's "last frame → static image" handoff (Screen 3), pre-bundle a matching static JPEG/WebP asset for that exact chip-highlight moment rather than programmatically capturing the video's current frame — this avoids `PixelCopy`/surface-capture memory spikes entirely.

## 3. Zoom Transition (`graphicsLayer`)
- Use a single `Modifier.graphicsLayer { ... }` block per animated element, reading animated values from `Animatable<Float>`/`Animatable<Offset>` state — avoids the invalidation cost of animating layout-affecting modifiers (`Modifier.scale`/`Modifier.offset` outside `graphicsLayer` trigger relayout; `graphicsLayer` properties are draw-phase only and far cheaper).
- Blur via `RenderEffect.createBlurEffect()` wrapped in `Modifier.graphicsLayer { renderEffect = ... }`, API 31+ only; fallback to a pure alpha/scale crossfade below API 31 (no blur).

## 4. Parallax OS Stack
- Built on `LazyColumn` with a shared scroll-offset source of truth (`LazyListState.firstVisibleItemScrollOffset` combined with `firstVisibleItemIndex` and item heights, or a custom `Modifier.onGloballyPositioned` accumulator for cross-item continuous offset).
- Each layer's background parallax element reads offset via `derivedStateOf { listState.calculateOffset() * parallaxFactor }` — `derivedStateOf` is essential here to prevent every pixel of scroll from triggering full recomposition of layer content that hasn't actually changed.
- Background parallax layers are drawn via `graphicsLayer { translationY = ... }`, not re-laid-out, for the same draw-phase-only cost reason as Section 3.

## 5. Lottie Usage
- Reserved for **small, self-contained iconographic animations** only (e.g., the circular "+/–" quantity steppers, a checkmark on syscall step completion, a subtle chip "pulse" glow). 
- **Not** used for the teardown sequence (that's video) or full-screen transitions (that's `graphicsLayer`).
- Lottie files kept under ~50KB each; loaded via `com.airbnb.android:lottie-compose`, with `LottieCompositionSpec.RawRes` and `iterations = 1` unless explicitly looping (e.g., idle pulse).

## 6. State/Data Layer
- No backend required for v1 — all track content (video URIs, captions, layer text, syscall step definitions) ships as bundled JSON manifests parsed via `kotlinx.serialization`, loaded once at Hub launch.
- Persistence (progress tracking, last-visited track) via `DataStore<Preferences>`, not shared prefs directly.

## 7. Testing Strategy
- Macrobenchmark (`androidx.benchmark.macro`) on the Hardware Teardown screen specifically to catch frame-drop regressions during scroll-scrubbing on CI, using a captured scroll-input trace.
- Compose UI tests for the Syscall Simulator's step state machine (pure state, easy to test deterministically).
- Manual device matrix: 1 low-tier (4GB RAM, API 29), 1 mid-tier, 1 high-tier for the video scrubbing screen specifically.