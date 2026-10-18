package com.kernelcraft.core.security.governance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Automated unit test suite verifying Enterprise Incident Response and Zero-Trust Remote Governance:
 *
 * 1. Cryptographically Signed Remote Kill-Switch & Attestation (SecurityGovernanceManager)
 * 2. Multi-Tier Dynamic Certificate Pinning & Hot-Reloading (DynamicPinningManager)
 * 3. Zero-Knowledge Privacy-Preserving Threat Telemetry (SecurityTelemetryDispatcher)
 * 4. Anti-Reverse Engineering Decoy & Honeypot Mode (DecoyModeManager)
 */
class SecurityGovernanceTest {

    @Before
    fun setUp() {
        DecoyModeManager.setDecoyMode(false)
        DynamicPinningManager.updateActivePins(emptyList())
    }

    // =========================================================================
    // Category 1: Remote Kill-Switch & Attestation Verification
    // =========================================================================

    @Test
    fun `rejects unsigned or tampered governance payloads without authentic signature`() {
        val payload = """
            {
                "revocation_status": "ACTIVE",
                "emergency_kill_active": false,
                "min_supported_version_code": 1
            }
        """.trimIndent()

        val invalidSignature = "MEQCIB8Z3FakeSignatureThatFailsECDSAVerification1234567890AA=="

        val verdict = SecurityGovernanceManager.processGovernancePayload(
            context = null,
            rawJsonPayload = payload,
            detachedSignatureBase64 = invalidSignature,
            currentVersionCode = 1,
            bypassSignatureVerificationForTesting = false
        )

        assertFalse("Tampered payload must not be permitted to execute", verdict.isPermittedToExecute)
        assertTrue("Tampered payload must trigger emergency halt", verdict.isEmergencyHaltActive)
        assertNotNull(verdict.haltReason)
        assertTrue(verdict.haltReason!!.contains("signature verification failed"))
    }

    @Test
    fun `triggers emergency halt and revocation when revocation_status is REVOKED`() {
        val payload = """
            {
                "revocation_status": "REVOKED",
                "emergency_kill_active": true,
                "min_supported_version_code": 1,
                "timestamp": 1774092000000
            }
        """.trimIndent()

        val verdict = SecurityGovernanceManager.processGovernancePayload(
            context = null,
            rawJsonPayload = payload,
            detachedSignatureBase64 = "MOCK_SIGNATURE",
            currentVersionCode = 5,
            bypassSignatureVerificationForTesting = true
        )

        assertFalse("Revoked build must not execute", verdict.isPermittedToExecute)
        assertTrue("Emergency halt must be active", verdict.isEmergencyHaltActive)
        assertTrue(verdict.haltReason!!.contains("Emergency Security Kill-Switch"))
        assertEquals(1774092000000L, verdict.payloadTimestamp)
    }

    @Test
    fun `triggers emergency halt when app version is below min_supported_version_code`() {
        val payload = """
            {
                "revocation_status": "ACTIVE",
                "emergency_kill_active": false,
                "min_supported_version_code": 100
            }
        """.trimIndent()

        val verdict = SecurityGovernanceManager.processGovernancePayload(
            context = null,
            rawJsonPayload = payload,
            detachedSignatureBase64 = "MOCK_SIGNATURE",
            currentVersionCode = 95, // Outdated version
            bypassSignatureVerificationForTesting = true
        )

        assertFalse("Deprecated build must not execute", verdict.isPermittedToExecute)
        assertTrue("Emergency halt must be active for deprecated build", verdict.isEmergencyHaltActive)
        assertTrue(verdict.haltReason!!.contains("Critical Security Update Required"))
        assertEquals(100, verdict.minSupportedVersion)
    }

    @Test
    fun `permits execution when payload is active and app version satisfies threshold`() {
        val payload = """
            {
                "revocation_status": "ACTIVE",
                "emergency_kill_active": false,
                "min_supported_version_code": 50,
                "timestamp": 1774092000000
            }
        """.trimIndent()

        val verdict = SecurityGovernanceManager.processGovernancePayload(
            context = null,
            rawJsonPayload = payload,
            detachedSignatureBase64 = "MOCK_SIGNATURE",
            currentVersionCode = 55,
            bypassSignatureVerificationForTesting = true
        )

        assertTrue("Permitted to execute", verdict.isPermittedToExecute)
        assertFalse("Emergency halt must be inactive", verdict.isEmergencyHaltActive)
        assertFalse("Decoy mode recommended must be false", verdict.isDecoyModeRecommended)
    }

    // =========================================================================
    // Category 2: Dynamic Certificate Pinning & Hot-Reloading
    // =========================================================================

    @Test
    fun `DynamicPinningManager recognizes static tiers and dynamic pins`() {
        // Tier 1, 2, 3 must be valid by default
        assertTrue("Tier 1 Primary Pin must be valid", DynamicPinningManager.isPinValid(DynamicPinningManager.TIER_1_PRIMARY_PIN))
        assertTrue("Tier 2 Backup Pin must be valid", DynamicPinningManager.isPinValid(DynamicPinningManager.TIER_2_BACKUP_PIN))
        assertTrue("Tier 3 Emergency Fallback Pin must be valid", DynamicPinningManager.isPinValid(DynamicPinningManager.TIER_3_EMERGENCY_FALLBACK_PIN))

        // Arbitrary unknown pin must be rejected
        assertFalse("Unknown pin must be rejected", DynamicPinningManager.isPinValid("RoguePinHashNotEnrolledInTiers1234567890AA="))

        // Update active dynamic pins OTA
        val newDynamicPin = "OtaHotRolloverPinSHA256HashForZeroDowntime="
        DynamicPinningManager.updateActivePins(listOf("sha256/$newDynamicPin"))

        assertTrue("New dynamic pin must be accepted", DynamicPinningManager.isPinValid(newDynamicPin))
        assertTrue("All pins set includes new dynamic pin", DynamicPinningManager.getAllValidPins().contains(newDynamicPin))
    }

    @Test
    fun `governance payload applies dynamic pinset update OTA`() {
        val newPin = "DynamicOtaPinFromGovernanceAuthority99="
        val payload = """
            {
                "revocation_status": "ACTIVE",
                "emergency_kill_active": false,
                "min_supported_version_code": 1,
                "pinset_update": [
                    "sha256/$newPin"
                ]
            }
        """.trimIndent()

        SecurityGovernanceManager.processGovernancePayload(
            context = null,
            rawJsonPayload = payload,
            detachedSignatureBase64 = "MOCK_SIG",
            currentVersionCode = 1,
            bypassSignatureVerificationForTesting = true
        )

        assertTrue("OTA pin from governance payload must now be valid", DynamicPinningManager.isPinValid(newPin))
    }

    // =========================================================================
    // Category 3: Anti-Reverse Engineering Decoy & Honeypot Mode
    // =========================================================================

    @Test
    fun `DecoyModeManager scrambles telemetry and throttles media playback`() {
        assertFalse("Decoy mode must be inactive by default", DecoyModeManager.isDecoyMode())
        assertEquals(1.0f, DecoyModeManager.getDecoyPlaybackSpeedMultiplier(), 0.01f)

        DecoyModeManager.setDecoyMode(true)
        assertTrue("Decoy mode must be active after enabling", DecoyModeManager.isDecoyMode())
        assertEquals(0.45f, DecoyModeManager.getDecoyPlaybackSpeedMultiplier(), 0.01f)

        val baseFreq = 2.0f
        val scrambledFreq = DecoyModeManager.getDecoyCpuFrequency(baseFreq)
        assertTrue("Scrambled frequency must be within [0.20, 4.85] GHz", scrambledFreq in 0.20f..4.85f)

        val decoySlices = DecoyModeManager.generateDecoyProcessSlices()
        assertTrue("Decoy process slices must be populated", decoySlices.isNotEmpty())
        assertTrue("Decoy slices contain honeypot probe task", decoySlices.any { it.processName.contains("ptrace_sentinel") })

        val canaryUrl = DecoyModeManager.generateCanaryHoneypotUrl()
        assertTrue("Canary URL must target canary domain", canaryUrl.startsWith("https://api.kernelcraft.com/v1/debug/symbols_trace"))
        assertTrue("Canary URL must contain KC_CANARY token", canaryUrl.contains("KC_CANARY_"))

        DecoyModeManager.setDecoyMode(false)
        assertFalse(DecoyModeManager.isDecoyMode())
    }

    // =========================================================================
    // Category 4: Zero-Knowledge Privacy-Preserving Threat Telemetry
    // =========================================================================

    @Test
    fun `ThreatTelemetrySignal enforces zero-knowledge anonymization without PII`() {
        val signal = ThreatTelemetrySignal(
            eventId = UUID.randomUUID().toString(),
            vector = ThreatVector.FRIDA_HOOK_ATTEMPT,
            osApiLevel = 35,
            playIntegrityCode = "MEETS_DEVICE_INTEGRITY",
            anonymizedPayloadHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )

        // Verify coarse timestamp is bucketed to hourly epoch (modulo 3,600,000 == 0)
        assertEquals("Timestamp must be rounded to nearest hour to prevent timing correlation", 0L, signal.coarseTimestampHour % 3600000L)

        // Verify anonymized hash is present
        assertFalse("Anonymized payload hash must not be blank", signal.anonymizedPayloadHash.isBlank())

        // Verify threat vector name
        assertEquals("FRIDA_HOOK_ATTEMPT", signal.vector.name)
    }
}
