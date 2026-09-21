package com.kernelcraft.core.security.runtime

import android.os.Debug
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetSocketAddress
import java.net.Socket

/**
 * High-performance anti-hooking and dynamic instrumentation detector.
 *
 * Protects against:
 * 1. Frida Gadget & Frida Server (`frida-agent.so`, `gadget.so`, port 27042/27043)
 * 2. Xposed / LSPosed / EdXposed framework hooking (`XposedBridge`, `ExposedBridge`)
 * 3. Cydia Substrate (`libsubstrate.so`, `MS$2`)
 * 4. Ptrace attach / Native debuggers (`TracerPid > 0` in `/proc/self/status`)
 */
object AntiHookingDetector {

    // Suspicious shared library indicators in process memory maps
    private val SUSPICIOUS_LIBRARIES = listOf(
        "frida",
        "gadget.so",
        "xposed",
        "edxposed",
        "lsposed",
        "substrate",
        "libmembi.so",
        "riru",
        "zygisk"
    )

    // Default Frida server and gadget listening ports
    private val FRIDA_DEFAULT_PORTS = listOf(27042, 27043, 4444)

    // Known hooking framework classes in stack traces
    private val HOOK_STACK_PATTERNS = listOf(
        "de.robv.android.xposed.XposedBridge",
        "me.weishu.exposed.ExposedBridge",
        "com.saurik.substrate.MS$2",
        "com.elderdrivers.riru.core",
        "io.github.vvb2060.magisk"
    )

    /**
     * Executes comprehensive anti-hooking scan across memory maps, ports, traces, and ptrace status.
     */
    fun isHookingDetected(findings: MutableList<String> = mutableListOf()): Boolean {
        var detected = false

        if (checkInjectedLibraries(findings)) detected = true
        if (checkFridaPorts(findings)) detected = true
        if (checkHookingStackTraces(findings)) detected = true
        if (checkTracerPid(findings)) detected = true
        if (checkDebuggerAttached(findings)) detected = true

        return detected
    }

    /**
     * Scans `/proc/self/maps` for injected shared libraries from hooking engines.
     */
    fun checkInjectedLibraries(findings: MutableList<String> = mutableListOf()): Boolean {
        return try {
            val mapsFile = File("/proc/self/maps")
            if (!mapsFile.exists() || !mapsFile.canRead()) return false

            var hookDetected = false
            BufferedReader(FileReader(mapsFile)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    val lowerLine = line.lowercase()
                    for (pattern in SUSPICIOUS_LIBRARIES) {
                        if (lowerLine.contains(pattern)) {
                            findings.add("Injected hooking library found in /proc/self/maps: $pattern")
                            hookDetected = true
                            break
                        }
                    }
                    if (hookDetected) break
                    line = reader.readLine()
                }
            }
            hookDetected
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Attempts lightweight socket connections on localhost to probe for active Frida server / gadget listeners.
     */
    fun checkFridaPorts(findings: MutableList<String> = mutableListOf()): Boolean {
        for (port in FRIDA_DEFAULT_PORTS) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 25)
                    findings.add("Active dynamic instrumentation port open on localhost: $port (Frida candidate)")
                    return true
                }
            } catch (_: Exception) {
                // Expected when port is closed
            }
        }
        return false
    }

    /**
     * Examines current thread stack trace for hooking framework dispatch frames.
     */
    fun checkHookingStackTraces(findings: MutableList<String> = mutableListOf()): Boolean {
        val stackTrace = Thread.currentThread().stackTrace
        for (element in stackTrace) {
            val className = element.className
            for (pattern in HOOK_STACK_PATTERNS) {
                if (className.contains(pattern)) {
                    findings.add("Hooking framework stack frame detected: $className")
                    return true
                }
            }
        }
        return false
    }

    /**
     * Inspects `/proc/self/status` to determine if a debugger or ptrace monitor is actively attached.
     */
    fun checkTracerPid(findings: MutableList<String> = mutableListOf()): Boolean {
        return try {
            val statusFile = File("/proc/self/status")
            if (!statusFile.exists() || !statusFile.canRead()) return false

            var isTraced = false
            BufferedReader(FileReader(statusFile)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.startsWith("TracerPid:")) {
                        val pidStr = line.substringAfter("TracerPid:").trim()
                        val pid = pidStr.toIntOrNull() ?: 0
                        if (pid > 0) {
                            findings.add("Active native tracer attached to process (TracerPid: $pid)")
                            isTraced = true
                        }
                        break
                    }
                    line = reader.readLine()
                }
            }
            isTraced
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if Java debugger or JDWP listener is active.
     */
    fun checkDebuggerAttached(findings: MutableList<String> = mutableListOf()): Boolean {
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            findings.add("Java Debugger (JDWP) currently attached")
            return true
        }
        return false
    }
}
