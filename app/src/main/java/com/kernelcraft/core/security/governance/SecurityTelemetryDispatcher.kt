package com.kernelcraft.core.security.governance

import android.content.Context
import android.os.Build
import android.util.Base64
import com.kernelcraft.core.security.crypto.EncryptedPayload
import com.kernelcraft.core.security.crypto.KeyStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Categorized security attack vectors and threat incident signals.
 */
enum class ThreatVector {
    FRIDA_HOOK_ATTEMPT,
    ROOT_BINARY_DETECTED,
    SIGNATURE_MISMATCH,
    EMERGENCY_HALT_TRIGGERED,
    UNTRUSTED_INSTALLER,
    DEBUGGER_ATTACHED,
    INTEGRITY_VERDICT_FAILED,
    TAMPERED_PIN_DETECTED,
    DECOY_MODE_TRIGGERED
}

/**
 * Anonymized Zero-Knowledge Security Signal.
 * Completely stripped of PII, IP, MAC addresses, and persistent device hardware IDs.
 */
data class ThreatTelemetrySignal(
    val eventId: String = UUID.randomUUID().toString(),
    val vector: ThreatVector,
    val osApiLevel: Int = Build.VERSION.SDK_INT,
    val coarseTimestampHour: Long = (System.currentTimeMillis() / 3600000L) * 3600000L,
    val playIntegrityCode: String = "APP_LICENSED",
    val anonymizedPayloadHash: String
)

/**
 * Privacy-preserving, Zero-Knowledge Threat Telemetry Dispatcher.
 *
 * Enforces:
 * 1. Strict sanitization: Zero PII, Zero MAC, Zero IP, Zero Hardware Serial numbers.
 * 2. Asymmetric / AES-GCM encryption of events prior to transit using Hardware Keystore.
 * 3. Offline FIFO buffer with asynchronous flushing.
 */
object SecurityTelemetryDispatcher {

    private const val TELEMETRY_KEY_ALIAS = "kernelcraft_telemetry_vault_key"
    private val offlineQueue = ConcurrentLinkedQueue<EncryptedPayload>()
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Creates and dispatches a sanitized threat signal.
     */
    fun reportThreat(
        vector: ThreatVector,
        integrityVerdictCode: String = "VERDICT_EVALUATED",
        context: Context? = null
    ) {
        // Generate anonymous ephemeral session hash (no device identifiers)
        val ephemeralEntropy = UUID.randomUUID().toString() + vector.name
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(ephemeralEntropy.toByteArray(Charsets.UTF_8))
        val anonymousHash = try {
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(hash)
        }

        val signal = ThreatTelemetrySignal(
            vector = vector,
            playIntegrityCode = integrityVerdictCode,
            anonymizedPayloadHash = anonymousHash
        )

        dispatchSignal(signal)
    }

    /**
     * Encrypts and buffers/dispatches the threat signal.
     */
    fun dispatchSignal(signal: ThreatTelemetrySignal) {
        try {
            val json = JSONObject().apply {
                put("event_id", signal.eventId)
                put("vector", signal.vector.name)
                put("api_level", signal.osApiLevel)
                put("coarse_epoch_hour", signal.coarseTimestampHour)
                put("integrity_code", signal.playIntegrityCode)
                put("anon_hash", signal.anonymizedPayloadHash)
            }

            val rawBytes = json.toString().toByteArray(Charsets.UTF_8)
            val encrypted = KeyStoreManager.encrypt(TELEMETRY_KEY_ALIAS, rawBytes)

            offlineQueue.offer(encrypted)

            // Trigger async flush
            scope.launch {
                flushTelemetryQueue()
            }
        } catch (_: Exception) {
            // Fail-safe: Telemetry failures must never crash or block the application
        }
    }

    /**
     * Flushes buffered encrypted telemetry records.
     */
    suspend fun flushTelemetryQueue(): Int {
        var flushedCount = 0
        while (!offlineQueue.isEmpty()) {
            val payload = offlineQueue.poll() ?: break
            try {
                // In production: POST payload to https://telemetry.kernelcraft.com/v1/incident
                // Here: Securely zeroize payload after processing
                payload.zeroize()
                flushedCount++
            } catch (_: Exception) {
                // Re-queue or drop based on resilience policy
                break
            }
        }
        return flushedCount
    }

    fun getPendingQueueSize(): Int = offlineQueue.size

    fun clearQueueForTesting() {
        while (!offlineQueue.isEmpty()) {
            offlineQueue.poll()?.zeroize()
        }
    }
}
