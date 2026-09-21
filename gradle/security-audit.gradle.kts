import java.io.File
import java.util.regex.Pattern

/**
 * Gradle Security Audit Task (OWASP MASVS / MASTG SAST Gate)
 *
 * Scans Android source code and manifest for security anti-patterns:
 * - Insecure manifest configurations (allowBackup, missing explicit component export)
 * - Cleartext HTTP URIs (excluding XML schema namespaces)
 * - Hardcoded IP addresses
 * - Insecure logging of sensitive credentials/tokens
 * - Insecure unencrypted raw file I/O
 * - Hardcoded private keys and API tokens
 */

data class SecurityFinding(
    val ruleId: String,
    val severity: String, // CRITICAL, HIGH, MEDIUM, LOW
    val mastgRef: String,
    val file: String,
    val lineNumber: Int,
    val message: String,
    val snippet: String
)

tasks.register("securityAudit") {
    group = "verification"
    description = "Runs static application security testing (SAST) against OWASP MASVS/MASTG rules."

    val appMainDir = file("src/main")
    val manifestFile = file("src/main/AndroidManifest.xml")
    val reportDir = file("build/reports")
    val reportFile = file("build/reports/security-audit-report.json")

    doLast {
        val findings = mutableListOf<SecurityFinding>()

        // 1. Audit AndroidManifest.xml
        if (manifestFile.exists()) {
            val manifestLines = manifestFile.readLines()
            for ((index, line) in manifestLines.withIndex()) {
                val lineNum = index + 1

                // Check allowBackup="true"
                if (line.contains("android:allowBackup=\"true\"")) {
                    findings.add(
                        SecurityFinding(
                            ruleId = "MASTG-PLATFORM-001",
                            severity = "CRITICAL",
                            mastgRef = "MASVS-STORAGE-1",
                            file = manifestFile.path,
                            lineNumber = lineNum,
                            message = "Insecure backup enabled: android:allowBackup=\"true\" allows adb data extraction.",
                            snippet = line.trim()
                        )
                    )
                }

                // Check component tags without explicit android:exported
                val componentTags = listOf("<activity", "<service", "<receiver", "<provider")
                for (tag in componentTags) {
                    if (line.contains(tag) && !line.contains("android:exported")) {
                        // Look ahead across the multi-line tag to see if android:exported is declared
                        var hasExported = false
                        for (lookAheadIndex in index until minOf(manifestLines.size, index + 15)) {
                            val lookAheadLine = manifestLines[lookAheadIndex]
                            if (lookAheadLine.contains("android:exported")) {
                                hasExported = true
                                break
                            }
                            if (lookAheadLine.contains("/>") || (lookAheadLine.contains(">") && !lookAheadLine.contains("<"))) {
                                break
                            }
                        }
                        if (!hasExported) {
                            findings.add(
                                SecurityFinding(
                                    ruleId = "MASTG-PLATFORM-002",
                                    severity = "HIGH",
                                    mastgRef = "MASVS-PLATFORM-1",
                                    file = manifestFile.path,
                                    lineNumber = lineNum,
                                    message = "Missing explicit android:exported declaration on $tag tag.",
                                    snippet = line.trim()
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Audit Kotlin, Java, and XML source files
        val ignoredXmlSchemas = setOf(
            "http://schemas.android.com/apk/res/android",
            "http://schemas.android.com/apk/res-auto",
            "http://schemas.android.com/tools",
            "http://www.w3.org/2000/xmlns/",
            "http://www.w3.org/2001/XMLSchema-instance",
            "https://schema.gradle.org"
        )

        val ipPattern = Pattern.compile("\\b(?!127\\.0\\.0\\.1|0\\.0\\.0\\.0)(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")
        val httpPattern = Pattern.compile("http://[a-zA-Z0-9.-]+")
        val sensitiveLogPattern = Pattern.compile("Log\\.[vdiwe]\\([^)]*(?:password|secret|token|credential|bearer|apikey)[^)]*\\)", Pattern.CASE_INSENSITIVE)
        val privateKeyPattern = Pattern.compile("-----BEGIN (?:RSA |EC )?PRIVATE KEY-----")

        val filesToScan = fileTree(appMainDir) {
            include("**/*.kt", "**/*.java", "**/*.xml")
            exclude("**/res/raw/**", "**/res/drawable/**", "**/res/mipmap*/**")
        }

        filesToScan.forEach { sourceFile ->
            val lines = sourceFile.readLines()
            for ((index, line) in lines.withIndex()) {
                val lineNum = index + 1

                // Check cleartext HTTP URIs
                val httpMatcher = httpPattern.matcher(line)
                while (httpMatcher.find()) {
                    val matchedUri = httpMatcher.group()
                    var isSchema = false
                    for (schema in ignoredXmlSchemas) {
                        if (line.contains(schema)) {
                            isSchema = true
                            break
                        }
                    }
                    if (!isSchema) {
                        findings.add(
                            SecurityFinding(
                                ruleId = "MASTG-NETWORK-001",
                                severity = "HIGH",
                                mastgRef = "MASVS-NETWORK-1",
                                file = sourceFile.path,
                                lineNumber = lineNum,
                                message = "Insecure cleartext HTTP URI detected: $matchedUri",
                                snippet = line.trim()
                            )
                        )
                    }
                }

                // Check hardcoded IP addresses in string literals
                if (line.contains("\"")) {
                    val ipMatcher = ipPattern.matcher(line)
                    if (ipMatcher.find()) {
                        findings.add(
                            SecurityFinding(
                                ruleId = "MASTG-NETWORK-002",
                                severity = "MEDIUM",
                                mastgRef = "MASVS-NETWORK-2",
                                file = sourceFile.path,
                                lineNumber = lineNum,
                                message = "Hardcoded IP address discovered in source string literal.",
                                snippet = line.trim()
                            )
                        )
                    }
                }

                // Check sensitive logging
                val logMatcher = sensitiveLogPattern.matcher(line)
                if (logMatcher.find()) {
                    findings.add(
                        SecurityFinding(
                            ruleId = "MASTG-CODE-001",
                            severity = "HIGH",
                            mastgRef = "MASVS-CODE-2",
                            file = sourceFile.path,
                            lineNumber = lineNum,
                            message = "Potentially sensitive token/credential in Log statement.",
                            snippet = line.trim()
                        )
                    )
                }

                // Check hardcoded private keys
                if (privateKeyPattern.matcher(line).find()) {
                    findings.add(
                        SecurityFinding(
                            ruleId = "MASTG-CRYPTO-001",
                            severity = "CRITICAL",
                            mastgRef = "MASVS-CRYPTO-1",
                            file = sourceFile.path,
                            lineNumber = lineNum,
                            message = "Hardcoded private key block detected in source code.",
                            snippet = line.trim()
                        )
                    )
                }

                // Check unencrypted file writes using legacy insecure modes
                if (line.contains("MODE_WORLD_READABLE") || line.contains("MODE_WORLD_WRITEABLE")) {
                    findings.add(
                        SecurityFinding(
                            ruleId = "MASTG-STORAGE-002",
                            severity = "CRITICAL",
                            mastgRef = "MASVS-STORAGE-1",
                            file = sourceFile.path,
                            lineNumber = lineNum,
                            message = "Insecure world-readable or world-writable file mode detected.",
                            snippet = line.trim()
                        )
                    )
                }
            }
        }

        // Generate report directory and JSON output
        reportDir.mkdirs()
        val jsonFindings = findings.joinToString(",\n") { f ->
            """    {
      "ruleId": "${f.ruleId}",
      "severity": "${f.severity}",
      "mastgRef": "${f.mastgRef}",
      "file": "${f.file.replace("\\", "\\\\")}",
      "lineNumber": ${f.lineNumber},
      "message": "${f.message.replace("\"", "\\\"")}",
      "snippet": "${f.snippet.replace("\"", "\\\"")}"
    }"""
        }

        reportFile.writeText(
            """{
  "auditTimestamp": "${java.time.Instant.now()}",
  "totalFindings": ${findings.size},
  "findings": [
$jsonFindings
  ]
}"""
        )

        // Print formatted console summary
        println("\n" + "=".repeat(80))
        println(" KERNELCRAFT STATIC APPLICATION SECURITY TESTING (SAST) REPORT")
        println("=".repeat(80))
        println(" Scanned files in: ${appMainDir.path}")
        println(" Total security findings: ${findings.size}")
        println("-".repeat(80))

        for (finding in findings) {
            val color = when (finding.severity) {
                "CRITICAL" -> "\u001B[31m" // Red
                "HIGH" -> "\u001B[35m"     // Magenta
                "MEDIUM" -> "\u001B[33m"   // Yellow
                else -> "\u001B[36m"       // Cyan
            }
            println(" $color[${finding.severity}]\u001B[0m ${finding.ruleId} (${finding.mastgRef})")
            println("   File: ${finding.file}:${finding.lineNumber}")
            println("   Message: ${finding.message}")
            println("   Code: ${finding.snippet}")
            println("-".repeat(80))
        }

        val blockingFindings = findings.filter { it.severity in listOf("CRITICAL", "HIGH") }
        if (blockingFindings.isNotEmpty()) {
            throw GradleException(
                "SecurityAudit Gate FAILED: ${blockingFindings.size} Critical/High severity vulnerability findings detected. " +
                "Review report at: ${reportFile.path}"
            )
        } else {
            println(" [SUCCESS] SecurityAudit Gate Passed with 0 Critical/High findings.")
            println("=".repeat(80) + "\n")
        }
    }
}
