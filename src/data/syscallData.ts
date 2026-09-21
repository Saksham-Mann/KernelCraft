export interface SyscallStep {
  stepIndex: number;
  title: string;
  subtitle: string;
  ringMode: 'USER' | 'KERNEL';
  ringNumber: 'Ring 3 (EL0)' | 'Ring 0 (EL1)';
  codeSnippet: string;
  explanation: string;
  highlightedBox: 'APP' | 'LIBC' | 'TRAP' | 'DISPATCH' | 'RETURN';
  registerState: {
    x8: string;  // Syscall number (e.g., 64 for write on ARM64)
    x0: string;  // Argument 1 (file descriptor)
    x1: string;  // Argument 2 (buffer pointer)
    x2: string;  // Argument 3 (count)
  };
}

export const SYSCALL_STEPS: SyscallStep[] = [
  {
    stepIndex: 0,
    title: '1. User Application Calls write()',
    subtitle: 'High-level application code requests data transmission to standard output.',
    ringMode: 'USER',
    ringNumber: 'Ring 3 (EL0)',
    codeSnippet: `// Userspace Application
const bytesWritten = write(fd, "KernelCraft\\n", 12);`,
    explanation:
      'The application thread executes in unprivileged User Mode (EL0). It cannot directly access hardware peripherals or physical memory mapped to other processes.',
    highlightedBox: 'APP',
    registerState: {
      x8: '0x00 (Unassigned)',
      x0: '1 (stdout)',
      x1: '0x7ffcd3e4a0 (buf)',
      x2: '12 (bytes)',
    },
  },
  {
    stepIndex: 1,
    title: '2. Bionic Libc Marshals Arguments',
    subtitle: 'Standard C library places syscall number into register x8.',
    ringMode: 'USER',
    ringNumber: 'Ring 3 (EL0)',
    codeSnippet: `// Bionic libc wrapper (/bionic/libc/arch-arm64/syscalls/__write.S)
mov x8, #64        // __NR_write syscall number
svc #0             // Supervisor Call (Trap into Kernel)`,
    explanation:
      'The C library wrapper places the syscall index (__NR_write = 64) into register x8 and sets up the parameter registers (x0: fd, x1: buf, x2: count).',
    highlightedBox: 'LIBC',
    registerState: {
      x8: '64 (__NR_write)',
      x0: '1 (stdout)',
      x1: '0x7ffcd3e4a0 (buf)',
      x2: '12 (bytes)',
    },
  },
  {
    stepIndex: 2,
    title: '3. CPU Trap & Mode Switch (EL0 → EL1)',
    subtitle: 'Hardware interrupt switches CPU privilege ring from User to Kernel mode.',
    ringMode: 'KERNEL',
    ringNumber: 'Ring 0 (EL1)',
    codeSnippet: `// CPU Hardware Architecture Exception Trap
CPU switches PSTATE.M to EL1t (Supervisor)
Saves program counter (PC) into ELR_EL1
Jumps to kernel exception vector table (VBAR_EL1 + 0x400)`,
    explanation:
      'Executing `svc #0` triggers a synchronous CPU exception. The hardware immediately enforces kernel memory access protections and locks execution to kernel address space.',
    highlightedBox: 'TRAP',
    registerState: {
      x8: '64 (__NR_write)',
      x0: '1 (stdout)',
      x1: '0xffff800010a (kmapped)',
      x2: '12 (bytes)',
    },
  },
  {
    stepIndex: 3,
    title: '4. Kernel Syscall Dispatch Table',
    subtitle: 'Linux kernel validates permissions and routes to sys_write().',
    ringMode: 'KERNEL',
    ringNumber: 'Ring 0 (EL1)',
    codeSnippet: `// Linux Kernel (fs/read_write.c)
SYSCALL_DEFINE3(write, unsigned int, fd, const char __user *, buf, size_t, count) {
    struct fd f = fdget_pos(fd);
    ret = vfs_write(f.file, buf, count, &pos);
    return ret;
}`,
    explanation:
      'The kernel checks whether the file descriptor is valid and owned by the calling process. It safely copies the buffer from userspace memory and dispatches to the device driver / VFS.',
    highlightedBox: 'DISPATCH',
    registerState: {
      x8: '64 (__NR_write)',
      x0: '12 (bytes written)',
      x1: '0x00 (clean)',
      x2: '0x00 (clean)',
    },
  },
  {
    stepIndex: 4,
    title: '5. Return from Exception (eret)',
    subtitle: 'Result returned in x0, CPU flips back to unprivileged User Mode.',
    ringMode: 'USER',
    ringNumber: 'Ring 3 (EL0)',
    codeSnippet: `// Kernel returns to user space
eret               // Restores PC from ELR_EL1, drops to EL0
// User space resumes execution with result = 12 bytes`,
    explanation:
      'The CPU executes `eret`, dropping privileges back to EL0 User Mode. The application thread resumes seamlessly, receiving the return value (12 bytes written) in register x0.',
    highlightedBox: 'RETURN',
    registerState: {
      x8: '0x00',
      x0: '12 (success: 12 bytes)',
      x1: '0x7ffcd3e4a0',
      x2: '0',
    },
  },
];
