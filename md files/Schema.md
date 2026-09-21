# schema.md — State Management Models

## 1. Track Content (loaded from bundled JSON manifest)

```kotlin
@Serializable
data class TrackManifest(
    val trackId: String,
    val title: String,
    val subtitle: String,
    val accentColor: String,          // hex, e.g. "#B0223A" — used for Hub card + Teardown/OS backgrounds
    val teardownVideoAsset: String,    // e.g. "raw/teardown_chip01"
    val teardownVideoDurationUs: Long,
    val chipHighlightStillAsset: String,
    val chapters: List<TeardownChapter>,
    val chipHighlightNormalizedOffset: NormalizedOffset, // where on-screen the chip sits at progress ~0.9
    val osLayers: List<OsLayerContent>,
    val syscallSteps: List<SyscallStepContent>
)

@Serializable
data class TeardownChapter(
    val id: String,
    val progressStart: Float,   // 0f..1f
    val progressEnd: Float,     // 0f..1f
    val caption: String
)

@Serializable
data class NormalizedOffset(val x: Float, val y: Float) // 0f..1f relative to viewport

@Serializable
data class OsLayerContent(
    val id: String,             // "hardware" | "kernel" | "drivers" | "syscall_interface" | "os_services" | "userspace"
    val title: String,
    val body: String,
    val keyTerms: List<String>,
    val backgroundColorHex: String,
    val illustrationAsset: String,
    val hasSimulatorCta: Boolean = false
)

@Serializable
data class SyscallStepContent(
    val stepIndex: Int,         // 0..4
    val title: String,
    val body: String,
    val mode: ExecutionMode,    // USER | KERNEL
    val diagramAsset: String
)

enum class ExecutionMode { USER, KERNEL }
```

## 2. Screen UI States (ViewModel-exposed via `StateFlow<T>`)

```kotlin
// Selection Hub
data class TrackSummary(
    val trackId: String,
    val title: String,
    val subtitle: String,
    val accentColorHex: String
)

data class HubUiState(
    val tracks: List<TrackSummary> = emptyList(),
    val isLoading: Boolean = true
)

// Hardware Teardown
enum class ChapterId { INTACT, COVER_OFF, MOTHERBOARD, CHIP_HIGHLIGHT, DIVE_PROMPT }

data class TeardownUiState(
    val trackId: String,
    val scrollProgress: Float = 0f,          // 0f..1f, single source of truth
    val currentChapter: TeardownChapter? = null,
    val videoDurationUs: Long = 0L,
    val isBuffering: Boolean = false,
    val showDiveInCta: Boolean = false        // true once scrollProgress >= 0.9
)

// Dive-In Transition (self-contained animation state, not scroll-derived)
data class TransitionUiState(
    val animationProgress: Float = 0f,        // 0f..1f, driven by Animatable in a LaunchedEffect
    val isComplete: Boolean = false
)

// OS Architecture Parallax Stack
data class OsStackUiState(
    val trackId: String,
    val layers: List<OsLayerContent> = emptyList(),
    val scrollOffsetPx: Float = 0f,
    val activeLayerIndex: Int = 0
)

// Syscall Simulator — the core interactive state machine
data class SyscallSimUiState(
    val trackId: String,
    val steps: List<SyscallStepContent> = emptyList(),
    val currentStep: Int = 0,                 // 0..4
    val executionMode: ExecutionMode = ExecutionMode.USER,
    val isComplete: Boolean = false
) {
    val canGoBack: Boolean get() = currentStep > 0
    val canGoNext: Boolean get() = currentStep < steps.lastIndex
}
```

## 3. Syscall Simulator — State Flow / Transition Table

| Step | Name | executionMode | Trigger to enter | Trigger to next |
|---|---|---|---|---|
| 0 | User code calls `write()` | USER | Screen entry | Tap "Next" / swipe |
| 1 | libc wraps the call | USER | Step 0 → Next | Tap "Next" / swipe |
| 2 | Trap / mode switch | USER→KERNEL (animated flip) | Step 1 → Next | Tap "Next" / swipe |
| 3 | Kernel executes handler | KERNEL | Step 2 → Next | Tap "Next" / swipe |
| 4 | Return to userspace | KERNEL→USER (animated flip) | Step 3 → Next | Tap "Finish" → `isComplete = true` |

**Reducer-style transitions:**
```kotlin
sealed interface SyscallSimEvent {
    data object Next : SyscallSimEvent
    data object Back : SyscallSimEvent
    data object Restart : SyscallSimEvent
}

fun reduce(state: SyscallSimUiState, event: SyscallSimEvent): SyscallSimUiState = when (event) {
    is SyscallSimEvent.Next -> if (state.canGoNext) {
        state.copy(
            currentStep = state.currentStep + 1,
            executionMode = state.steps[state.currentStep + 1].mode
        )
    } else state.copy(isComplete = true)
    is SyscallSimEvent.Back -> if (state.canGoBack) {
        state.copy(
            currentStep = state.currentStep - 1,
            executionMode = state.steps[state.currentStep - 1].mode
        )
    } else state
    is SyscallSimEvent.Restart -> state.copy(currentStep = 0, executionMode = ExecutionMode.USER, isComplete = false)
}
```

## 4. Persistence (DataStore keys)
```kotlin
object PrefsKeys {
    val LAST_TRACK_ID = stringPreferencesKey("last_track_id")
    fun trackCompletedKey(trackId: String) = booleanPreferencesKey("completed_$trackId")
}
```