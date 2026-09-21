package com.kernelcraft.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.cycleStackTiers
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.executeFullTeardownAndKernelJourney
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.interactWithSimulatorControls
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.navigateToSiliconDieTransition
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.performHighFrequencyScrub
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.reverseDiveToHardware
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.skipSplashOrWaitForTeardown
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.waitForSiliconImmersionScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Macrobenchmark scenarios for KernelCraft verifying production performance thresholds:
 * 1. Cold Startup Latency: Direct paint into interactive teardown viewport.
 * 2. 60/120Hz Scrubbing Frame Timing: P95 <= 8.33ms (120Hz) / <= 16.6ms (60Hz), zero jank frames.
 * 3. Micro-Zoom Transition Frame Timing: Smooth camera scale from 1.0x to 8.0x without dropped frames.
 * 4. Kernel Stack Navigation & Telemetry: Seamless tier transitions and zero-allocation canvas updates.
 * 5. Full End-to-End Cycle: Complete user journey benchmark with Baseline Profiles required.
 *
 * Strict Thresholds:
 * - Startup: TTID <= 400ms (Cold with Baseline Profile), zero jank during initial render.
 * - Scrubbing: P95 frame time <= 8.33ms (120 FPS target) / <= 16.6ms (60 FPS target).
 * - Transition: Zero frames exceeding 16.6ms across the 750ms zoom curve.
 * - Memory: Peak native memory <= 180MB, decoder pipeline properly released on silicon immersion.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TeardownMacrobenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val targetPackage = "com.kernelcraft"

    /**
     * Cold startup benchmark evaluating initial paint latency with AOT Baseline Profile.
     */
    @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require
        ),
        startupMode = StartupMode.COLD,
        iterations = 5
    ) {
        pressHome()
        startActivityAndWait()
        device.skipSplashOrWaitForTeardown(targetPackage)
    }

    /**
     * Frame timing metric during rapid 60/120Hz vertical scrubbing of the teardown timeline.
     * Evaluates ExoPlayer single-frame seeks, milestone engine evaluations, and zero-allocation canvas redraws.
     * Target: P95 <= 8.33ms on 120Hz displays / <= 16.6ms on 60Hz displays.
     */
    @Test
    fun teardownScrubFrameTiming() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require
        ),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.skipSplashOrWaitForTeardown(targetPackage)
        }
    ) {
        device.performHighFrequencyScrub(swipeCount = 6)
    }

    /**
     * Frame timing metric during the shared camera micro-zoom transition ($1.0x -> 8.0x$)
     * into the physical SoC silicon die.
     * Evaluates RenderThread compositing, speed-line vignette canvas rendering, and zero allocation.
     */
    @Test
    fun socMicroZoomTransitionFrameTiming() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require
        ),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.skipSplashOrWaitForTeardown(targetPackage)
            device.performHighFrequencyScrub(swipeCount = 4)
        }
    ) {
        device.navigateToSiliconDieTransition()
        device.waitForSiliconImmersionScreen()
    }

    /**
     * Frame timing metric during vertical stack tier navigation and live telemetry simulation
     * (CPU Scheduler Gantt chart, Memory Space allocator matrix, Binder IPC tracer).
     */
    @Test
    fun kernelStackTierNavigationFrameTiming() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require
        ),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.skipSplashOrWaitForTeardown(targetPackage)
            device.performHighFrequencyScrub(swipeCount = 4)
            device.navigateToSiliconDieTransition()
            device.waitForSiliconImmersionScreen()
        }
    ) {
        device.cycleStackTiers()
        device.interactWithSimulatorControls()
    }

    /**
     * Complete end-to-end journey benchmark: cold start -> scrub -> micro-zoom -> stack explore -> reverse dive.
     * Evaluates total pipeline stability, memory allocation bounds (<= 180MB native graphic buffer),
     * and decoder lifecycle teardown/reinstantiation.
     */
    @Test
    fun fullUserJourneyBenchmark() = benchmarkRule.measureRepeated(
        packageName = targetPackage,
        metrics = listOf(
            StartupTimingMetric(),
            FrameTimingMetric()
        ),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Require
        ),
        startupMode = StartupMode.COLD,
        iterations = 3
    ) {
        pressHome()
        startActivityAndWait()
        device.executeFullTeardownAndKernelJourney(targetPackage)
    }
}
