package com.kernelcraft.core.security.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Automated unit test suite verifying the Native C++ / JNI Zero-Trust Handshake:
 *
 * 1. Rolling HMAC-SHA256 token assembly and validation contract.
 * 2. Replay attack rejection via nonce challenge verification.
 * 3. Status mask bit evaluations (anti-debug & anti-dump indicators).
 * 4. Token length and signature integrity validation.
 */
class NativeSecurityBridgeTest {

    private val testHmacKey = "KC_NATIVE_SEC_KEY_P256_ATTEST_2026"

    private fun createSyntheticToken(
        threadId: Int = 1234,
        uptimeMs: Long = 45000L,
        textChecksum: Long = 0x811C9DC5L,
        nonce: Long = 9876543210L,
        statusMask: Int = 0,
        hmacKey: String = testHmacKey
    ): ByteArray {
        val payload = ByteBuffer.allocate(28).apply {
            putInt(threadId)
            putLong(uptimeMs)
            putInt(textChecksum.toInt())
            putLong(nonce)
            putInt(statusMask)
        }.array()

        val hmac = NativeSecurityBridge.computeHmacSha256(payload, hmacKey)

        val fullToken = ByteArray(60)
        System.arraycopy(hmac, 0, fullToken, 0, 32)
        System.arraycopy(payload, 0, fullToken, 32, 28)
        return fullToken
    }

    @Test
    fun `valid attestation token produces successful verification result`() {
        val expectedNonce = 9876543210L
        val token = createSyntheticToken(
            threadId = 4102,
            uptimeMs = 82190L,
            textChecksum = 0x12345678L,
            nonce = expectedNonce,
            statusMask = 0
        )

        val result = NativeSecurityBridge.verifyAttestationToken(token, expectedNonce, testHmacKey)

        assertTrue("Valid token must produce valid attestation result", result.isValid)
        assertEquals(4102, result.threadId)
        assertEquals(82190L, result.uptimeMs)
        assertEquals(0x12345678L, result.textChecksum)
        assertEquals(expectedNonce, result.nonce)
        assertEquals(0, result.statusMask)
        assertFalse(result.isDebuggerDetected)
        assertFalse(result.isMemoryTampered)
    }

    @Test
    fun `tampered payload or HMAC mismatch rejects token`() {
        val expectedNonce = 1122334455L
        val token = createSyntheticToken(nonce = expectedNonce)

        // Corrupt single byte in payload
        token[35] = (token[35].toInt() xor 0xFF).toByte()

        val result = NativeSecurityBridge.verifyAttestationToken(token, expectedNonce, testHmacKey)

        assertFalse("Tampered token must fail attestation", result.isValid)
        assertNotNull(result.failureReason)
        assertTrue(result.failureReason!!.contains("HMAC signature mismatch"))
    }

    @Test
    fun `replay attack with unexpected nonce is rejected`() {
        val originalNonce = 123456789L
        val token = createSyntheticToken(nonce = originalNonce)

        val differentChallengeNonce = 987654321L
        val result = NativeSecurityBridge.verifyAttestationToken(token, differentChallengeNonce, testHmacKey)

        assertFalse("Replay token with stale nonce must fail attestation", result.isValid)
        assertNotNull(result.failureReason)
        assertTrue(result.failureReason!!.contains("Replay attack detected"))
    }

    @Test
    fun `debugger detection bit in status mask flags debugger`() {
        val expectedNonce = 5566778899L
        val token = createSyntheticToken(
            nonce = expectedNonce,
            statusMask = (1 shl 0) // Bit 0: debugger present
        )

        val result = NativeSecurityBridge.verifyAttestationToken(token, expectedNonce, testHmacKey)

        assertFalse("Token with debugger bit set must not be valid", result.isValid)
        assertTrue("Debugger detected flag must be true", result.isDebuggerDetected)
        assertFalse(result.isMemoryTampered)
        assertTrue(result.failureReason!!.contains("debugger=true"))
    }

    @Test
    fun `memory tampering bit in status mask flags memory tampering`() {
        val expectedNonce = 9988776655L
        val token = createSyntheticToken(
            nonce = expectedNonce,
            statusMask = (1 shl 1) // Bit 1: memory tampered
        )

        val result = NativeSecurityBridge.verifyAttestationToken(token, expectedNonce, testHmacKey)

        assertFalse("Token with memory tampering bit must not be valid", result.isValid)
        assertFalse(result.isDebuggerDetected)
        assertTrue("Memory tampered flag must be true", result.isMemoryTampered)
        assertTrue(result.failureReason!!.contains("memoryTampered=true"))
    }

    @Test
    fun `malformed token length is immediately rejected`() {
        val shortToken = ByteArray(40)
        val resultShort = NativeSecurityBridge.verifyAttestationToken(shortToken, 123L, testHmacKey)
        assertFalse(resultShort.isValid)
        assertTrue(resultShort.failureReason!!.contains("Malformed token length"))

        val longToken = ByteArray(72)
        val resultLong = NativeSecurityBridge.verifyAttestationToken(longToken, 123L, testHmacKey)
        assertFalse(resultLong.isValid)
        assertTrue(resultLong.failureReason!!.contains("Malformed token length"))
    }

    @Test
    fun `computeHmacSha256 produces deterministic 32-byte digest`() {
        val data = "KernelCraftSecureNativePayloadTest".toByteArray(Charsets.UTF_8)
        val hmac1 = NativeSecurityBridge.computeHmacSha256(data, testHmacKey)
        val hmac2 = NativeSecurityBridge.computeHmacSha256(data, testHmacKey)

        assertEquals(32, hmac1.size)
        assertTrue("HMAC computation must be deterministic", hmac1.contentEquals(hmac2))
    }
}
