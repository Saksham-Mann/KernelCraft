#pragma once

#include <stdbool.h>
#include <stdint.h>

namespace kernelcraft::antidump {

/**
 * Initializes Linux inotify file watchers on /proc/self/mem, /proc/self/maps,
 * and /proc/self/pagemap. Unauthorized access triggers an instant SIGSEGV/abort.
 */
void start_inotify_watcher();

/**
 * Iterates dynamically linked ELF headers via dl_iterate_phdr to detect
 * injected dynamic libraries (frida, gadget, substrate, xposed).
 * Returns true if rogue libraries are detected.
 */
bool scan_loaded_modules();

/**
 * Computes a 32-bit checksum of the .text executable segment of libkernelcraft_secure.so
 * to detect in-memory inline function hooking (e.g. Frida interceptor replacing function prologue).
 */
uint32_t compute_text_segment_checksum();

/**
 * Master memory integrity evaluation.
 */
bool is_memory_tampered();

} // namespace kernelcraft::antidump
