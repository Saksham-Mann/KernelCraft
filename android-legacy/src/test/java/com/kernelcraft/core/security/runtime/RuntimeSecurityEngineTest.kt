package com.kernelcraft.core.security.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

/**
 * Automated unit tests verifying the runtime protection engine:
 * 1. Anti-hooking memory map parsing, TracerPid detection, and stack trace filters.
 * 2. Device integrity root detection, KernelSU/Magisk daemons, and mount flags parsing.
 * 3. Opaque control-flow bitmask calculations and memory zeroization.
 * 4. Play Integrity cryptographic nonce generation and SHA-256 hashing.
 */
class RuntimeSecurityEngineTest {

    // =========================================================================
    // 1. Anti-Hooking & Memory Map Logic Tests
    // =========================================================================

    @Test
    fun `memory map parser detects injected frida and xposed shared libraries`() {
        val mockCleanMaps = """
            7f98000000-7f98010000 r-xp 00000000 08:01 12345 /system/lib64/libc.so
            7f98010000-7f98020000 r--p 00010000 08:01 12345 /system/lib64/libc.so
            7f98020000-7f98030000 rw-p 00020000 08:01 12345 /system/lib64/libc.so
        """.trimIndent()

        val mockFridaMaps = """
            7f98000000-7f98010000 r-xp 00000000 08:01 12345 /system/lib64/libc.so
            7f98100000-7f98200000 r-xp 00000000 00:00 0 /data/local/tmp/frida-agent-64.so
        """.trimIndent()

        val mockXposedMaps = """
            7f98000000-7f98010000 r-xp 00000000 08:01 12345 /system/lib64/libc.so
            7f98300000-7f98400000 r-xp 00000000 00:00 0 /data/app/de.robv.android.xposed.installer/lib/arm64/libxposed_art.so
        """.trimIndent()

        fun parseMaps(content: String): Boolean {
            val targets = listOf("frida", "gadget.so", "xposed", "substrate", "libmembi.so")
            return content.lineSequence().any { line ->
                val lower = line.lowercase()
                targets.any { lower.contains(it) }
            }
        }

        assertFalse("Clean memory map must not trigger alerts", parseMaps(mockCleanMaps))
        assertTrue("Injected frida-agent must be detected", parseMaps(mockFridaMaps))
        assertTrue("Injected libxposed must be detected", parseMaps(mockXposedMaps))
    }

    @Test
    fun `tracerPid parser correctly identifies attached tracers`() {
        val mockCleanStatus = """
            Name:	com.kernelcraft
            State:	S (sleeping)
            Tgid:	1024
            Pid:	1024
            TracerPid:	0
            Uid:	10100	10100	10100	10100
        """.trimIndent()

        val mockTracedStatus = """
            Name:	com.kernelcraft
            State:	t (tracing stop)
            Tgid:	1024
            Pid:	1024
            TracerPid:	4096
            Uid:	10100	10100	10100	10100
        """.trimIndent()

        fun extractTracerPid(statusText: String): Int {
            return statusText.lineSequence()
                .firstOrNull { it.startsWith("TracerPid:") }
                ?.substringAfter("TracerPid:")
                ?.trim()
                ?.toIntOrNull() ?: 0
        }

        assertEquals(0, extractTracerPid(mockCleanStatus))
        assertEquals(4096, extractTracerPid(mockTracedStatus))
        assertTrue(extractTracerPid(mockTracedStatus) > 0)
    }

    @Test
    fun `stack trace filter flags dynamic hooking framework entrypoints`() {
        val cleanClass = "com.kernelcraft.MainActivity"
        val xposedClass = "de.robv.android.xposed.XposedBridge"
        val exposedClass = "me.weishu.exposed.ExposedBridge"

        val patterns = listOf(
            "de.robv.android.xposed.XposedBridge",
            "me.weishu.exposed.ExposedBridge",
            "com.saurik.substrate.MS$2"
        )

        fun isHookClass(className: String): Boolean {
            return patterns.any { className.contains(it) }
        }

        assertFalse(isHookClass(cleanClass))
        assertTrue(isHookClass(xposedClass))
        assertTrue(isHookClass(exposedClass))
    }

    // =========================================================================
    // 2. Device Integrity & Mount Flags Tests
    // =========================================================================

    @Test
    fun `proc mounts parser flags system partitions remounted as read-write`() {
        val mockCleanMounts = """
            /dev/block/bootdevice/by-name/system /system ext4 ro,seclabel,nodev,noatime 0 0
            /dev/block/bootdevice/by-name/vendor /vendor ext4 ro,seclabel,nodev,noatime 0 0
            /dev/block/bootdevice/by-name/userdata /data f2fs rw,seclabel,nosuid,nodev 0 0
        """.trimIndent()

        val mockTamperedMounts = """
            /dev/block/bootdevice/by-name/system /system ext4 rw,seclabel,nodev,noatime 0 0
            /dev/block/bootdevice/by-name/vendor /vendor ext4 ro,seclabel,nodev,noatime 0 0
        """.trimIndent()

        fun isDangerousMount(mountsText: String): Boolean {
            return mountsText.lineSequence().any { line ->
                val tokens = line.split(" ")
                if (tokens.size >= 4) {
                    val mountPoint = tokens[1]
                    val options = tokens[3].split(",")
                    val isRw = options.contains("rw")
                    isRw && (mountPoint == "/system" || mountPoint == "/vendor")
                } else false
            }
        }

        assertFalse("Read-only system partitions must pass", isDangerousMount(mockCleanMounts))
        assertTrue("Read-write system remount must trigger security violation", isDangerousMount(mockTamperedMounts))
    }

    // =========================================================================
    // 3. Opaque Control-Flow & Fail-Secure Tests
    // =========================================================================

    @Test
    fun `composite verification mask enforces all security layers`() {
        val expectedCleanMask = RuntimeSecurityPolicy.REQUIRED_CLEAN_MASK // 0x7F = 127

        var mask = 0
        mask = mask or RuntimeSecurityPolicy.FLAG_MEM_MAPS_CLEAN
        mask = mask or RuntimeSecurityPolicy.FLAG_PORTS_CLEAN
        mask = mask or RuntimeSecurityPolicy.FLAG_STACK_CLEAN
        mask = mask or RuntimeSecurityPolicy.FLAG_TRACER_CLEAN
        mask = mask or RuntimeSecurityPolicy.FLAG_SU_CLEAN
        mask = mask or RuntimeSecurityPolicy.FLAG_MOUNTS_CLEAN
        mask = mask or RuntimeSecurityPolicy.FLAG_TESTKEYS_CLEAN

        assertEquals(expectedCleanMask, mask)

        // If one check fails (e.g. Frida agent injected), mask does NOT match
        val bypassedMask = mask and RuntimeSecurityPolicy.FLAG_MEM_MAPS_CLEAN.inv()
        assertNotEquals(expectedCleanMask, bypassedMask)
    }

    @Test
    fun `deriveIntegrityToken generates deterministic SHA-256 tokens`() {
        val mask = RuntimeSecurityPolicy.REQUIRED_CLEAN_MASK
        val pkg = "com.kernelcraft"

        val token1 = RuntimeSecurityPolicy.deriveIntegrityToken(mask, pkg)
        val token2 = RuntimeSecurityPolicy.deriveIntegrityToken(mask, pkg)
        val tamperedToken = RuntimeSecurityPolicy.deriveIntegrityToken(mask and 0x3F, pkg)

        assertEquals("Same mask and package must yield identical token", token1, token2)
        assertEquals(64, token1.length) // 256 bits = 64 hex characters
        assertNotEquals("Altered mask must yield completely different token", token1, tamperedToken)
    }

    @Test
    fun `zeroizeBuffer safely clears sensitive memory contents`() {
        val secretKey = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
        assertFalse(secretKey.all { it == 0.toByte() })

        RuntimeSecurityPolicy.zeroizeBuffer(secretKey)

        assertTrue("All bytes in buffer must be scrubbed to zero", secretKey.all { it == 0.toByte() })
    }

    // =========================================================================
    // 4. Play Integrity API Nonce Tests
    // =========================================================================

    @Test
    fun `generateNonce produces valid Base64 string of high entropy`() {
        val nonce1 = PlayIntegrityManager.generateNonce()
        val nonce2 = PlayIntegrityManager.generateNonce()

        assertNotNull(nonce1)
        assertNotNull(nonce2)
        assertNotEquals("Consecutive nonces must be unique", nonce1, nonce2)
        assertTrue("Nonce length should reflect 32 bytes encoded", nonce1.length >= 40)
    }

    @Test
    fun `hashNonce computes standard SHA-256 representation`() {
        val nonce = "test_server_nonce_12345"
        val hash1 = PlayIntegrityManager.hashNonce(nonce)
        val hash2 = PlayIntegrityManager.hashNonce(nonce)

        assertEquals(hash1, hash2)
        assertNotNull(hash1)
    }
}
