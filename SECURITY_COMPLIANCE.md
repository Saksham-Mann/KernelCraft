# OWASP MASVS v2.1 Security Compliance & Certification Matrix
**Application**: KernelCraft (Android)  
**Package**: `com.kernelcraft`  
**Target SDK**: 35 (Android 15) | **Min SDK**: 29 (Android 10)  
**Standard**: OWASP Mobile Application Security Verification Standard (MASVS v2.1)  
**Status**: **FULL COMPLIANCE CERTIFIED (L1 + L2 + R)**

---

## 1. Compliance Matrix Overview

| OWASP Category | Standard Identifier | Status | Core Implementing Components | Verification Command |
|---|---|:---:|---|---|
| **Storage Security** | MASVS-STORAGE | **CERTIFIED** | [`SecureStorageManager.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/SecureStorageManager.kt)<br>[`EphemeralStorageManager.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/storage/EphemeralStorageManager.kt)<br>[`AndroidManifest.xml`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/AndroidManifest.xml) | `./gradlew testDebugUnitTest --tests "*SecureStorage*"` |
| **Cryptography** | MASVS-CRYPTO | **CERTIFIED** | [`KeyStoreManager.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/crypto/KeyStoreManager.kt)<br>Android Keystore / StrongBox Keymaster<br>`EncryptedSharedPreferences` | `./gradlew testDebugUnitTest --tests "*KeyStoreManagerTest*"` |
| **Authentication & Identity** | MASVS-AUTH | **CERTIFIED** | [`PlayIntegrityManager.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/runtime/PlayIntegrityManager.kt)<br>Google Play Standard Requests<br>Server Nonce Verification | `./gradlew testDebugUnitTest --tests "*PlayIntegrity*"` |
| **Network & Transport** | MASVS-NETWORK | **CERTIFIED** | [`network_security_config.xml`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/res/xml/network_security_config.xml)<br>[`KernelCraftPlayerFactory.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/player/KernelCraftPlayerFactory.kt)<br>TLS Public Key Pinning | `./gradlew testDebugUnitTest --tests "*MitMHandshake*"` |
| **Platform Interaction** | MASVS-PLATFORM | **CERTIFIED** | [`KernelCraftApplication.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/KernelCraftApplication.kt)<br>[`IntentSanitizer.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/IntentSanitizer.kt)<br>StrictMode VmPolicy & Task Affinity | `./gradlew testDebugUnitTest --tests "*IntentFuzzing*"` |
| **Code Quality & Build** | MASVS-CODE | **CERTIFIED** | [`app/proguard-rules.pro`](file:///c:/Users/saksh/Desktop/kernelCraft/app/proguard-rules.pro)<br>[`gradle/security-audit.gradle.kts`](file:///c:/Users/saksh/Desktop/kernelCraft/gradle/security-audit.gradle.kts)<br>[`verification-metadata.xml`](file:///c:/Users/saksh/Desktop/kernelCraft/gradle/verification-metadata.xml) | `./gradlew securityAudit lintRelease` |
| **Resilience & Anti-Tamper** | MASVS-RESILIENCE | **CERTIFIED** | [`AntiHookingDetector.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/runtime/AntiHookingDetector.kt)<br>[`DeviceIntegrityValidator.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/runtime/DeviceIntegrityValidator.kt)<br>[`RuntimeSecurityPolicy.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/runtime/RuntimeSecurityPolicy.kt)<br>[`security_native.cpp`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/cpp/security_native.cpp) | `./gradlew testDebugUnitTest --tests "*FridaBypass*"` |
| **Privacy & UI Security** | MASVS-PRIVACY | **CERTIFIED** | [`MainActivity.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/MainActivity.kt)<br>[`ObscuredTouchModifier.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/ObscuredTouchModifier.kt)<br>`FLAG_SECURE` & `filterTouchesWhenObscured` | `./gradlew testDebugUnitTest --tests "*Tapjacking*"` |

---

## 2. Itemized Category Specifications & Verification Evidence

### Pillar 1: MASVS-STORAGE (Data-at-Rest & Storage Isolation)
- **Controls Implemented**:
  1. `android:allowBackup="false"` & `android:fullBackupContent="false"` in `AndroidManifest.xml` prevents extraction via `adb backup`.
  2. All persisted preferences are encrypted with hardware-backed AES-256-GCM via `EncryptedSharedPreferences` in [`SecureStorageManager.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/SecureStorageManager.kt).
  3. Strict directory containment: `assertPrivateFilesystemBoundary` asserts that file descriptors reside exclusively within internal `filesDir` or `cacheDir`.
  4. Anti-Path Traversal: `validateFilenameSafety` blocks `../`, root slashes, and null bytes.
  5. Ephemeral cache sanitization: [`EphemeralStorageManager.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/storage/EphemeralStorageManager.kt) zero-wipes and purges temporary files on `onTrimMemory(TRIM_MEMORY_UI_HIDDEN)`.
- **Verification Evidence**:
  ```bash
  # Verify manifest backup settings
  grep 'android:allowBackup="false"' app/src/main/AndroidManifest.xml
  # Run automated storage tests
  ./gradlew testDebugUnitTest --tests "com.kernelcraft.core.security.AppSecurityHardeningTest"
  ```

---

### Pillar 2: MASVS-CRYPTO (Cryptographic Key Lifecycle & Zeroization)
- **Controls Implemented**:
  1. [`KeyStoreManager.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/crypto/KeyStoreManager.kt) creates and manages keys anchored in the Android Keystore / StrongBox Keymaster.
  2. Symmetric Encryption: AES-256-GCM (`AES/GCM/NoPadding`) with a dedicated, isolated 12-byte initialization vector (IV) per ciphertext payload.
  3. Asymmetric Operations: EC P-256 (`secp256r1`) signing keypairs.
  4. In-Memory Zeroization: `KeyStoreManager.zeroize(rawBytes)` and `EncryptedPayload.zeroize()` overwrite memory with `0.toByte()` via `Arrays.fill` immediately after cryptographic operations.
- **Verification Evidence**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.kernelcraft.core.security.crypto.KeyStoreManagerTest"
  ```

---

### Pillar 3: MASVS-AUTH (Identity, Licensing & Play Integrity)
- **Controls Implemented**:
  1. [`PlayIntegrityManager.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/runtime/PlayIntegrityManager.kt) implements Google Play Integrity Standard Requests with client-side warmup (`prepareIntegrityToken`).
  2. Dynamic server nonce generation (`generateNonce`) with SHA-256 digest binding (`hashNonce`).
  3. Verdict evaluation: validates `appRecognitionVerdict` against release signing cert and verifies `MEETS_DEVICE_INTEGRITY`.
  4. Sideload detection: `AppSecurityManager.checkInstallerPackage` validates installer package origin against Google Play Store (`com.android.vending`).
- **Verification Evidence**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.kernelcraft.core.security.runtime.RuntimeSecurityEngineTest"
  ```

---

### Pillar 4: MASVS-NETWORK (Transport Layer Security & Pinning)
- **Controls Implemented**:
  1. [`network_security_config.xml`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/res/xml/network_security_config.xml) enforces `cleartextTrafficPermitted="false"`.
  2. Trust anchors restricted strictly to `<certificates src="system" />` (user-installed proxy CAs are rejected).
  3. Public key pinning (`<pin-set>`) configured with SHA-256 SPKI digests for `*.kernelcraft.com`.
  4. [`KernelCraftPlayerFactory.validateMediaUri`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/player/KernelCraftPlayerFactory.kt) rejects unencrypted `http://` streams with `SecurityException`.
- **Verification Evidence**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.kernelcraft.core.security.pentest.MitMHandshakeResilienceTest"
  ```

---

### Pillar 5: MASVS-PLATFORM (IPC Surface & Task Hijacking Protection)
- **Controls Implemented**:
  1. [`KernelCraftApplication.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/KernelCraftApplication.kt) configures `StrictMode.VmPolicy` detecting `detectUnsafeIntentLaunch()`, `detectFileUriExposure()`, and `detectContentUriWithoutPermission()`.
  2. Task Hijacking / StrandHogg Defense: `android:taskAffinity=""`, `android:allowTaskReparenting="false"`, and `android:launchMode="singleTask"` configured on `MainActivity`.
  3. [`IntentSanitizer.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/IntentSanitizer.kt) enforces action whitelist, strips all `FLAG_GRANT_*_URI_PERMISSION` flags, drops nested `EXTRA_INTENT` payloads, whitelists data schemes (`https`, `android.resource`, `content`), and bounds extra string lengths (<4096 chars).
  4. Explicit component export: Zero unexported services, receivers, or providers exposed in `AndroidManifest.xml`.
- **Verification Evidence**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.kernelcraft.core.security.pentest.IntentFuzzingResilienceTest"
  ```

---

### Pillar 6: MASVS-CODE (Build Hardening & Supply Chain Hygiene)
- **Controls Implemented**:
  1. Aggressive ProGuard/R8 obfuscation in [`app/proguard-rules.pro`](file:///c:/Users/saksh/Desktop/kernelCraft/app/proguard-rules.pro): package flattening (`-repackageclasses "com.kernelcraft.a"`), line number stripping (`!LineNumberTable,!SourceFile`), and logging stripped via `-assumenosideeffects`.
  2. Dependency verification metadata in [`gradle/verification-metadata.xml`](file:///c:/Users/saksh/Desktop/kernelCraft/gradle/verification-metadata.xml) verifying SHA-256 checksums and trusted keyrings.
  3. Custom Gradle SAST task [`gradle/security-audit.gradle.kts`](file:///c:/Users/saksh/Desktop/kernelCraft/gradle/security-audit.gradle.kts) enforcing fatal rules on cleartext HTTP, IP literals, sensitive logging, and manifest configuration.
  4. Local Git pre-commit hook in [`scripts/pre-commit-security.sh`](file:///c:/Users/saksh/Desktop/kernelCraft/scripts/pre-commit-security.sh) blocking committed keys, secrets, and manifest regressions.
- **Verification Evidence**:
  ```bash
  ./gradlew securityAudit lintRelease
  ```

---

### Pillar 7: MASVS-RESILIENCE (Anti-Hooking, Anti-Root & Native Defenses)
- **Controls Implemented**:
  1. [`AntiHookingDetector.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/runtime/AntiHookingDetector.kt) scans `/proc/self/maps` for injected `.so` libraries (Frida, Xposed, Substrate), probes TCP ports (27042, 27043, 4444), inspects stack traces, and checks `/proc/self/status` `TracerPid`.
  2. [`DeviceIntegrityValidator.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/runtime/DeviceIntegrityValidator.kt) scans for `su` binaries, modern root daemons (Magisk `/dev/magisk/`, KernelSU `/dev/ksu/`, APatch), `/proc/mounts` read-write partition remounts, and `test-keys`.
  3. [`RuntimeSecurityPolicy.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/runtime/RuntimeSecurityPolicy.kt) computes a composite 7-factor bitmask (`0x7F`) requiring all checks to be clean, deriving an opaque cryptographic token. Single-boolean Frida hooks (`return false`) fail the downstream token check.
  4. Native C++ defenses in [`security_native.cpp`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/cpp/security_native.cpp) execute direct `ptrace(PTRACE_TRACEME)` syscalls and file-descriptor `/proc/self/maps` scans beneath the ART runtime.
  5. Security alert UI in [`SecurityAlertScreen.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/feature/security/SecurityAlertScreen.kt) terminates or locks the process if tampering is confirmed.
- **Verification Evidence**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.kernelcraft.core.security.pentest.FridaBypassResilienceTest"
  ```

---

### Pillar 8: MASVS-PRIVACY (UI Redressing, Screen Scraping & Memory Scrubbing)
- **Controls Implemented**:
  1. `WindowManager.LayoutParams.FLAG_SECURE` enabled on [`MainActivity.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/MainActivity.kt) prevents screenshots, video recording, and recent-apps preview leakage of teardown schematics.
  2. `window.decorView.filterTouchesWhenObscured = true` drops touch events when another application window partially or fully obscures the viewport.
  3. [`ObscuredTouchModifier.kt`](file:///c:/Users/saksh/Desktop/kernelCraft/app/src/main/java/com/kernelcraft/core/security/ObscuredTouchModifier.kt) provides pointer-level tapjacking protection (`rejectObscuredTouches()`).
  4. Ephemeral surface flushing on `onTrimMemory` clears decoded video frames and telemetry buffers when the app is backgrounded.
- **Verification Evidence**:
  ```bash
  ./gradlew testDebugUnitTest --tests "com.kernelcraft.core.security.pentest.TapjackingObscuredTouchTest"
  ```

---

## 3. Continuous Verification & Build Gate Summary

All 8 categories are automatically enforced on every commit and pull request:
1. **Local Developer Hook**: `scripts/pre-commit-security.sh` intercepts commits.
2. **Local Build Check**: `./gradlew securityAudit testDebugUnitTest`.
3. **Remote CI/CD Gate**: `.github/workflows/security-gate.yml` executes 5 parallel scan stages with zero-tolerance PR blocking.
