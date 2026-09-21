package com.kernelcraft.feature.security

import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.core.security.runtime.RuntimeSecurityStatus

/**
 * Non-bypassable full-screen security alert displayed when dynamic hooking,
 * root manipulation, or environment tampering is discovered at runtime.
 */
@Composable
fun SecurityAlertScreen(
    securityStatus: RuntimeSecurityStatus,
    onTerminateApp: () -> Unit = {
        Process.killProcess(Process.myPid())
        System.exit(1)
    }
) {
    // Intercept hardware back button to prevent navigating away from alert
    BackHandler(enabled = true) {
        onTerminateApp()
    }

    val darkBackground = Color(0xFF141211)
    val cardBackground = Color(0xFF1F1A19)
    val accentRed = Color(0xFFFF4D4D)
    val studioOrange = Color(0xFFDD5622)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Security Shield Icon with Red Glow
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(accentRed.copy(alpha = 0.15f))
                        .border(1.5.dp, accentRed.copy(alpha = 0.40f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = accentRed,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Security Integrity Alert",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "KernelCraft cannot execute in an untrusted or instrumented runtime environment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA09C9A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Diagnostic Findings Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBackground)
                        .border(1.dp, Color(0xFF2C2624), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DIAGNOSTIC FINDINGS",
                                style = MaterialTheme.typography.labelMedium,
                                color = accentRed,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "ERR-0x%02X".format(securityStatus.compositeMask),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF7A7572)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (securityStatus.securityFindings.isEmpty()) {
                            Text(
                                text = "• Environment integrity signature mismatch detected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD4CFCC)
                            )
                        } else {
                            securityStatus.securityFindings.forEach { finding ->
                                Text(
                                    text = "• $finding",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD4CFCC),
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guidance info banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(studioOrange.copy(alpha = 0.10f))
                        .border(1.dp, studioOrange.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "To protect hardware simulation algorithms and kernel telemetry, running on devices with active Frida agents, Xposed frameworks, Magisk, or custom test-keys is blocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE89675),
                        lineHeight = 18.sp
                    )
                }
            }

            // Bottom Exit Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = onTerminateApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Terminate Application",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
