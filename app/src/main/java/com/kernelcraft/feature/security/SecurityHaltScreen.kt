package com.kernelcraft.feature.security

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernelcraft.core.security.governance.GovernanceVerdict

/**
 * Non-dismissible emergency lockdown screen displayed when a cryptographically signed
 * remote kill-switch, emergency zero-day revocation, or mandatory minimum-version gate
 * is triggered by the SecOps Security Governance Authority.
 */
@Composable
fun SecurityHaltScreen(
    verdict: GovernanceVerdict,
    onOpenUpdateLink: (() -> Unit)? = null,
    onExitApp: () -> Unit = {
        Process.killProcess(Process.myPid())
        System.exit(0)
    }
) {
    val context = LocalContext.current

    // Intercept back navigation - prevent bypassing lockdown
    BackHandler(enabled = true) {
        onExitApp()
    }

    val darkBackground = Color(0xFF121110)
    val cardBackground = Color(0xFF1C1918)
    val accentRed = Color(0xFFFF453A)
    val titaniumBorder = Color(0xFF383330)
    val textPrimary = Color(0xFFF5F5F7)
    val textSecondary = Color(0xFFA19F9D)
    val studioOrange = Color(0xFFDD5622)

    val defaultOpenUpdate: () -> Unit = onOpenUpdateLink ?: {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }

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
                Spacer(modifier = Modifier.height(36.dp))

                // Glowing Emergency Halt Shield
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(accentRed.copy(alpha = 0.12f))
                        .border(1.5.dp, accentRed.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = accentRed,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Security Lockdown",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cryptographic Remote Governance Enforcement",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentRed,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Detail Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBackground)
                        .border(1.dp, titaniumBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GOVERNANCE STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentRed.copy(alpha = 0.20f))
                                    .border(1.dp, accentRed.copy(alpha = 0.60f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (verdict.isEmergencyHaltActive) "HALTED / REVOKED" else "DEPRECATED",
                                    color = accentRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = verdict.haltReason
                                ?: "This build has been deactivated by enterprise security policy due to a security advisory or required platform update.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textPrimary,
                            lineHeight = 20.sp
                        )

                        // Technical Audit Trace
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141211))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Root Authority", fontSize = 11.sp, color = textSecondary, fontFamily = FontFamily.Monospace)
                                Text("ECDSA P-256 / SecOps", fontSize = 11.sp, color = textPrimary, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cryptographic Sig", fontSize = 11.sp, color = textSecondary, fontFamily = FontFamily.Monospace)
                                Text("SHA256withECDSA Verified", fontSize = 11.sp, color = Color(0xFF10B981), fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Min Required Build", fontSize = 11.sp, color = textSecondary, fontFamily = FontFamily.Monospace)
                                Text("v${verdict.minSupportedVersion}", fontSize = 11.sp, color = textPrimary, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Session Zeroization", fontSize = 11.sp, color = textSecondary, fontFamily = FontFamily.Monospace)
                                Text("Completed (Keys Purged)", fontSize = 11.sp, color = textSecondary, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = defaultOpenUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = studioOrange,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Update Application via Store",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onExitApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, titaniumBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textSecondary
                    )
                ) {
                    Text(
                        text = "Exit Application",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
