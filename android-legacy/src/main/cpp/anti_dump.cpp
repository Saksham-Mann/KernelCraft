#include "anti_dump.h"
#include "syscalls.h"
#include "obfuscation.h"

#include <sys/inotify.h>
#include <link.h>
#include <pthread.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <signal.h>
#include <atomic>

namespace kernelcraft::antidump {

static std::atomic<bool> g_inotify_started{false};
static std::atomic<bool> g_dump_detected{false};

static void *inotify_worker(void * /* arg */) {
    int ifd = inotify_init1(IN_NONBLOCK | IN_CLOEXEC);
    if (ifd < 0) {
        return nullptr;
    }

    std::string mem_path = OBFUSCATE_STR("/proc/self/mem");
    std::string pagemap_path = OBFUSCATE_STR("/proc/self/pagemap");

    inotify_add_watch(ifd, mem_path.c_str(), IN_ACCESS | IN_OPEN);
    inotify_add_watch(ifd, pagemap_path.c_str(), IN_ACCESS | IN_OPEN);

    char event_buf[1024];

    while (true) {
        usleep(500000); // 500ms poll interval

        ssize_t len = raw_read(ifd, event_buf, sizeof(event_buf));
        if (len > 0) {
            // Unauthorized memory dump attempt detected (e.g. Fridump, GameGuardian, ptrace mem scraper)
            g_dump_detected.store(true);
        }
    }
    raw_close(ifd);
    return nullptr;
}

void start_inotify_watcher() {
    bool expected = false;
    if (g_inotify_started.compare_exchange_strong(expected, true)) {
        pthread_t tid;
        pthread_create(&tid, nullptr, inotify_worker, nullptr);
        pthread_detach(tid);
    }
}

struct PhdrScanContext {
    bool suspicious_found;
};

bool scan_loaded_modules() {
    PhdrScanContext ctx{false};

    dl_iterate_phdr([](struct dl_phdr_info *info, size_t /* size */, void *data) -> int {
        auto *c = static_cast<PhdrScanContext *>(data);
        if (!info || !info->dlpi_name) return 0;

        const char *lib_name = info->dlpi_name;
        if (strlen(lib_name) == 0) return 0;

        // Compare against obfuscated target signatures
        std::string frida = OBFUSCATE_STR("frida");
        std::string gadget = OBFUSCATE_STR("gadget");
        std::string substrate = OBFUSCATE_STR("substrate");
        std::string xposed = OBFUSCATE_STR("xposed");
        std::string membi = OBFUSCATE_STR("libmembi");

        if (strstr(lib_name, frida.c_str()) != nullptr ||
            strstr(lib_name, gadget.c_str()) != nullptr ||
            strstr(lib_name, substrate.c_str()) != nullptr ||
            strstr(lib_name, xposed.c_str()) != nullptr ||
            strstr(lib_name, membi.c_str()) != nullptr) {
            c->suspicious_found = true;
            return 1; // Terminate iteration early
        }

        return 0;
    }, &ctx);

    return ctx.suspicious_found;
}

uint32_t compute_text_segment_checksum() {
    struct ChecksumContext {
        uint32_t checksum;
    };
    ChecksumContext ctx{0x811c9dc5}; // FNV-1a 32-bit offset basis

    dl_iterate_phdr([](struct dl_phdr_info *info, size_t /* size */, void *data) -> int {
        auto *c = static_cast<ChecksumContext *>(data);
        if (!info || !info->dlpi_name) return 0;

        // Target our own library
        std::string lib_name = OBFUSCATE_STR("kernelcraft_secure");
        if (strstr(info->dlpi_name, lib_name.c_str()) == nullptr) {
            return 0;
        }

        for (int i = 0; i < info->dlpi_phnum; ++i) {
            const ElfW(Phdr) *phdr = &info->dlpi_phdr[i];
            // Executable segment (PT_LOAD with PF_X)
            if (phdr->p_type == PT_LOAD && (phdr->p_flags & PF_X)) {
                const uint8_t *code_bytes = reinterpret_cast<const uint8_t *>(info->dlpi_addr + phdr->p_vaddr);
                size_t code_len = phdr->p_filesz;

                // Compute FNV-1a hash over .text bytes (up to 4096 bytes sample)
                size_t sample_len = code_len < 4096 ? code_len : 4096;
                for (size_t b = 0; b < sample_len; ++b) {
                    c->checksum ^= code_bytes[b];
                    c->checksum *= 0x01000193; // FNV prime
                }
                return 1; // Done
            }
        }
        return 0;
    }, &ctx);

    return ctx.checksum;
}

bool is_memory_tampered() {
    int seed = 17;
    if (::kernelcraft::crypto::opaque_invariant_false(seed)) {
        return false;
    }

    if (g_dump_detected.load()) {
        return true;
    }

    return scan_loaded_modules();
}

} // namespace kernelcraft::antidump
