package com.kernelcraft.core.security.distribution

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.InstallSourceInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Recognized Android application distribution channels.
 */
enum class DistributionChannel {
    GOOGLE_PLAY_STORE,
    VERIFIED_MDM,
    SIDELOADED_PACKAGE_INSTALLER,
    DIRECT_ADB_OR_UNTRACKED
}

/**
 * Result of installation origin security audit.
 */
data class InstallSourceAssessment(
    val isTrustedOrigin: Boolean,
    val channel: DistributionChannel,
    val installerPackage: String?,
    val initiatingPackage: String?,
    val originatingPackage: String?,
    val restrictHighFidelityTelemetry: Boolean,
    val securityFindings: List<String>
)

/**
 * Evaluates application installer origin and enforces licensing / anti-repackaging boundaries.
 */
object InstallSourceValidator {

    private val TRUSTED_PLAY_INSTALLERS = setOf(
        "com.android.vending",           // Google Play Store
        "com.google.android.feedback"   // Play Services
    )

    private val TRUSTED_MDM_INSTALLERS = setOf(
        "com.google.android.apps.work.clouddpc", // Android Device Policy
        "com.microsoft.windowsintune.companyportal" // Microsoft Intune
    )

    private val KNOWN_SIDELOAD_INSTALLERS = setOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.samsung.android.packageinstaller"
    )

    /**
     * Conducts installation provenance audit.
     */
    fun evaluateInstallSource(context: Context, isDebuggableBuild: Boolean = false): InstallSourceAssessment {
        val findings = mutableListOf<String>()
        var installerPkg: String? = null
        var initiatingPkg: String? = null
        var originatingPkg: String? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
                val sourceInfo: InstallSourceInfo = context.packageManager.getInstallSourceInfo(context.packageName)
                installerPkg = sourceInfo.installingPackageName
                initiatingPkg = sourceInfo.initiatingPackageName
                originatingPkg = sourceInfo.originatingPackageName
            } else {
                @Suppress("DEPRECATION")
                installerPkg = context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            findings.add("Package metadata lookup failed for installer source")
        }

        val channel = when {
            installerPkg != null && TRUSTED_PLAY_INSTALLERS.contains(installerPkg) -> {
                DistributionChannel.GOOGLE_PLAY_STORE
            }
            installerPkg != null && TRUSTED_MDM_INSTALLERS.contains(installerPkg) -> {
                DistributionChannel.VERIFIED_MDM
            }
            installerPkg != null && KNOWN_SIDELOAD_INSTALLERS.contains(installerPkg) -> {
                findings.add("Application was manually sideloaded via system package installer: $installerPkg")
                DistributionChannel.SIDELOADED_PACKAGE_INSTALLER
            }
            else -> {
                findings.add("Untracked installation source (Direct ADB push or cracked APK mirror)")
                DistributionChannel.DIRECT_ADB_OR_UNTRACKED
            }
        }

        val isTrusted = (channel == DistributionChannel.GOOGLE_PLAY_STORE || channel == DistributionChannel.VERIFIED_MDM)
        val shouldRestrict = !isTrusted && !isDebuggableBuild

        if (shouldRestrict) {
            findings.add("Restricting high-fidelity silicon die schematics and telemetry due to untrusted distribution origin")
        }

        return InstallSourceAssessment(
            isTrustedOrigin = isTrusted,
            channel = channel,
            installerPackage = installerPkg,
            initiatingPackage = initiatingPkg,
            originatingPackage = originatingPkg,
            restrictHighFidelityTelemetry = shouldRestrict,
            securityFindings = findings
        )
    }

    /**
     * Helper to classify an arbitrary installer package name.
     */
    fun classifyInstaller(installerPackage: String?): DistributionChannel {
        return when {
            installerPackage != null && TRUSTED_PLAY_INSTALLERS.contains(installerPackage) -> DistributionChannel.GOOGLE_PLAY_STORE
            installerPackage != null && TRUSTED_MDM_INSTALLERS.contains(installerPackage) -> DistributionChannel.VERIFIED_MDM
            installerPackage != null && KNOWN_SIDELOAD_INSTALLERS.contains(installerPackage) -> DistributionChannel.SIDELOADED_PACKAGE_INSTALLER
            else -> DistributionChannel.DIRECT_ADB_OR_UNTRACKED
        }
    }
}
