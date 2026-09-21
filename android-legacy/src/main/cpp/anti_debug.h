#pragma once

#include <stdbool.h>

namespace kernelcraft::antidebug {

/**
 * Executes ptrace(PTRACE_TRACEME, 0, 1, 0) via raw direct syscall.
 * Returns true if another tracer is already attached.
 */
bool check_and_deny_ptrace();

/**
 * Parses /proc/self/status using direct raw syscalls to detect TracerPid != 0.
 * Returns true if an active tracer is detected.
 */
bool is_tracer_pid_detected();

/**
 * Spawns an isolated native background pthread that continuously monitors
 * TracerPid via raw syscalls. If TracerPid != 0, triggers an uncatchable
 * exit via raw_exit_group(-1).
 */
void start_watchdog_thread();

/**
 * Master debugger presence check.
 */
bool is_debugger_present();

} // namespace kernelcraft::antidebug
