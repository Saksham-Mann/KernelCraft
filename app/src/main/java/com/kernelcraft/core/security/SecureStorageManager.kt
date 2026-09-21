package com.kernelcraft.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Hardware Keystore-backed encrypted storage manager and private filesystem isolation gatekeeper.
 *
 * Implements:
 * 1. AES-256-GCM hardware-backed encrypted key-value storage.
 * 2. Strict filesystem isolation verifying zero data leakage to external or public storage.
 * 3. Anti-path-traversal verification.
 */
object SecureStorageManager {

    private const val SECURE_PREFS_NAME = "kernelcraft_secure_prefs"

    @Volatile
    private var securePrefsInstance: SharedPreferences? = null

    /**
     * Obtains or initializes the Keystore-backed [EncryptedSharedPreferences] instance.
     */
    fun getEncryptedPreferences(context: Context): SharedPreferences {
        return securePrefsInstance ?: synchronized(this) {
            securePrefsInstance ?: run {
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    SECURE_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).also { securePrefsInstance = it }
            }
        }
    }

    /**
     * Creates a validated file descriptor strictly inside the internal private files directory.
     * Throws [SecurityException] if path traversal or external path escaping is detected.
     */
    fun getPrivateFile(context: Context, relativePath: String): File {
        validateFilenameSafety(relativePath)
        val file = File(context.filesDir, relativePath)
        assertPrivateFilesystemBoundary(context, file)
        return file
    }

    /**
     * Creates a validated cache file descriptor strictly inside the internal private cache directory.
     */
    fun getPrivateCacheFile(context: Context, relativePath: String): File {
        validateFilenameSafety(relativePath)
        val file = File(context.cacheDir, relativePath)
        assertPrivateFilesystemBoundary(context, file)
        return file
    }

    /**
     * Ensures target file is strictly confined within internal app-private sandboxed directories.
     */
    fun assertPrivateFilesystemBoundary(context: Context, targetFile: File) {
        val canonicalTarget = targetFile.canonicalPath
        val canonicalFilesDir = context.filesDir.canonicalPath
        val canonicalCacheDir = context.cacheDir.canonicalPath

        val isWithinInternalSandbox = canonicalTarget.startsWith(canonicalFilesDir) ||
                canonicalTarget.startsWith(canonicalCacheDir)

        if (!isWithinInternalSandbox) {
            throw SecurityException("Filesystem isolation violation: Path $canonicalTarget escapes internal private sandbox!")
        }
    }

    /**
     * Rejects path-traversal patterns (e.g. "../", null bytes).
     */
    fun validateFilenameSafety(filename: String) {
        if (filename.contains("..") || filename.contains("\u0000") || filename.startsWith("/")) {
            throw SecurityException("Illegal filename pattern with potential path traversal: $filename")
        }
    }
}
