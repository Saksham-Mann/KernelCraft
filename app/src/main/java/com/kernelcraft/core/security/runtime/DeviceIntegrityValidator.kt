package com.kernelcraft.core.security.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Validates hardware, OS, and filesystem integrity against rooting, Magisk, KernelSU, and APatch.
 */
object DeviceIntegrityValidator {

    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/app/Superuser.apk"
    )

    private val ROOT_DAEMON_DIRS = listOf(
        "/dev/magisk",
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/dev/ksu",
        "/data/adb/ksu",
        "/data/adb/ap",
        "/data/adb/modules"
    )

    private val ROOT_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "io.github.a13e300.ksu",
        "me.bmax.apatch",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser"
    )

    /**
     * Conducts multi-signal device compromise evaluation.
     */
    fun isRooted(context: Context? = null, findings: MutableList<String> = mutableListOf()): Boolean {
        var rooted = false

        if (checkSuBinaries(findings)) rooted = true
        if (checkRootDaemons(findings)) rooted = true
        if (checkTestKeys(findings)) rooted = true
        if (checkMountFlags(findings)) rooted = true
        if (context != null && checkRootPackages(context, findings)) rooted = true

        return rooted
    }

    /**
     * Scans standard filesystem binary locations for su executables.
     */
    fun checkSuBinaries(findings: MutableList<String> = mutableListOf()): Boolean {
        for (path in SU_PATHS) {
            try {
                if (File(path).exists()) {
                    findings.add("Root executable detected: $path")
                    return true
                }
            } catch (_: SecurityException) {
                // Restricted permissions could also hint at system changes
            }
        }
        return false
    }

    /**
     * Checks for modern Magisk, KernelSU, or APatch runtime sockets and directories.
     */
    fun checkRootDaemons(findings: MutableList<String> = mutableListOf()): Boolean {
        for (dirPath in ROOT_DAEMON_DIRS) {
            try {
                val file = File(dirPath)
                if (file.exists()) {
                    findings.add("Modern root manager daemon directory detected: $dirPath")
                    return true
                }
            } catch (_: SecurityException) {
                // Ignore
            }
        }
        return false
    }

    /**
     * Checks if OS build was signed with unofficial test-keys.
     */
    fun checkTestKeys(findings: MutableList<String> = mutableListOf()): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            findings.add("System build signed with test-keys (custom ROM indicator)")
            return true
        }
        return false
    }

    /**
     * Reads `/proc/mounts` to verify that system partitions remain strictly read-only (`ro`).
     */
    fun checkMountFlags(findings: MutableList<String> = mutableListOf()): Boolean {
        return try {
            val mountsFile = File("/proc/mounts")
            if (!mountsFile.exists() || !mountsFile.canRead()) return false

            var dangerousRemount = false
            BufferedReader(FileReader(mountsFile)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    val tokens = line.split(" ")
                    if (tokens.size >= 4) {
                        val mountPoint = tokens[1]
                        val options = tokens[3].split(",")
                        val isRw = options.contains("rw")

                        // Critical system partitions must NEVER be mounted as read-write
                        if (isRw && (mountPoint == "/system" || mountPoint == "/vendor" || mountPoint == "/system_ext")) {
                            findings.add("System partition remounted as read-write (rw): $mountPoint")
                            dangerousRemount = true
                            break
                        }
                    }
                    line = reader.readLine()
                }
            }
            dangerousRemount
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Detects known root manager application packages on the device.
     */
    fun checkRootPackages(context: Context, findings: MutableList<String> = mutableListOf()): Boolean {
        val pm = context.packageManager
        for (pkg in ROOT_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                findings.add("Known root manager package installed: $pkg")
                return true
            } catch (_: PackageManager.NameNotFoundException) {
                // Expected when clean
            }
        }
        return false
    }
}
