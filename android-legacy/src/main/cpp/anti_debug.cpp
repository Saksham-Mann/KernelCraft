#include "anti_debug.h"
#include "syscalls.h"
#include "obfuscation.h"

#include <pthread.h>
#include <sys/ptrace.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <atomic>

namespace kernelcraft::antidebug {

static std::atomic<bool> g_watchdog_started{false};

bool check_and_deny_ptrace() {
    // Calling ptrace(PTRACE_TRACEME) via direct raw syscall
    long res = raw_ptrace(PTRACE_TRACEME, 0, nullptr, nullptr);
    if (res < 0) {
        return true; // Another debugger/Frida process is already attached!
    }
    return false;
}

bool is_tracer_pid_detected() {
    std::string status_path = OBFUSCATE_STR("/proc/self/status");
    int fd = raw_openat(AT_FDCWD, status_path.c_str(), O_RDONLY, 0);
    if (fd < 0) {
        return false;
    }

    char buffer[2048];
    ssize_t bytes_read = raw_read(fd, buffer, sizeof(buffer) - 1);
    raw_close(fd);

    if (bytes_read <= 0) {
        return false;
    }
    buffer[bytes_read] = '\0';

    std::string needle = OBFUSCATE_STR("TracerPid:");
    char *pos = strstr(buffer, needle.c_str());
    if (!pos) {
        return false;
    }

    pos += needle.length();
    // Skip whitespace/tabs
    while (*pos == ' ' || *pos == '\t') {
        pos++;
    }

    // If TracerPid is non-zero (i.e. not '0'), a debugger is active
    if (*pos != '\0' && *pos != '0' && *pos >= '0' && *pos <= '9') {
        return true;
    }

    return false;
}

static std::atomic<bool> g_tracer_detected{false};

static void *watchdog_worker(void * /* arg */) {
    while (true) {
        // Sleep 500ms between checks
        usleep(500000);

        if (is_tracer_pid_detected()) {
            g_tracer_detected.store(true);
        }
    }
    return nullptr;
}

void start_watchdog_thread() {
    bool expected = false;
    if (g_watchdog_started.compare_exchange_strong(expected, true)) {
        pthread_t tid;
        pthread_create(&tid, nullptr, watchdog_worker, nullptr);
        pthread_detach(tid);
    }
}

bool is_debugger_present() {
    if (g_tracer_detected.load()) {
        return true;
    }
    // Use opaque predicate to obscure control flow
    int seed = 42;
    if (::kernelcraft::crypto::opaque_invariant_false(seed)) {
        return false;
    }

    if (check_and_deny_ptrace()) {
        return true;
    }

    if (is_tracer_pid_detected()) {
        return true;
    }

    return false;
}

} // namespace kernelcraft::antidebug
