package com.kernelcraft

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.kernelcraft.core.security.AppSecurityManager
import com.kernelcraft.core.security.IntentSanitizer
import com.kernelcraft.core.security.governance.SecurityGovernanceManager
import com.kernelcraft.core.security.governance.SecurityTelemetryDispatcher
import com.kernelcraft.core.security.governance.ThreatVector
import com.kernelcraft.core.security.runtime.PlayIntegrityManager
import com.kernelcraft.core.security.runtime.RuntimeSecurityPolicy
import com.kernelcraft.feature.security.SecurityAlertScreen
import com.kernelcraft.feature.security.SecurityHaltScreen
import com.kernelcraft.navigation.KernelCraftNavHost
import kotlinx.coroutines.launch

/**
 * Single-Activity entry point for KernelCraft.
 *
 * Hardened with:
 * 1. Anti-Screen Scraping: WindowManager.LayoutParams.FLAG_SECURE prevents screenshotting & recent apps leakage.
 * 2. Anti-Tapjacking: window.decorView.filterTouchesWhenObscured blocks touch injection from overlays.
 * 3. IPC Sanitization: Validates incoming Intents against redirection and unauthorized URI grants.
 * 4. Runtime Integrity: Executes anti-tamper and root detection audits on launch.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // Enforce anti-tapjacking and anti-screen scraping strictly on production builds
        // (Allows Android Studio emulator touch streaming and mirroring during development)
        if (!isDebug) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
            window.decorView.filterTouchesWhenObscured = true
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Sanitize launch intent
        val sanitizedIntent = IntentSanitizer.sanitize(intent)
        if (sanitizedIntent == null && intent?.action != null) {
            Log.w("KernelCraftSecurity", "Rejected potentially unsafe or unauthorized launch intent")
        }

        // Conduct multi-signal runtime integrity & anti-hooking audit
        val runtimeStatus = RuntimeSecurityPolicy.evaluateRuntimeSecurity(this)

        if (!runtimeStatus.isSecure && !isDebug) {
            SecurityTelemetryDispatcher.reportThreat(
                vector = ThreatVector.ROOT_BINARY_DETECTED,
                context = this
            )
        }

        // Asynchronously warm up Play Integrity standard token provider
        lifecycleScope.launch {
            PlayIntegrityManager.warmUp(this@MainActivity)
        }

        val governanceVerdict = SecurityGovernanceManager.getLastVerdict()

        setContent {
            if (governanceVerdict.isEmergencyHaltActive) {
                // Non-bypassable emergency remote kill-switch / deprecation halt screen
                SecurityHaltScreen(verdict = governanceVerdict)
            } else if (!runtimeStatus.isSecure && !isDebug) {
                // Non-bypassable security alert screen when environment is compromised
                SecurityAlertScreen(securityStatus = runtimeStatus)
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFDD5622) // Studio background default
                ) {
                    KernelCraftNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val sanitized = IntentSanitizer.sanitize(intent)
        if (sanitized != null) {
            setIntent(sanitized)
        } else {
            Log.w("KernelCraftSecurity", "Rejected potentially malicious new intent")
        }
    }
}
