package com.kernelcraft.core.security.distribution

import android.app.PendingIntent
import com.kernelcraft.core.security.privacy.SensitiveContentManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated unit tests verifying distribution security, installer provenance classification,
 * telemetry restriction policies, and pending intent immutability.
 */
class InstallSourceValidatorTest {

    @Test
    fun `classifyInstaller identifies authentic Google Play Store installer`() {
        val channelVending = InstallSourceValidator.classifyInstaller("com.android.vending")
        val channelFeedback = InstallSourceValidator.classifyInstaller("com.google.android.feedback")

        assertEquals(DistributionChannel.GOOGLE_PLAY_STORE, channelVending)
        assertEquals(DistributionChannel.GOOGLE_PLAY_STORE, channelFeedback)
    }

    @Test
    fun `classifyInstaller identifies verified Enterprise MDM channels`() {
        val channelMdm = InstallSourceValidator.classifyInstaller("com.google.android.apps.work.clouddpc")
        val channelIntune = InstallSourceValidator.classifyInstaller("com.microsoft.windowsintune.companyportal")

        assertEquals(DistributionChannel.VERIFIED_MDM, channelMdm)
        assertEquals(DistributionChannel.VERIFIED_MDM, channelIntune)
    }

    @Test
    fun `classifyInstaller identifies manual package installer sideloading`() {
        val channelSideload = InstallSourceValidator.classifyInstaller("com.google.android.packageinstaller")
        val channelAosp = InstallSourceValidator.classifyInstaller("com.android.packageinstaller")

        assertEquals(DistributionChannel.SIDELOADED_PACKAGE_INSTALLER, channelSideload)
        assertEquals(DistributionChannel.SIDELOADED_PACKAGE_INSTALLER, channelAosp)
    }

    @Test
    fun `classifyInstaller categorizes untracked null or adb sources`() {
        val channelNull = InstallSourceValidator.classifyInstaller(null)
        val channelEmpty = InstallSourceValidator.classifyInstaller("")
        val channelRandom = InstallSourceValidator.classifyInstaller("com.cracked.apkpure")

        assertEquals(DistributionChannel.DIRECT_ADB_OR_UNTRACKED, channelNull)
        assertEquals(DistributionChannel.DIRECT_ADB_OR_UNTRACKED, channelEmpty)
        assertEquals(DistributionChannel.DIRECT_ADB_OR_UNTRACKED, channelRandom)
    }

    @Test
    fun `isPendingIntentImmutable enforces strict FLAG_IMMUTABLE presence`() {
        val immutableFlag = PendingIntent.FLAG_IMMUTABLE
        val combinedImmutableFlag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val unsafeMutableFlag = PendingIntent.FLAG_MUTABLE
        val zeroFlags = 0

        assertTrue("FLAG_IMMUTABLE must pass", SensitiveContentManager.isPendingIntentImmutable(immutableFlag))
        assertTrue("Combined flags with FLAG_IMMUTABLE must pass", SensitiveContentManager.isPendingIntentImmutable(combinedImmutableFlag))
        assertFalse("Unsafe mutable flag without immutable bit must fail", SensitiveContentManager.isPendingIntentImmutable(unsafeMutableFlag))
        assertFalse("Zero flags must fail", SensitiveContentManager.isPendingIntentImmutable(zeroFlags))
    }

    @Test
    fun `telemetry restriction policy protects high-fidelity silicon models on untrusted builds`() {
        fun shouldRestrictTelemetry(channel: DistributionChannel, isDebug: Boolean): Boolean {
            val isTrusted = (channel == DistributionChannel.GOOGLE_PLAY_STORE || channel == DistributionChannel.VERIFIED_MDM)
            return !isTrusted && !isDebug
        }

        // Production builds from Play Store -> Full telemetry accessible
        assertFalse(shouldRestrictTelemetry(DistributionChannel.GOOGLE_PLAY_STORE, isDebug = false))

        // Production builds sideloaded -> Telemetry restricted/watermarked
        assertTrue(shouldRestrictTelemetry(DistributionChannel.SIDELOADED_PACKAGE_INSTALLER, isDebug = false))
        assertTrue(shouldRestrictTelemetry(DistributionChannel.DIRECT_ADB_OR_UNTRACKED, isDebug = false))

        // Debug builds during engineering testing -> Permitted
        assertFalse(shouldRestrictTelemetry(DistributionChannel.DIRECT_ADB_OR_UNTRACKED, isDebug = true))
    }
}
