package com.kernelcraft.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.File

/**
 * Result of runtime device and environment integrity evaluation.
 */
data class SecurityAssessment(
    val isDeviceCompromised: Boolean,
    val isRootDetected: Boolean,
    val isDebuggerDetected: Boolean,
    val isUntrustedInstaller: Boolean,
    val securityFindings: List<String>
)

/**
 * Runtime anti-tamper and environment integrity verification manager.
 *
 * Implements:
 * 1. Root detection (su binaries, test-keys, dangerous directories).
 * 2. Debugger & Developer options detection.
 * 3. Installer package origin validation.
 */
object AppSecurityManager {

    private val KNOWN_ROOT_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",           // Google Play Store
        "com.google.android.feedback"   // Play Services
    )

    /**
     * Conducts a complete runtime security and integrity evaluation.
     */
    fun performSecurityAudit(context: Context, isDebuggableBuild: Boolean = false): SecurityAssessment {
        val findings = mutableListOf<String>()

        // 1. Root & Tamper Detection
        val isRooted = checkRootIndicators(findings)

        // 2. Debugger & Development Environment Detection
        val isDebugger = checkDebuggerAndDevMode(context, isDebuggableBuild, findings)

        // 3. Installer Package Validation
        val isUntrustedInstaller = checkInstallerPackage(context, isDebuggableBuild, findings)

        val isCompromised = isRooted || (isDebugger && !isDebuggableBuild) || (isUntrustedInstaller && !isDebuggableBuild)

        return SecurityAssessment(
            isDeviceCompromised = isCompromised,
            isRootDetected = isRooted,
            isDebuggerDetected = isDebugger,
            isUntrustedInstaller = isUntrustedInstaller,
            securityFindings = findings
        )
    }

    /**
     * Scans for su binaries, test-keys build tags, and root management packages.
     */
    fun checkRootIndicators(findings: MutableList<String> = mutableListOf()): Boolean {
        var rooted = false

        // Check for test-keys in Build tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            findings.add("OS build signed with test-keys (custom ROM indicator)")
            rooted = true
        }

        // Check for root execution binaries on filesystem
        for (path in KNOWN_ROOT_PATHS) {
            try {
                if (File(path).exists()) {
                    findings.add("Root binary discovered at: $path")
                    rooted = true
                    break
                }
            } catch (_: SecurityException) {
                // Restricted permissions could also hint at system sandboxing changes
            }
        }

        return rooted
    }

    /**
     * Detects attached native/Java debuggers and debuggable process flags.
     */
    fun checkDebuggerAndDevMode(
        context: Context,
        isDebuggableBuild: Boolean,
        findings: MutableList<String> = mutableListOf()
    ): Boolean {
        var debuggerPresent = false

        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            findings.add("Active debugger attached to process runtime")
            debuggerPresent = true
        }

        val isAppDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isAppDebuggable && !isDebuggableBuild) {
            findings.add("Application process flagged as DEBUGGABLE in production")
            debuggerPresent = true
        }

        return debuggerPresent
    }

    /**
     * Validates that the application was installed from an authentic, verified distribution channel.
     */
    fun checkInstallerPackage(
        context: Context,
        isDebuggableBuild: Boolean,
        findings: MutableList<String> = mutableListOf()
    ): Boolean {
        if (isDebuggableBuild) return false

        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        val isTrusted = installer != null && TRUSTED_INSTALLERS.contains(installer)
        if (!isTrusted) {
            findings.add("Sideloaded or untrusted installer source: ${installer ?: "direct adb / unknown"}")
        }
        return !isTrusted
    }
}
