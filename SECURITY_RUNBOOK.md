# KernelCraft Incident Response & Enterprise Security Runbook

**Classification:** STRICTLY CONFIDENTIAL // SECOPS INTERNAL  
**Authority:** Security Operations & Release Engineering  
**Applicable Platform:** Android (KernelCraft Release & Distribution Builds)  
**Standard Compliance:** OWASP MASVS v2.1 (MASVS-RESILIENCE, MASVS-NETWORK, MASVS-CRYPTO, MASVS-PRIVACY)

---

## Table of Contents
1. [Emergency Kill-Switch & Zero-Day Revocation](#1-emergency-kill-switch--zero-day-revocation)
2. [Cryptographic Payload Generation & Signature Verification](#2-cryptographic-payload-generation--signature-verification)
3. [Zero-Downtime Certificate Pinning Rotation (Tier 1 -> Tier 2)](#3-zero-downtime-certificate-pinning-rotation)
4. [Anti-Reverse Engineering Decoy & Honeypot Forensics](#4-anti-reverse-engineering-decoy--honeypot-forensics)
5. [Zero-Knowledge Threat Telemetry Forensics (BigQuery / OpenSearch)](#5-zero-knowledge-threat-telemetry-forensics)
6. [Incident Escalation Matrix & SLAs](#6-incident-escalation-matrix--slas)

---

## 1. Emergency Kill-Switch & Zero-Day Revocation

When a zero-day vulnerability, compromised signing key, or unauthorized rogue APK distribution is discovered post-launch, the application can be frozen fleet-wide via the cryptographically signed **Remote Security Governance Protocol**.

### Immediate Impact on Client Devices:
- The running app immediately aborts interactive teardown and kernel simulation sessions.
- Triggers **Fail-Secure Zeroization**:
  - `RuntimeSecurityPolicy.scrubMemory()` zeroes out all in-memory cryptographic buffers.
  - `EphemeralStorageManager.cleanEphemeralCache(context)` wipes decrypted frame caches, tokens, and temporary files.
- Displays the non-dismissible `SecurityHaltScreen` with emergency status badges and store remediation links.

---

## 2. Cryptographic Payload Generation & Signature Verification

To prevent Man-in-the-Middle (MitM) attackers from spoofing kill commands or redirecting pinsets, all governance payloads **MUST** be signed with the offline **ECDSA P-256 Air-Gapped HSM Key**. Unsigned or invalidly signed payloads are discarded immediately.

### Step 2.1: Prepare the Governance JSON Payload (`payload.json`)

```json
{
  "revocation_status": "REVOKED",
  "emergency_kill_active": true,
  "min_supported_version_code": 105,
  "decoy_mode_active": false,
  "timestamp": 1774092000000,
  "pinset_update": [
    "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=",
    "k2/oNdHQ3ZChCQKqCsoGWDNiTwElUVsCTX7hlTFLIwU="
  ]
}
```

### Step 2.2: Sign the Payload Using Offline OpenSSL / HSM (ECDSA P-256)

```bash
# 1. Generate detached DER signature from the offline private key
openssl dgst -sha256 -sign /etc/hsm/keys/governance_secp256r1.pem -out payload.sig payload.json

# 2. Base64-encode the signature for transit header or envelope
openssl base64 -in payload.sig -out payload.sig.b64 -A

# 3. Verify locally against the baked-in Public Key before CDN publication
openssl dgst -sha256 -verify /etc/hsm/keys/governance_pubkey.pem -signature payload.sig payload.json
```

### Step 2.3: Deploy to Edge CDN (Cloudflare / Fastly)

```bash
# Invalidate CDN cache and deploy with a 60-second TTL
curl -X POST "https://api.cloudflare.com/client/v4/zones/${CF_ZONE_ID}/purge_cache" \
     -H "Authorization: Bearer ${CF_API_TOKEN}" \
     -H "Content-Type: application/json" \
     -d '{"files":["https://governance.kernelcraft.com/v1/policy.json"]}'

# Upload the signed payload and signature header
curl -X PUT "https://edge-storage.kernelcraft.com/governance/v1/policy.json" \
     -H "X-KernelCraft-Signature: $(cat payload.sig.b64)" \
     -H "Content-Type: application/json" \
     --data-binary @payload.json
```

---

## 3. Zero-Downtime Certificate Pinning Rotation

KernelCraft employs a 3-tier pinning architecture (`DynamicPinningManager.kt`) backed by dynamic OTA updating:
- **Tier 1 (Primary):** Current production leaf/intermediate SPKI hash.
- **Tier 2 (Standby Backup):** Pre-generated standby keypair ready for immediate rollover.
- **Tier 3 (Disaster Recovery Root):** Offline root CA public key hash for catastrophic recovery.

### Step 3.1: Calculate SPKI Hash of New Certificate

```bash
# Extract SubjectPublicKeyInfo (SPKI) SHA-256 in Base64
openssl x509 -in new_production_cert.pem -pubkey -noout | \
openssl pkey -pubin -outform der | \
openssl dgst -sha256 -binary | \
openssl enc -base64
# Output example: 9x8B...7qA=
```

### Step 3.2: Issue In-Flight Key Rotation

1. Update edge reverse proxy (NGINX / Envoy) to present the **Tier 2 (Standby)** certificate.
   - Because Tier 2 is already accepted by client builds, 100% of existing users connect without error.
2. Sign and push an updated `pinset_update` array in the remote governance payload:
   - Primary: Set to the newly active certificate (former Tier 2).
   - Backup: Introduce a new Tier 2 standby SPKI hash.
3. Once 99.5% fleet attestation is confirmed via telemetry, retire the old Tier 1 key.

---

## 4. Anti-Reverse Engineering Decoy & Honeypot Forensics

When non-fatal dynamic analysis heuristics (e.g. Frida runtime hooking, debugger attachment, unverified installer) are triggered, the app quietly engages **Decoy Mode (`DecoyModeManager.kt`)** rather than terminating immediately.

### Indicators of Decoy Engagement:
1. **CPU Scheduler Telemetry Scrambled:** Fake clock speeds oscillating erratically between 0.20 GHz and 4.85 GHz.
2. **Video Playback Throttled:** Playback rate throttled to 0.45x normal speed to slow down dynamic analysis.
3. **Canary Honeypot URL Queried:** Requests to `https://api.kernelcraft.com/v1/debug/symbols_trace?probe=KC_CANARY_XXXXXX`.

### Edge WAF / SIEM Decoy Detection Filter:
```nginx
# NGINX Honeytoken Intercept Rule
location /v1/debug/symbols_trace {
    access_log /var/log/nginx/honeypot_triggers.log honeytoken_format;
    # Alert Slack / PagerDuty SecOps webhook
    mirror /internal/secops_alert_mirror;
    # Return fake decompiler debugging symbols to prolong attacker diversion
    return 200 '{"status":"ok","debug_symbols_loaded":true,"offsets":{"ptrace":48201,"frida_guard":9102}}';
}
```

---

## 5. Zero-Knowledge Threat Telemetry Forensics

`SecurityTelemetryDispatcher.kt` dispatches privacy-preserving signals. All device serials, MAC addresses, raw IP addresses, and user identifiers are stripped prior to transit.

### Schema:
| Field | Type | Description |
|---|---|---|
| `event_id` | String (UUID) | Ephemeral UUID |
| `vector` | String | Attack Vector enum (`FRIDA_HOOK_ATTEMPT`, `ROOT_BINARY_DETECTED`, `EMERGENCY_HALT_TRIGGERED`, etc.) |
| `api_level` | Integer | Android OS SDK version (e.g. 34, 35) |
| `coarse_epoch_hour` | Long | Timestamp rounded to nearest hour (prevents side-channel timing correlation) |
| `integrity_code` | String | Google Play Integrity evaluation verdict |
| `anon_hash` | String | Salted SHA-256 hash of ephemeral event entropy |

### BigQuery Fleet Monitoring Query:
```sql
-- Query: Aggregate active threat campaigns across the fleet in the last 24 hours
SELECT
  vector,
  api_level,
  integrity_code,
  COUNT(DISTINCT event_id) as total_signals,
  TIMESTAMP_MILLIS(coarse_epoch_hour) as event_hour
FROM
  `kernelcraft-secops.telemetry.threat_signals`
WHERE
  TIMESTAMP_MILLIS(coarse_epoch_hour) >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 24 HOUR)
GROUP BY
  vector, api_level, integrity_code, event_hour
ORDER BY
  total_signals DESC;
```

---

## 6. Incident Escalation Matrix & SLAs

| Severity | Incident Profile | Immediate Action | SLA |
|---|---|---|---|
| **P0 - Catastrophic** | Zero-Day RCE, Private Signing Key Compromised, Rogue Play Store Trojan | Publish ECDSA-signed Emergency Kill-Switch (`emergency_kill_active: true`) | < 15 Minutes |
| **P1 - Critical** | TLS Leaf Certificate Compromise / Unauthorized CA Issuance | Rotate to Tier 2 Standby Pin via Edge Proxy & deploy OTA `pinset_update` | < 1 Hour |
| **P2 - High** | New Unpacked Frida Script / Magisk Root Cloak Bypass In Wild | Engage Decoy Mode fleet-wide for affected OS builds, capture honeypot logs | < 4 Hours |
| **P3 - Moderate** | Deprecated App Version with unpatched library vulnerabilities | Increment `min_supported_version_code` in signed governance payload | < 24 Hours |

---
*Document certified by DevSecOps and Enterprise Security Architecture.*
