package com.kernelcraft.core.security

import android.content.Intent

/**
 * Validates and sanitizes incoming Intents received by exported Android components.
 *
 * Implements:
 * 1. Action filtering against permitted launcher and view actions.
 * 2. Stripping unsafe URI grant permission flags (FLAG_GRANT_READ_URI_PERMISSION / FLAG_GRANT_WRITE_URI_PERMISSION).
 * 3. Mitigation against Intent redirection / nested intent attacks (EXTRA_INTENT).
 */
object IntentSanitizer {

    private val ALLOWED_ACTIONS = setOf(
        Intent.ACTION_MAIN,
        Intent.ACTION_VIEW
    )

    private val ALLOWED_DATA_SCHEMES = setOf(
        "https",
        "android.resource",
        "content"
    )

    private const val MAX_STRING_EXTRA_LENGTH = 4096

    /**
     * Inspects an incoming Intent and returns a sanitized copy if safe, or null if malicious/invalid.
     */
    fun sanitize(intent: Intent?): Intent? {
        if (intent == null) return null

        try {
            // 1. Verify Action if present
            val action = intent.action
            if (action != null && action !in ALLOWED_ACTIONS) {
                return null
            }

            // 2. Reject nested intents to prevent intent redirection vulnerabilities
            if (intent.hasExtra(Intent.EXTRA_INTENT)) {
                return null
            }

            // 3. Verify Data URI Scheme if present
            val dataUri = intent.data
            if (dataUri != null) {
                val scheme = dataUri.scheme?.lowercase()
                if (scheme != null && scheme !in ALLOWED_DATA_SCHEMES) {
                    return null // Reject unauthorized or dangerous schemes (http, javascript, intent, file)
                }
            }

            // 4. Validate String Extras against oversized payload DOS
            intent.extras?.let { bundle ->
                for (key in bundle.keySet()) {
                    val value = bundle.get(key)
                    if (value is String && value.length > MAX_STRING_EXTRA_LENGTH) {
                        return null // Reject oversized payload
                    }
                }
            }

            // 5. Clear unsafe permission grant flags from caller
            val safeFlags = intent.flags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            ).inv()

            return Intent(intent).apply {
                flags = safeFlags
            }
        } catch (_: Exception) {
            // Drop malformed or unpackable parcelables
            return null
        }
    }

    /**
     * Returns true if the intent contains any unsafe privilege escalation or URI grant flags.
     */
    fun hasUnsafeFlags(intent: Intent?): Boolean {
        if (intent == null) return false
        val dangerousFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        return (intent.flags and dangerousFlags) != 0
    }
}
