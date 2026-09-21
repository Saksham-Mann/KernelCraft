package com.kernelcraft.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

/**
 * Deterministic UiAutomator actions automating the complete KernelCraft end-to-end journey:
 * 1. Cold startup past brand reveal splash into interactive teardown viewport.
 * 2. High-frequency 60/120Hz vertical scrubbing physics loop into peak disassembly frame (~00:04.800).
 * 3. Bounding reticle / CTA detection & click ("Inspect Silicon Architecture").
 * 4. Micro-zoom transition ($1.0x -> 8.0x$) into the silicon die and OS stack explorer.
 * 5. Vertical tier cycling across all 4 OS architecture layers.
 * 6. Live telemetry interactions (memory stress & CPU simulation).
 * 7. Reverse hardware dive ("Zoom Out to Hardware") back to the teardown player.
 */
object ScrubAndZoomUiAutomatorFlow {

    private const val DEFAULT_TIMEOUT_MS = 5000L

    /**
     * Handles initial startup: dismisses splash if present or waits for the teardown
     * viewport to render.
     */
    fun UiDevice.skipSplashOrWaitForTeardown(targetPackage: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        wait(Until.hasObject(By.pkg(targetPackage).depth(0)), timeoutMs)

        // If splash brand text is present, tap to immediately advance
        val splashText = findObject(By.text("KERNELCRAFT"))
        if (splashText != null) {
            splashText.click()
        }

        // Wait for teardown viewport to stabilize
        waitForIdle(1000)
    }

    /**
     * Simulates high-frequency vertical dragging / flinging to advance video playback
     * to the exploded disassembly milestone (~00:04.800).
     */
    fun UiDevice.performHighFrequencyScrub(swipeCount: Int = 4) {
        val centerX = displayWidth / 2
        val startY = (displayHeight * 0.75f).toInt()
        val endY = (displayHeight * 0.25f).toInt()

        repeat(swipeCount) {
            // Rapid vertical swipe with few steps (high velocity fling)
            swipe(centerX, startY, centerX, endY, 12)
            waitForIdle(350)
        }
    }

    /**
     * Locates and clicks the SoC reticle callout ("Inspect Silicon Architecture")
     * or the bottom dock CTA button ("Dive into SoC Architecture →").
     */
    fun UiDevice.navigateToSiliconDieTransition(timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        // First try the reticle annotation callout
        val socReticle = wait(
            Until.findObject(By.textContains("Inspect Silicon Architecture")),
            timeoutMs
        ) ?: findObject(By.textContains("Snapdragon"))
          ?: findObject(By.textContains("Dive into SoC Architecture"))

        if (socReticle != null) {
            socReticle.click()
        } else {
            // Fallback: tap the physical SoC chip area near normalized (0.46, 0.32)
            val chipX = (displayWidth * 0.46f).toInt()
            val chipY = (displayHeight * 0.32f).toInt()
            click(chipX, chipY)
        }

        waitForIdle(1200)
    }

    /**
     * Awaits the completion of the micro-zoom transition into the Kernel Stack Explorer.
     */
    fun UiDevice.waitForSiliconImmersionScreen(timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        wait(
            Until.findObject(By.textContains("Zoom Out to Hardware")),
            timeoutMs
        )
    }

    /**
     * Cycles through all 4 hierarchical OS stack tiers in the Kernel Stack Explorer:
     * - Tier 0: User Apps: Android & Jetpack Compose
     * - Tier 1: Android Framework: API & Runtime (ART)
     * - Tier 2: Hardware Abstraction Layer (HAL)
     * - Tier 3: Linux Kernel: Process Scheduler & Device Drivers
     */
    fun UiDevice.cycleStackTiers() {
        val tiers = listOf(
            "User Apps",
            "Android Framework",
            "Hardware Abstraction",
            "Linux Kernel"
        )

        for (tierKeyword in tiers) {
            val tierTab = findObject(By.textContains(tierKeyword))
            if (tierTab != null) {
                tierTab.click()
                waitForIdle(400)
            }
        }
    }

    /**
     * Interacts with live simulation triggers (Memory Space Allocator workloads).
     */
    fun UiDevice.interactWithSimulatorControls() {
        // Test Memory Stress buttons
        val appLaunchBtn = findObject(By.text("App Launch"))
        appLaunchBtn?.click()
        waitForIdle(300)

        val heavyTaskBtn = findObject(By.text("Heavy Task"))
        heavyTaskBtn?.click()
        waitForIdle(300)

        val trimMemoryBtn = findObject(By.text("Trim Memory"))
        trimMemoryBtn?.click()
        waitForIdle(300)
    }

    /**
     * Triggers the reverse hardware dive back to the teardown screen.
     */
    fun UiDevice.reverseDiveToHardware(timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        val zoomOutBtn = wait(
            Until.findObject(By.textContains("Zoom Out to Hardware")),
            timeoutMs
        )
        if (zoomOutBtn != null) {
            zoomOutBtn.click()
            waitForIdle(1500)
        }
    }

    /**
     * Executes the complete end-to-end journey in one deterministic sequence.
     */
    fun UiDevice.executeFullTeardownAndKernelJourney(targetPackage: String) {
        skipSplashOrWaitForTeardown(targetPackage)
        performHighFrequencyScrub(swipeCount = 4)
        navigateToSiliconDieTransition()
        waitForSiliconImmersionScreen()
        cycleStackTiers()
        interactWithSimulatorControls()
        reverseDiveToHardware()
    }
}
