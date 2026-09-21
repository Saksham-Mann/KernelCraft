#pragma once

#include <stddef.h>
#include <stdint.h>
#include <string>

/**
 * C++20 Compile-Time String Obfuscation & Opaque Predicates Engine
 *
 * Guarantees that sensitive strings (paths, symbols, canary keys) do not appear
 * in the .rodata section or `strings libkernelcraft_secure.so`.
 * Strings are encrypted at compile-time via constexpr XOR and decrypted
 * into ephemeral stack memory on demand.
 */

namespace kernelcraft::crypto {

template <size_t N, uint8_t Key>
class ObfuscatedString {
public:
    constexpr ObfuscatedString(const char (&str)[N]) {
        for (size_t i = 0; i < N; ++i) {
            encrypted_data[i] = static_cast<uint8_t>(str[i]) ^ static_cast<uint8_t>(Key + (i * 7));
        }
    }

    // Decrypts string into an ephemeral stack buffer
    std::string decrypt() const {
        char buffer[N];
        for (size_t i = 0; i < N; ++i) {
            buffer[i] = static_cast<char>(encrypted_data[i] ^ static_cast<uint8_t>(Key + (i * 7)));
        }
        return std::string(buffer, N - 1);
    }

private:
    uint8_t encrypted_data[N]{};
};

// Generates a pseudo-random compile-time byte key from __TIME__
constexpr uint8_t generate_compile_key() {
    uint8_t key = 0x5A;
    const char *time = __TIME__; // "HH:MM:SS"
    for (size_t i = 0; time[i] != '\0'; ++i) {
        key = static_cast<uint8_t>((key * 33) ^ time[i]);
    }
    return key == 0 ? 0xA5 : key;
}

#define OBFUSCATE_KEY (::kernelcraft::crypto::generate_compile_key() ^ (__LINE__ & 0xFF))

#define OBFUSCATE_STR(str) \
    ([]() -> std::string { \
        constexpr ::kernelcraft::crypto::ObfuscatedString<sizeof(str), OBFUSCATE_KEY> obf(str); \
        return obf.decrypt(); \
    })()

/**
 * Opaque Predicates
 * Mathematical invariants that evaluate to true or false at runtime,
 * defeating simple NOP patching and static decompilation analysis.
 */
static inline bool opaque_invariant_true(int32_t val) {
    // For any integer x, x*(x+1) is always even
    uint32_t uval = static_cast<uint32_t>(val);
    uint32_t prod = uval * (uval + 1);
    return (prod & 1) == 0;
}

static inline bool opaque_invariant_false(int32_t val) {
    // Square of an integer in 2's complement cannot be negative when sign-extended
    int64_t sq = static_cast<int64_t>(val) * val;
    return sq < 0;
}

} // namespace kernelcraft::crypto
