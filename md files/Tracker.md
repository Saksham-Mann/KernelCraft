# tracker.md — Development Kanban Checklist

## Backlog / Not Started
- [ ] (Add future track content beyond the launch track here)

## Phase 1: Asset Prep
- [x] Shoot/source raw teardown footage
- [x] Edit footage to 5-chapter narrative cut (10–20s)
- [x] All-Intra encode; verify with `ffprobe` all frames are I-frames
- [x] Export chip-highlight static still (matches Dive-In handoff moment)
- [x] Produce OS layer illustrations (Hardware, Kernel, Drivers, Syscall Interface, OS Services, Userspace)
- [ ] Author Lottie: stepper feedback icon
- [ ] Author Lottie: syscall completion checkmark
- [ ] Author Lottie: idle chip pulse (optional/nice-to-have)
- [ ] Write track content JSON (chapters, layer text, syscall steps)
- [ ] Validate JSON against schema.md structures
- [ ] **Phase 1 sign-off**

## Phase 2: Core Scaffold
- [x] Project setup, single-Activity + Compose Navigation graph (all routes stubbed)
- [x] ViewModels + UiState data classes wired (schema.md) with placeholder data
- [ ] DataStore setup (last track, completion flags)
- [ ] JSON manifest repository layer
- [x] Theme module: color tokens, typography scale, shape tokens, `CircularIconButton` component
- [x] Full nav smoke test: Hub → Teardown → Transition → OS Stack → Syscall Sim → back
- [ ] **Phase 2 sign-off**

## Phase 3: Scrollytelling UI
- [ ] Selection Hub: real track cards, tap micro-interaction
- [x] Teardown: ExoPlayer wired, all-intra asset loaded
- [x] Teardown: scroll → normalized progress → throttled `seekTo` pipeline
- [x] Teardown: chapter caption bottom-sheet, chip highlight overlay, Dive-In CTA
- [x] Transition: `graphicsLayer` scale/blur/crossfade `Animatable` sequence
- [x] Transition: static-still handoff (no live frame capture)
- [x] OS Stack: interactive 5-tier architecture explorer (`KernelStackScreen.kt`)
- [x] OS Stack: per-layer white content sheets with discrete background color shifts
- [x] OS Stack: live CPU governor, runqueues, and Binder IPC telemetry visualizers
- [x] Macrobenchmark & Baseline Profile suite set up on Teardown & Kernel Stack screen (`:baselineprofile`)
- [x] Perfetto/Layout Inspector profiling pass — fix recomposition storms
- [ ] Device matrix test: low-tier (memory <250MB peak, <5% dropped frames)
- [ ] Device matrix test: mid-tier
- [ ] Device matrix test: high-tier
- [ ] **Phase 3 sign-off**

## Phase 4: Interactive Sandbox
- [x] OS & Kernel Live Telemetry visualizer state machine (`TierInteractiveVisualizer`)
- [x] Step 0–4 diagrams built (privilege rings, EAS CPU governor, Binder IPC pipe)
- [x] Subsystem inspection modals (Camera Optics ray-tracer & Battery/Vapor Chamber thermal simulator)
- [x] Procedural low-latency Audio Engine (`TeardownAudioSynthesizer.kt`) with velocity ratchet clicks, whirrs, and silicon hum
- [ ] Completion flow: Restart / Back to Hub, persist completion to DataStore
- [ ] Accessibility pass: content descriptions, 48dp touch targets, TalkBack walkthrough
- [x] Asset delivery decision finalized (19.8MB all-intra H.264 cut fits within 25MB base bundle)
- [x] ProGuard/R8 rules verified on release build (aggressive obfuscation, logging stripped, source debug removed)
- [x] Full QA pass against this tracker
- [ ] Final device-matrix regression pass
- [ ] **Phase 4 / release sign-off**

## Phase 5: Security Hardening & DevSecOps (Complete)
- [x] **Category 1 (Binary Protection & Anti-Tamper)**: Aggressive R8 dictionary renaming, debug metadata stripping (`!SourceFile,!LineNumberTable`), runtime root/debugger/installer verification (`AppSecurityManager.kt`).
- [x] **Category 2 (Data-at-Rest Hardening)**: `allowBackup="false"`, Keystore-backed AES-256-GCM encrypted preferences, private directory boundary enforcement & path-traversal prevention (`SecureStorageManager.kt`).
- [x] **Category 3 (Transport Layer Security)**: Cleartext HTTP disabled, system trust anchors, TLS public key pinning (`network_security_config.xml`), ExoPlayer URI scheme validation (`KernelCraftPlayerFactory.kt`).
- [x] **Category 4 (IPC & Attack Surface Reduction)**: Explicit component visibility, Intent action validation, dangerous URI permission flag stripping, nested intent injection prevention (`IntentSanitizer.kt`).
- [x] **Category 5 (UI Redressing & Anti-Tapjacking)**: Anti-screen scraping (`FLAG_SECURE`), overlay touch-injection defense (`filterTouchesWhenObscured = true`).
- [x] **Category 6 (Dependency & Supply Chain Hygiene)**: Strict HTTPS repositories, `androidx.security:security-crypto` integration, automated security test suite (`AppSecurityHardeningTest.kt`).
- [x] **CI/CD Security Gate (SAST)**: Strict Android Lint fatal security rules & custom `./gradlew securityAudit` task scanning for OWASP MASVS violations (`gradle/security-audit.gradle.kts`).
- [x] **Dependency Verification**: Cryptographic supply chain checksum validation (`gradle/verification-metadata.xml`).
- [x] **GitHub Actions Pipeline**: 5-stage automated security gate enforcing zero-tolerance MASVS policy (`.github/workflows/security-gate.yml`).
- [x] **Local Git Security Hook**: Automated pre-commit hook scanning for committed keystores, secrets, and manifest regressions (`scripts/pre-commit-security.sh`).
- [x] **Phase 5 sign-off**

## Phase 6: Runtime Defense & Anti-Instrumentation Layer (Complete)
- [x] **Play Integrity API (`PlayIntegrityManager.kt`)**: Standard Requests with token warmup, cryptographic server nonce generation & hashing, verdicts parsing (`appRecognitionVerdict`, `deviceRecognitionVerdict`, `appAccessRiskVerdict`), and remediation dialog hooks.
- [x] **Anti-Hooking & Dynamic Instrumentation (`AntiHookingDetector.kt`)**: `/proc/self/maps` scanner for injected `.so` libraries (Frida, Xposed, Substrate, Gadget), Frida TCP port checks (27042, 27043, 4444), stack trace anomaly inspection, `/proc/self/status` `TracerPid` detection.
- [x] **Device Integrity & Root Guard (`DeviceIntegrityValidator.kt`)**: Multi-path `su` executable discovery, modern root daemons (Magisk `/dev/magisk/`, KernelSU `/dev/ksu`, APatch), `/proc/mounts` read-write partition inspection (`/system` remount checks), unofficial `test-keys` verification.
- [x] **Opaque Control-Flow & Fail-Secure Engine (`RuntimeSecurityPolicy.kt`)**: Composite bitmask validation (0x7F) resisting single-boolean Frida hooks, in-memory zeroization (`zeroizeBuffer`), process kill on compromise.
- [x] **Native Syscall Defenses (`NativeSecurityBridge.kt` & `security_native.cpp`)**: Low-level C++ `ptrace(PTRACE_TRACEME)` and `/proc/self/maps` POSIX file-descriptor scanning bypassing ART Java hooks.
- [x] **Security Tamper UI (`SecurityAlertScreen.kt`)**: Non-bypassable Jetpack Compose alert screen displaying diagnostic error codes and mandatory termination.
- [x] **Automated Runtime Verification (`RuntimeSecurityEngineTest.kt`)**: 8 automated test cases for memory map analysis, tracer detection, mount parsing, and token math.
- [x] **Phase 6 sign-off**

## Phase 7: Red Team Adversarial Assessment & Verification (Complete)
- [x] **Frida / Xposed Dynamic Bypass Simulation (`FridaBypassResilienceTest.kt`)**: Proved that mocking high-level boolean methods fails against composite bitmasks (0x7F), native C++ dual-layer maps scanning, and cryptographic token derivation.
- [x] **Intent Spoofing & IPC Fuzzing (`IntentFuzzingResilienceTest.kt`)**: Validated strict dropping of unauthorized actions, privilege escalation flags (`FLAG_GRANT_*`), nested `EXTRA_INTENT` redirection payloads, malformed data schemes, and oversized string payloads (>4096 chars).
- [x] **MitM & Pinning Bypass Simulation (`MitMHandshakeResilienceTest.kt`)**: Verified handshake termination on untrusted/self-signed proxy CA certificates, certificate pinning mismatch aborts, and strict rejection of cleartext HTTP downgrade attempts.
- [x] **UI Overlay & Tapjacking Exploit Test (`TapjackingObscuredTouchTest.kt`)**: Verified obscured touch rejection (`FLAG_WINDOW_IS_OBSCURED`, `FLAG_WINDOW_IS_PARTIALLY_OBSCURED`) and Compose `rejectObscuredTouches()` modifier.
- [x] **Defensive Patches Deployed**: `IntentSanitizer.kt` hardened with scheme and length bounds; `ObscuredTouchModifier.kt` integrated; `RuntimeSecurityPolicy.kt` native cross-verification enabled.
- [x] **Phase 7 sign-off**

## Phase 8: OWASP MASVS v2.1 Compliance Certification & Cryptographic Vault (Complete)
- [x] **MASVS-CRYPTO (Key Vault)**: Hardware-backed Android Keystore / StrongBox Keymaster AES-256-GCM and EC P-256 key management with isolated 12-byte IV generation and in-memory zeroization (`KeyStoreManager.kt`).
- [x] **MASVS-PLATFORM (Task Hijacking Protection)**: `StrictMode.VmPolicy` (`detectUnsafeIntentLaunch()`, `detectFileUriExposure()`, `detectContentUriWithoutPermission()`) in `KernelCraftApplication.kt`; `android:taskAffinity=""`, `android:allowTaskReparenting="false"`, `android:launchMode="singleTask"` in `AndroidManifest.xml`.
- [x] **MASVS-STORAGE & MASVS-PRIVACY (Ephemeral Hygiene)**: `EphemeralStorageManager.kt` securely zero-wipes cache/codeCache files; `KernelCraftApplication.onTrimMemory` aggressively flushes decode surfaces and scrubs memory.
- [x] **Full Compliance Matrix (`SECURITY_COMPLIANCE.md`)**: Certified all 8 categories (STORAGE, CRYPTO, AUTH, NETWORK, PLATFORM, CODE, RESILIENCE, PRIVACY) with code links and verification commands.
- [x] **Automated Tests (`KeyStoreManagerTest.kt`)**: Validated zeroization routines, EncryptedPayload lifecycle, and secure file deletion.
- [x] **Phase 8 / Final Security Posture sign-off**

## Phase 9: Distribution Security, Platform 15 Hardening & Reproducible Builds (Complete)
- [x] **Android 15 Sensitive Content Protection (`SensitiveContentManager.kt`)**: Enforced automatic redaction during screen sharing/media projection (`View.setContentSensitivity`), dynamic `FLAG_SECURE` toggling, and `FLAG_IMMUTABLE` enforcement on all PendingIntents.
- [x] **Cryptographic Signing Pipeline (`signingConfigs`)**: Enforced APK Signature Schemes v2, v3 (with rotation lineage), and v4 (fs-verity streaming `.idsig`); strictly rejected legacy v1 JAR signing in `app/build.gradle.kts`.
- [x] **Distribution & Sideload Provenance (`InstallSourceValidator.kt`)**: Implemented `getInstallSourceInfo` verification to detect sideloaded/repackaged binaries and restrict sensitive silicon die telemetry on untrusted origins.
- [x] **Reproducible Release Builds & CycloneDX SBOM**: Configured deterministic archive packaging (`isPreserveFileTimestamps = false`, `isReproducibleFileOrder = true`) and added automated `./gradlew generateSbom` task producing `build/reports/bom.json`.
- [x] **Phase 9 / Final Distribution Security sign-off**

## Phase 10: Enterprise Incident Response & Zero-Trust Remote Governance (Complete)
- [x] **Cryptographically Signed Remote Kill-Switch (`SecurityGovernanceManager.kt`)**: Implemented detached ECDSA P-256 (`SHA256withECDSA`) signature verification against an embedded SecOps root public key, enforcing fail-secure zeroization and emergency halt upon revoked builds or outdated security versions.
- [x] **Dynamic Certificate Pinning & In-Flight Key Rotation (`DynamicPinningManager.kt`)**: Scaffolded 3-tier SPKI pinning (Tier 1 Primary, Tier 2 Standby, Tier 3 Disaster Recovery) with hot-reloading OTA pinset updates and dynamic `X509TrustManager`.
- [x] **Zero-Knowledge Threat Telemetry Dispatcher (`SecurityTelemetryDispatcher.kt`)**: Privacy-preserving incident reporting client stripping PII, IP, MAC, and device serials while dispatching encrypted threat vectors with coarse hourly timestamps and offline FIFO buffering.
- [x] **Anti-Reverse Engineering Decoy Mode (`DecoyModeManager.kt`)**: Stealth countermeasure for non-fatal dynamic analysis heuristics, injecting synthetic CPU scheduler jitter, degraded media playback (0.45x), and honeypot network canaries (`KC_CANARY_*`).
- [x] **Emergency Lockdown UI (`SecurityHaltScreen.kt`)**: Non-dismissible dark studio theme halt screen intercepting back navigation, detailing revocation cause, cryptographic audit trace, and store update pathways.
- [x] **Operational Incident Response Runbook (`SECURITY_RUNBOOK.md`)**: Complete playbook for offline HSM signature generation, edge CDN rollout, zero-downtime certificate rotation, decoy SIEM filters, and BigQuery threat analytics.
- [x] **Automated Unit Testing (`SecurityGovernanceTest.kt`)**: Automated verification for signature tampering rejection, revocation triggers, dynamic pin recognition, decoy metrics, and zero-knowledge telemetry.
- [x] **Phase 10 / Enterprise Remote Governance sign-off**

## Phase 11: Android NDK Low-Level Security & Binary Hardening (Complete)
- [x] **Direct Kernel Syscall Engine (`syscalls.h`)**: Implemented raw inline assembly syscalls (`svc #0` on ARM64, `svc 0` on ARM32, `syscall` on x86_64) for `__NR_ptrace`, `__NR_openat`, `__NR_read`, `__NR_close`, and `__NR_exit_group`, evading libc GOT/PLT function interception.
- [x] **Native Anti-Debugging & Tracer Interception (`anti_debug.cpp`)**: Called `ptrace(PTRACE_TRACEME)` via raw syscall denial-of-service and spawned an isolated background pthread monitoring `TracerPid` from `/proc/self/status` with instant `raw_exit_group` termination.
- [x] **Memory Dump & Hooking Protection (`anti_dump.cpp`)**: Configured Linux `inotify` file-descriptor monitoring on `/proc/self/mem`, `/proc/self/maps`, and `/proc/self/pagemap`, aborting external memory scrapers; integrated `dl_iterate_phdr` module scanning for injected `.so` libraries (Frida, Gadget, Substrate, Xposed).
- [x] **C++20 Compile-Time String Encryption & Symbol Stripping (`obfuscation.h` & `CMakeLists.txt`)**: Enforced `constexpr` XOR string encryption macros (`OBFUSCATE_STR`) keeping sensitive paths/keys out of binary strings; configured `-fvisibility=hidden`, `-fvisibility-inlines-hidden`, and `-Wl,--strip-all`.
- [x] **Zero-Trust JNI Rolling HMAC Handshake (`jni_bridge.cpp` & `NativeSecurityBridge.kt`)**: Generated and validated dynamic 60-byte HMAC attestation tokens incorporating calling thread ID, monotonic uptime, `.text` segment checksum, and nonce challenges.
- [x] **Automated Unit Tests (`NativeSecurityBridgeTest.kt`)**: Validated token structure, cryptographic HMAC verification, replay attack prevention, and debugger/memory tampering status mask evaluations.
- [x] **Phase 11 / NDK Low-Level Security sign-off**

## Bugs / Polish (ongoing)
- [ ] (log as discovered)