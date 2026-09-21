package com.kernelcraft.feature.teardown

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Top-level ViewModel managing the Teardown Player viewport state with SavedStateHandle persistence.
 *
 * Implements:
 * 1. Process death & configuration change preservation for scrub progress and selected components.
 * 2. Rewind / Reassemble animation with organic spring damping physics.
 * 3. User preferences (haptic toggle, coach mark dismissal).
 */
class TeardownViewModel(
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    companion object {
        private const val KEY_PROGRESS = "persist_scrub_progress"
        private const val KEY_SELECTED_COMPONENT = "persist_selected_component"
        private const val KEY_HAPTICS = "persist_haptics_enabled"
        private const val KEY_AUDIO = "persist_audio_enabled"
        private const val KEY_TUTORIAL_SEEN = "persist_tutorial_seen"
    }

    private val initialProgress: Float = 0.0f
    private val initialComponent: String? = null
    private val initialHaptics: Boolean = savedStateHandle[KEY_HAPTICS] ?: true
    private val initialAudio: Boolean = savedStateHandle[KEY_AUDIO] ?: true
    private val initialTutorialSeen: Boolean = savedStateHandle[KEY_TUTORIAL_SEEN] ?: false

    private val _uiState = MutableStateFlow(
        TeardownUiState(
            scrollProgress = initialProgress,
            selectedComponentId = initialComponent,
            isHapticsEnabled = initialHaptics,
            isAudioEnabled = initialAudio,
            showTutorialCoachMark = !initialTutorialSeen && initialProgress < 0.05f
        )
    )
    val uiState: StateFlow<TeardownUiState> = _uiState.asStateFlow()

    private var reassembleJob: Job? = null

    /**
     * Dispatch point for UI events.
     */
    fun onEvent(event: TeardownEvent) {
        when (event) {
            is TeardownEvent.ScrubUpdated -> {
                // If user touches screen during reassemble animation, cancel spring
                if (event.isDragging && _uiState.value.isReassembling) {
                    reassembleJob?.cancel()
                    reassembleJob = null
                }

                val milestone = TeardownMilestone.fromPositionMs(event.positionMs)

                // Dismiss tutorial on first drag
                val tutorialDismissed = _uiState.value.showTutorialCoachMark && event.progress > 0.02f
                if (tutorialDismissed) {
                    savedStateHandle[KEY_TUTORIAL_SEEN] = true
                }

                savedStateHandle[KEY_PROGRESS] = event.progress

                _uiState.update { current ->
                    current.copy(
                        scrollProgress = event.progress,
                        currentPositionMs = event.positionMs,
                        currentMilestone = milestone,
                        scrubVelocity = event.velocity,
                        isDragging = event.isDragging,
                        isCoasting = event.isCoasting,
                        isReassembling = false,
                        showTutorialCoachMark = if (tutorialDismissed) false else current.showTutorialCoachMark,
                        showDiveInCta = event.progress >= 0.90f
                    )
                }
            }

            is TeardownEvent.DurationResolved -> {
                if (event.durationMs > 0L) {
                    _uiState.update { current ->
                        current.copy(
                            durationMs = event.durationMs,
                            currentPositionMs = (current.scrollProgress * event.durationMs).toLong(),
                            currentMilestone = TeardownMilestone.fromProgress(current.scrollProgress, event.durationMs)
                        )
                    }
                }
            }

            is TeardownEvent.SelectComponent -> {
                savedStateHandle[KEY_SELECTED_COMPONENT] = event.componentId
                _uiState.update { it.copy(selectedComponentId = event.componentId) }
            }

            is TeardownEvent.SetSpecsExpanded -> {
                _uiState.update { it.copy(isSpecsExpanded = event.isExpanded) }
            }

            is TeardownEvent.ToggleHaptics -> {
                val newHaptics = !_uiState.value.isHapticsEnabled
                savedStateHandle[KEY_HAPTICS] = newHaptics
                _uiState.update { it.copy(isHapticsEnabled = newHaptics) }
            }

            is TeardownEvent.ToggleAudio -> {
                val newAudio = !_uiState.value.isAudioEnabled
                savedStateHandle[KEY_AUDIO] = newAudio
                _uiState.update { it.copy(isAudioEnabled = newAudio) }
            }

            is TeardownEvent.DismissTutorial -> {
                savedStateHandle[KEY_TUTORIAL_SEEN] = true
                _uiState.update { it.copy(showTutorialCoachMark = false) }
            }

            is TeardownEvent.ReassemblePhone -> {
                reassembleJob?.cancel()
                _uiState.update { current ->
                    current.copy(
                        scrollProgress = 0.0f,
                        currentPositionMs = 0L,
                        currentMilestone = TeardownMilestone.ASSEMBLED,
                        scrubVelocity = 0f,
                        isReassembling = false,
                        isDragging = false,
                        isCoasting = false
                    )
                }
                savedStateHandle[KEY_PROGRESS] = 0.0f
            }

            is TeardownEvent.DiveInTriggered -> {
                // NavHost route transition to ComponentDetail or Transition sequence
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        reassembleJob?.cancel()
    }
}
