#pragma once

#include <sys/syscall.h>
#include <unistd.h>
#include <fcntl.h>
#include <stddef.h>
#include <stdint.h>

/**
 * Direct Kernel Syscall Engine
 *
 * Invokes kernel syscalls via raw inline assembly to bypass libc wrappers (libc.so).
 * This thwarts PLT/GOT function hooking, Frida interceptors on libc open/read/ptrace,
 * and user-space trampoline redirection.
 */

#if defined(__aarch64__)

static inline long raw_syscall0(long n) {
    register long x8 __asm__("x8") = n;
    register long x0 __asm__("x0");
    __asm__ __volatile__(
        "svc #0"
        : "=r"(x0)
        : "r"(x8)
        : "memory"
    );
    return x0;
}

static inline long raw_syscall1(long n, long a1) {
    register long x8 __asm__("x8") = n;
    register long x0 __asm__("x0") = a1;
    __asm__ __volatile__(
        "svc #0"
        : "+r"(x0)
        : "r"(x8)
        : "memory"
    );
    return x0;
}

static inline long raw_syscall3(long n, long a1, long a2, long a3) {
    register long x8 __asm__("x8") = n;
    register long x0 __asm__("x0") = a1;
    register long x1 __asm__("x1") = a2;
    register long x2 __asm__("x2") = a3;
    __asm__ __volatile__(
        "svc #0"
        : "+r"(x0)
        : "r"(x8), "r"(x1), "r"(x2)
        : "memory"
    );
    return x0;
}

static inline long raw_syscall4(long n, long a1, long a2, long a3, long a4) {
    register long x8 __asm__("x8") = n;
    register long x0 __asm__("x0") = a1;
    register long x1 __asm__("x1") = a2;
    register long x2 __asm__("x2") = a3;
    register long x3 __asm__("x3") = a4;
    __asm__ __volatile__(
        "svc #0"
        : "+r"(x0)
        : "r"(x8), "r"(x1), "r"(x2), "r"(x3)
        : "memory"
    );
    return x0;
}

#elif defined(__arm__)

static inline long raw_syscall0(long n) {
    long ret;
    __asm__ __volatile__(
        "mov r12, r7\n\t"
        "mov r7, %1\n\t"
        "svc 0\n\t"
        "mov r7, r12\n\t"
        : "=r"(ret)
        : "r"(n)
        : "r12", "memory"
    );
    return ret;
}

static inline long raw_syscall1(long n, long a1) {
    register long r0 __asm__("r0") = a1;
    __asm__ __volatile__(
        "mov r12, r7\n\t"
        "mov r7, %1\n\t"
        "svc 0\n\t"
        "mov r7, r12\n\t"
        : "+r"(r0)
        : "r"(n)
        : "r12", "memory"
    );
    return r0;
}

static inline long raw_syscall3(long n, long a1, long a2, long a3) {
    register long r0 __asm__("r0") = a1;
    register long r1 __asm__("r1") = a2;
    register long r2 __asm__("r2") = a3;
    __asm__ __volatile__(
        "mov r12, r7\n\t"
        "mov r7, %4\n\t"
        "svc 0\n\t"
        "mov r7, r12\n\t"
        : "+r"(r0)
        : "r"(r0), "r"(r1), "r"(r2), "r"(n)
        : "r12", "memory"
    );
    return r0;
}

static inline long raw_syscall4(long n, long a1, long a2, long a3, long a4) {
    register long r0 __asm__("r0") = a1;
    register long r1 __asm__("r1") = a2;
    register long r2 __asm__("r2") = a3;
    register long r3 __asm__("r3") = a4;
    __asm__ __volatile__(
        "mov r12, r7\n\t"
        "mov r7, %5\n\t"
        "svc 0\n\t"
        "mov r7, r12\n\t"
        : "+r"(r0)
        : "r"(r0), "r"(r1), "r"(r2), "r"(r3), "r"(n)
        : "r12", "memory"
    );
    return r0;
}

#elif defined(__x86_64__)

static inline long raw_syscall0(long n) {
    long ret;
    __asm__ __volatile__(
        "syscall"
        : "=a"(ret)
        : "a"(n)
        : "rcx", "r11", "memory"
    );
    return ret;
}

static inline long raw_syscall1(long n, long a1) {
    long ret;
    __asm__ __volatile__(
        "syscall"
        : "=a"(ret)
        : "a"(n), "D"(a1)
        : "rcx", "r11", "memory"
    );
    return ret;
}

static inline long raw_syscall3(long n, long a1, long a2, long a3) {
    long ret;
    __asm__ __volatile__(
        "syscall"
        : "=a"(ret)
        : "a"(n), "D"(a1), "S"(a2), "d"(a3)
        : "rcx", "r11", "memory"
    );
    return ret;
}

static inline long raw_syscall4(long n, long a1, long a2, long a3, long a4) {
    long ret;
    register long r10 __asm__("r10") = a4;
    __asm__ __volatile__(
        "syscall"
        : "=a"(ret)
        : "a"(n), "D"(a1), "S"(a2), "d"(a3), "r"(r10)
        : "rcx", "r11", "memory"
    );
    return ret;
}

#else

// Fallback for host simulation or unsupported architectures
static inline long raw_syscall0(long n) { return syscall(n); }
static inline long raw_syscall1(long n, long a1) { return syscall(n, a1); }
static inline long raw_syscall3(long n, long a1, long a2, long a3) { return syscall(n, a1, a2, a3); }
static inline long raw_syscall4(long n, long a1, long a2, long a3, long a4) { return syscall(n, a1, a2, a3, a4); }

#endif

// High-level raw wrappers
static inline long raw_ptrace(long request, long pid, void *addr, void *data) {
    return raw_syscall4(__NR_ptrace, request, pid, (long)addr, (long)data);
}

static inline int raw_openat(int dirfd, const char *pathname, int flags, int mode) {
    return (int)raw_syscall4(__NR_openat, (long)dirfd, (long)pathname, (long)flags, (long)mode);
}

static inline ssize_t raw_read(int fd, void *buf, size_t count) {
    return (ssize_t)raw_syscall3(__NR_read, (long)fd, (long)buf, (long)count);
}

static inline int raw_close(int fd) {
    return (int)raw_syscall1(__NR_close, (long)fd);
}

__attribute__((noreturn)) static inline void raw_exit_group(int status) {
    raw_syscall1(__NR_exit_group, (long)status);
    _exit(status);
}
