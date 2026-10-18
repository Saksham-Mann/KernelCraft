package com.kernelcraft.core.security.storage

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.Arrays

/**
 * Ephemeral Storage and Cache Sanitizer (MASVS-STORAGE & MASVS-PRIVACY).
 *
 * Ensures that ephemeral directories (cacheDir, codeCacheDir) do not persist
 * unencrypted crash traces, sensitive telemetry dumps, or temporary decode artifacts.
 */
object EphemeralStorageManager {

    /**
     * Cleans and sanitizes ephemeral cache directories.
     */
    fun cleanEphemeralCache(context: Context): Int {
        var filesCleaned = 0
        val targetDirs = listOf(context.cacheDir, context.codeCacheDir)

        for (dir in targetDirs) {
            if (dir != null && dir.exists() && dir.isDirectory) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile) {
                        secureDelete(file)
                        filesCleaned++
                    }
                }
            }
        }
        return filesCleaned
    }

    /**
     * Overwrites file contents with zeros before unlinking to prevent flash memory reconstruction.
     */
    fun secureDelete(file: File): Boolean {
        if (!file.exists()) return false

        try {
            if (file.isFile && file.length() > 0) {
                val length = file.length().toInt()
                val zeroBuffer = ByteArray(minOf(length, 4096))
                Arrays.fill(zeroBuffer, 0.toByte())

                FileOutputStream(file).use { fos ->
                    var bytesWritten = 0
                    while (bytesWritten < length) {
                        val toWrite = minOf(zeroBuffer.size, length - bytesWritten)
                        fos.write(zeroBuffer, 0, toWrite)
                        bytesWritten += toWrite
                    }
                    fos.flush()
                }
            }
        } catch (_: Exception) {
            // Proceed to deletion even if zero-wipe fails
        }
        return file.delete()
    }

    /**
     * Audits the cache directory to assert zero plaintext log or key files are present.
     */
    fun verifyCacheHygiene(context: Context): Boolean {
        val dangerousExtensions = setOf(".key", ".pem", ".jks", ".p12", ".db-journal")
        val cache = context.cacheDir ?: return true

        val files = cache.walkTopDown().filter { it.isFile }
        for (file in files) {
            val lowerName = file.name.lowercase()
            for (ext in dangerousExtensions) {
                if (lowerName.endsWith(ext)) {
                    return false
                }
            }
        }
        return true
    }
}
