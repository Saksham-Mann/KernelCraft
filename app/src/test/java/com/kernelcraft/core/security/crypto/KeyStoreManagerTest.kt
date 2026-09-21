package com.kernelcraft.core.security.crypto

import com.kernelcraft.core.security.storage.EphemeralStorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Automated unit tests verifying MASVS-CRYPTO and MASVS-STORAGE implementations:
 * 1. In-memory zeroization of sensitive raw key and cryptographic byte buffers.
 * 2. EncryptedPayload data integrity, zeroization lifecycle, and equality contracts.
 * 3. Ephemeral storage sanitization and secure file wiping.
 */
class KeyStoreManagerTest {

    @Test
    fun `zeroize overwrites byte array with zero bytes`() {
        val sensitiveKeyMaterial = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        assertFalse(sensitiveKeyMaterial.all { it == 0.toByte() })

        KeyStoreManager.zeroize(sensitiveKeyMaterial)

        assertTrue("All bytes in array must be zeroized", sensitiveKeyMaterial.all { it == 0.toByte() })
    }

    @Test
    fun `zeroize handles null and empty arrays safely`() {
        // Should not throw NullPointerException or IndexOutOfBoundsException
        KeyStoreManager.zeroize(null)
        KeyStoreManager.zeroize(ByteArray(0))
    }

    @Test
    fun `EncryptedPayload zeroize clears both IV and ciphertext buffers`() {
        val iv = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val ciphertext = byteArrayOf(0x10, 0x20, 0x30, 0x40, 0x50)

        val payload = EncryptedPayload(iv = iv, ciphertext = ciphertext)

        assertFalse(payload.iv.all { it == 0.toByte() })
        assertFalse(payload.ciphertext.all { it == 0.toByte() })

        payload.zeroize()

        assertTrue("IV must be scrubbed", payload.iv.all { it == 0.toByte() })
        assertTrue("Ciphertext must be scrubbed", payload.ciphertext.all { it == 0.toByte() })
    }

    @Test
    fun `EncryptedPayload equality and hashCode contract`() {
        val iv1 = byteArrayOf(1, 2, 3)
        val ct1 = byteArrayOf(4, 5, 6)
        val payload1 = EncryptedPayload(iv1, ct1)

        val iv2 = byteArrayOf(1, 2, 3)
        val ct2 = byteArrayOf(4, 5, 6)
        val payload2 = EncryptedPayload(iv2, ct2)

        val iv3 = byteArrayOf(9, 9, 9)
        val payload3 = EncryptedPayload(iv3, ct1)

        assertEquals(payload1, payload2)
        assertEquals(payload1.hashCode(), payload2.hashCode())
        assertFalse(payload1 == payload3)
    }

    @Test
    fun `EphemeralStorageManager secureDelete overwrites and unlinks file`() {
        val tempFile = File.createTempFile("kernelcraft_secret_trace", ".tmp")
        tempFile.writeText("SENSITIVE_SESSION_KEY_OR_TELEMETRY_DATA_1234567890")

        assertTrue("File must exist prior to deletion", tempFile.exists())
        val deleted = EphemeralStorageManager.secureDelete(tempFile)

        assertTrue("File must be deleted successfully", deleted)
        assertFalse("File must no longer exist on filesystem", tempFile.exists())
    }
}
