package com.kernelcraft.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.kernelcraft.benchmark.ScrubAndZoomUiAutomatorFlow.executeFullTeardownAndKernelJourney
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates AndroidX Baseline Profiles for KernelCraft to guide ART Ahead-Of-Time (AOT) compilation.
 *
 * Exercises the critical user journeys:
 * - Direct app startup into teardown viewport (eliminating initial render JIT stutters)
 * - 60/120Hz high-frequency vertical scrub physics & ExoPlayer seek pipelines
 * - Micro-zoom shared camera transition ($1.0x -> 8.0x$) into the SoC silicon die
 * - Kernel Stack Explorer hierarchy navigation & simulation engine loops
 * - Reverse hardware dive transition back to teardown player
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collect(
            packageName = "com.kernelcraft",
            includeInStartupProfile = true
        ) {
            // Start default MainActivity
            pressHome()
            startActivityAndWait()

            // Run the comprehensive teardown -> micro-zoom -> kernel stack journey
            device.executeFullTeardownAndKernelJourney(packageName)
        }
    }
}
