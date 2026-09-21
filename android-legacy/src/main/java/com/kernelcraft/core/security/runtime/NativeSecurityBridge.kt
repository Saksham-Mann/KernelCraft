package com.kernelcraft.core.security.runtime

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Result of the low-level native integrity handshake.
 */
data class NativeAttestationResult(
    val isValid: Boolean,
    val threadId: Int = 0,
    val uptimeMs: Long = 0L,
    val textChecksum: Long = 0L,
    val nonce: Long = 0L,
    val statusMask: Int = 0,
    val isDebuggerDetected: Boolean = false,
    val isMemoryTampered: Boolean = false,
    val failureReason: String? = null
)

/**
 * Hardened JNI Bridge interfacing with libkernelcraft_secure.so.
 *
 * Implements low-level POSIX and direct kernel assembly syscalls that run
 * beneath the Android ART runtime to defeat dynamic hooking (Frida),
 * process tracing (GDB/LLDB/TracerPid), and in-memory tampering.
 */
object NativeSecurityBridge {

    private const val NATIVE_LIBRARY_NAME = "kernelcraft_secure"
    private const val ATTESTATION_KEY = "KC_NATIVE_SEC_KEY_P256_ATTEST_2026"
    private const val TOKEN_SIZE_BYTES = 60

    private var isNativeLibraryLoaded = false

    init {
        try {
            System.loadLibrary(NATIVE_LIBRARY_NAME)
            isNativeLibraryLoaded = true
        } catch (_: Throwable) {
            isNativeLibraryLoaded = false
        }
    }

    fun isNativeBridgeActive(): Boolean = isNativeLibraryLoaded

    /**
     * Executes ptrace(PTRACE_TRACEME) via direct raw inline assembly syscall.
     * Returns true if another tracer (Frida, GDB, LLDB) is attached.
     */
    external fun isPtraceAttached(): Boolean

    /**
     * Scans dynamically loaded ELF shared libraries via dl_iterate_phdr.
     * Returns true if unauthorized libraries (frida, gadget, substrate, xposed) are found.
     */
    external fun scanNativeMaps(): Boolean

    /**
     * Generates a dynamic, rolling Zero-Trust HMAC attestation token from C++.
     * Token structure (60 bytes):
     * [0..31]  HMAC-SHA256 signature
     * [32..35] Calling thread ID
     * [36..43] Monotonic uptime ms
     * [44..47] .text section checksum
     * [48..55] Caller nonce
     * [56..59] Status mask (0 = secure)
     */
    external fun generateIntegrityAttestation(nonce: Long): ByteArray

    /**
     * Generates a cryptographically random nonce and validates the native attestation token.
     */
    fun verifyAttestation(): NativeAttestationResult {
        if (!isNativeLibraryLoaded) {
            return NativeAttestationResult(
                isValid = false,
                failureReason = "Native security library ($NATIVE_LIBRARY_NAME) failed to load."
            )
        }

        val nonce = SecureRandom().nextLong()
        val token = try {
            generateIntegrityAttestation(nonce)
        } catch (e: Throwable) {
            return NativeAttestationResult(
                isValid = false,
                failureReason = "Native attestation call failed: ${e.message}"
            )
        }

        return verifyAttestationToken(token, nonce)
    }

    /**
     * Parses and cryptographically validates a 60-byte native attestation token.
     */
    fun verifyAttestationToken(
        token: ByteArray,
        expectedNonce: Long,
        hmacKey: String = ATTESTATION_KEY
    ): NativeAttestationResult {
        if (token.size != TOKEN_SIZE_BYTES) {
            return NativeAttestationResult(
                isValid = false,
                failureReason = "Malformed token length: expected $TOKEN_SIZE_BYTES bytes, got ${token.size}."
            )
        }

        val receivedHmac = token.copyOfRange(0, 32)
        val payload = token.copyOfRange(32, 60)

        // 1. Verify HMAC-SHA256 signature
        val computedHmac = computeHmacSha256(payload, hmacKey)
        if (!MessageDigest.isEqual(receivedHmac, computedHmac)) {
            return NativeAttestationResult(
                isValid = false,
                failureReason = "HMAC signature mismatch: native token tampered or forged."
            )
        }

        // 2. Parse payload
        val buffer = ByteBuffer.wrap(payload)
        val threadId = buffer.int
        val uptimeMs = buffer.long
        val textChecksum = buffer.int.toLong() and 0xFFFFFFFFL
        val tokenNonce = buffer.long
        val statusMask = buffer.int

        // 3. Verify Nonce to defeat replay attacks
        if (tokenNonce != expectedNonce) {
            return NativeAttestationResult(
                isValid = false,
                threadId = threadId,
                uptimeMs = uptimeMs,
                textChecksum = textChecksum,
                nonce = tokenNonce,
                statusMask = statusMask,
                failureReason = "Replay attack detected: token nonce does not match expected challenge nonce."
            )
        }

        // 4. Evaluate status mask
        val isDebuggerDetected = (statusMask and (1 shl 0)) != 0
        val isMemoryTampered = (statusMask and (1 shl 1)) != 0

        val isSecure = (statusMask == 0) && !isDebuggerDetected && !isMemoryTampered

        return NativeAttestationResult(
            isValid = isSecure,
            threadId = threadId,
            uptimeMs = uptimeMs,
            textChecksum = textChecksum,
            nonce = tokenNonce,
            statusMask = statusMask,
            isDebuggerDetected = isDebuggerDetected,
            isMemoryTampered = isMemoryTampered,
            failureReason = if (!isSecure) "Status mask failure: debugger=$isDebuggerDetected, memoryTampered=$isMemoryTampered" else null
        )
    }

    /**
     * Helper computing HMAC-SHA256 over raw byte buffers.
     */
    fun computeHmacSha256(data: ByteArray, keyStr: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(keyStr.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }
}
