package com.kernelcraft.core.security.governance

import android.util.Base64
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.CopyOnWriteArraySet
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Dynamic Certificate Pinning and In-Flight Key Rotation Manager.
 *
 * Implements a 3-tier pinning architecture with hot-reloading capability:
 * - Tier 1: Primary production leaf / intermediate SPKI hash.
 * - Tier 2: Backup standby key ready for instant zero-downtime failover.
 * - Tier 3: Emergency Fallback offline disaster-recovery root/intermediate key.
 * - Dynamic Tier: Cryptographically signed OTA pin updates injected via SecurityGovernanceManager.
 */
object DynamicPinningManager {

    // Tier 1: Primary Production SPKI Pin (SHA-256 base64)
    const val TIER_1_PRIMARY_PIN = "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="

    // Tier 2: Standby Backup SPKI Pin (SHA-256 base64)
    const val TIER_2_BACKUP_PIN = "k2/oNdHQ3ZChCQKqCsoGWDNiTwElUVsCTX7hlTFLIwU="

    // Tier 3: Emergency Offline Fallback Root/Intermediate Key (SHA-256 base64)
    const val TIER_3_EMERGENCY_FALLBACK_PIN = "cN6K9JkF8Pq9tU4N2xL1wS5V8Q0mP7R4Z3aB2cD1eF0="

    // Dynamic OTA Pins received from authentic signed governance payloads
    private val dynamicPins = CopyOnWriteArraySet<String>()

    /**
     * Updates the in-flight dynamic pins from an authenticated governance payload.
     */
    fun updateActivePins(newPins: List<String>) {
        val sanitized = newPins.map { pin ->
            if (pin.startsWith("sha256/")) pin.removePrefix("sha256/") else pin
        }.filter { it.isNotBlank() }

        dynamicPins.clear()
        dynamicPins.addAll(sanitized)
    }

    /**
     * Returns all currently valid pins (Static Tiers 1-3 + Active Dynamic OTA Pins).
     */
    fun getAllValidPins(): Set<String> {
        val pins = mutableSetOf(
            TIER_1_PRIMARY_PIN,
            TIER_2_BACKUP_PIN,
            TIER_3_EMERGENCY_FALLBACK_PIN
        )
        pins.addAll(dynamicPins)
        return pins
    }

    /**
     * Checks if a raw SHA-256 Base64 SPKI hash is accepted by any active tier.
     */
    fun isPinValid(spkiSha256Base64: String): Boolean {
        val cleanPin = if (spkiSha256Base64.startsWith("sha256/")) {
            spkiSha256Base64.removePrefix("sha256/")
        } else {
            spkiSha256Base64
        }
        return getAllValidPins().contains(cleanPin)
    }

    /**
     * Computes the SHA-256 digest of an X.509 certificate's SubjectPublicKeyInfo (SPKI).
     */
    fun computeSpkiSha256(certificate: X509Certificate): String {
        val spkiBytes = certificate.publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256").digest(spkiBytes)
        return try {
            Base64.encodeToString(digest, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(digest)
        }
    }

    /**
     * Verifies that at least one certificate in the provided chain satisfies active pins.
     * Throws CertificateException if pin verification fails.
     */
    fun verifyChainPinning(chain: Array<X509Certificate>) {
        if (chain.isEmpty()) {
            throw CertificateException("Empty certificate chain presented for dynamic pin verification.")
        }

        val validPins = getAllValidPins()
        var pinMatched = false

        for (cert in chain) {
            val certSpkiHash = computeSpkiSha256(cert)
            if (validPins.contains(certSpkiHash)) {
                pinMatched = true
                break
            }
        }

        if (!pinMatched) {
            throw CertificateException("Dynamic Pinning Failure: No certificate in chain matched active pins.")
        }
    }

    /**
     * Creates a custom [X509TrustManager] enforcing both default system CA trust
     * and dynamic SPKI certificate pinning.
     */
    fun createDynamicPinningTrustManager(): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as java.security.KeyStore?)
        val defaultTrustManager = tmf.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
            ?: throw IllegalStateException("No default X509TrustManager available on platform.")

        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {
                defaultTrustManager.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {
                defaultTrustManager.checkServerTrusted(chain, authType)
                if (chain != null) {
                    verifyChainPinning(chain)
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return defaultTrustManager.acceptedIssuers
            }
        }
    }
}
