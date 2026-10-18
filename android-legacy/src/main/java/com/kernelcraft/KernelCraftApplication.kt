package com.kernelcraft

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.StrictMode
import android.util.Log
import com.kernelcraft.core.security.runtime.RuntimeSecurityPolicy
import com.kernelcraft.core.security.storage.EphemeralStorageManager

/**
 * Base Application class for KernelCraft enforcing platform-level MASVS-PLATFORM,
 * MASVS-STORAGE, and MASVS-PRIVACY compliance policies.
 */
class KernelCraftApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        configureStrictMode()
    }

    /**
     * Configures StrictMode VM policy to detect unsafe IPC, intent hijacking, and file leaks.
     */
    private fun configureStrictMode() {
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectFileUriExposure()
            .detectContentUriWithoutPermission()
            .penaltyLog()

        // Android 12+ (API 31): Detect unsafe intent launches (catches task hijacking & intent redirection)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vmPolicyBuilder.detectUnsafeIntentLaunch()
        }

        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            // In debug builds, log and alert
            vmPolicyBuilder.penaltyDropBox()
        }

        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    /**
     * Enforces aggressive cache and memory trimming when app enters background or system is low on RAM.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // App UI is hidden; scrub ephemeral decode caches and sanitize memory
            RuntimeSecurityPolicy.scrubMemory()
            EphemeralStorageManager.cleanEphemeralCache(this)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        RuntimeSecurityPolicy.scrubMemory()
        EphemeralStorageManager.cleanEphemeralCache(this)
    }
}
