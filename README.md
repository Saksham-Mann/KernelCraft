# KernelCraft

From Silicon to Syscall: An Interactive Hardware and Operating System Explorer.

## Overview

KernelCraft is an interactive educational mobile application built with Expo and React Native that bridges the divide between physical mobile hardware and operating system abstractions.

Most computer science curricula teach operating systems through abstract diagrams and textbook descriptions, leaving learners disconnected from the physical silicon executing their code. KernelCraft takes learners on an unbroken conceptual journey: beginning with an interactive physical hardware teardown of a smartphone, diving directly into the SoC, stepping through the hierarchical layers of the OS stack, and simulating execution mechanics down to the kernel syscall interface.

## Key Features

### 1. Hardware Teardown and Inspection
- Interactive 60-frame scrubbed teardown sequence that correlates physical disassembly and assembly to user gesture input.
- Interactive component hotspot annotations across critical hardware modules:
  - Application Processor and SoC (System on Chip)
  - LPDDR5 RAM and UFS Storage substrate
  - Multi-lens CMOS camera and optical image stabilization array
  - High-density Li-ion battery envelope
  - Qi wireless induction charging coil
  - Display and digitizer assembly
- Deep component telemetry inspector cards displaying architecture specifications, bus interfaces, power envelopes, and thermal constraints.
- Dedicated hardware specification matrix view (`/specs`) and detailed per-component deep dives (`/detail/[id]`).

### 2. Dive-In Transition
- Animated visual transition bridging the macroscopic physical silicon on the motherboard into microarchitectural diagrams and software execution environments.

### 3. Hierarchical OS Architecture Explorer
- Six-tier architectural navigation covering the full modern mobile operating system stack:
  - Userspace Applications: High-level UI, background services, and application processes.
  - Android Framework and IPC Binder: System services (ActivityManager, WindowManager, PackageManager) and inter-process communication.
  - Android Runtime (ART) and Native Daemons: Ahead-of-Time / Just-in-Time compilation, garbage collection, and native daemons (init, logd).
  - Hardware Abstraction Layer (HAL) and Device Drivers: Standardized hardware interfaces and kernel module drivers.
  - Linux Kernel: Process scheduler, virtual memory manager, network stack, and security subsystem.
  - Physical Silicon (SoC): CPU cores, GPU, NPU, DSP, and interconnect buses.

### 4. Interactive System Visualizers and Simulators
- Syscall Simulator: A 5-step guided interactive trace demonstrating a syscall (such as `write()`) crossing from userspace application code through the standard C library wrapper, triggering a CPU trap and mode switch into kernel space, executing in the kernel dispatch table, and returning to userspace.
- CPU Scheduler Visualizer: Interactive simulation of the Completely Fair Scheduler (CFS), task queues, dynamic priority adjustments, time slice distribution, and multi-core execution.
- Virtual Memory Space Visualizer: Side-by-side visualization of virtual address spaces, page tables, Translation Lookaside Buffer (TLB) hits and misses, physical page frames, and page fault handling.
- IPC Binder Tracer: Interactive transaction tracer showing client threads, the in-kernel Binder driver (`/dev/binder`), Service Manager lookups, and server execution threads.
- App Execution Simulator: Dynamic lifecycle simulation showing real-time memory usage, thread states, pending IPC requests, and active kernel syscall counters.
- Device Inspector: Real-time telemetry inspector displaying runtime platform details, device architecture, and hardware capabilities.

## Technical Architecture and Stack

- Framework: React Native 0.76.6 with Expo SDK 52
- Routing: Expo Router v4 (file-based routing architecture)
- Language: TypeScript 5.3 with strict typing
- Animation and Interaction: React Native Animated API, Expo Haptics, and PanResponder gesture handlers
- Graphics and Media: Expo Image, React Native SVG, and Ionicons (@expo/vector-icons)
- Platform Support: Android, iOS, and Web (React Native Web)

## Project Structure

```
kernelCraft/
├── app/                          # Expo Router file-based route definitions
│   ├── _layout.tsx               # Root layout, Stack navigator, and status bar
│   ├── index.tsx                 # Cold start and splash screen
│   ├── teardown.tsx              # Hardware teardown scrubber and simulator
│   ├── transition.tsx            # Silicon zoom transition
│   ├── kernel.tsx                # OS Architecture and interactive visualizers
│   ├── specs.tsx                 # Hardware specification matrix
│   ├── detail/
│   │   └── [id].tsx              # Dynamic component telemetry detail page
│   └── +not-found.tsx            # Fallback 404 handler
├── src/
│   ├── components/               # Modular UI components and visualizers
│   │   ├── AnnotationOverlay.tsx         # Teardown hotspot badges
│   │   ├── AppExecutionSimulator.tsx     # Process lifecycle simulation
│   │   ├── BinderTracer.tsx              # Binder IPC communication flow
│   │   ├── CpuSchedulerVisualizer.tsx    # CPU CFS scheduling queue
│   │   ├── IsometricSiliconCanvas.tsx    # Layered architecture stack view
│   │   ├── MemorySpaceVisualizer.tsx     # Virtual memory and page tables
│   │   ├── MilestoneCard.tsx             # Teardown milestone summary cards
│   │   ├── MyDeviceInspector.tsx         # Live device telemetry inspector
│   │   ├── SyscallSimulatorView.tsx      # Step-by-step syscall stepper
│   │   └── TeardownScrubber.tsx          # Manual gesture-based scrubber
│   ├── data/                     # Domain models, telemetry, and datasets
│   │   ├── kernelStackData.ts    # OS tier definitions and descriptions
│   │   ├── specsData.ts          # Hardware telemetry specification matrix
│   │   ├── syscallData.ts        # Syscall simulation step definitions
│   │   └── teardownData.ts       # Teardown frames, hotspots, and telemetry
│   └── theme/                    # Design tokens
│       ├── colors.ts             # Palette constants
│       └── typography.ts         # Typography styles and sizing
├── assets/                       # Image assets and teardown video frames
├── md files/                     # Product requirements, tech specs, and design docs
│   ├── AppFlow.md                # Screen-by-screen navigation journey
│   ├── Design.md                 # Design system and aesthetics guidelines
│   ├── Implementation.md         # Architecture and implementation notes
│   ├── Prd.md                    # Product requirements document
│   ├── Rules.md                  # Development and performance rules
│   ├── Schema.md                 # Data schema specifications
│   ├── Techspec.md               # Technical specification document
│   └── Tracker.md                # Feature completion and roadmap tracking
├── android-legacy/               # Original Jetpack Compose native codebase
├── app.json                      # Expo application configuration
├── package.json                  # Dependencies and scripts
└── tsconfig.json                 # TypeScript compiler configuration
```

## Getting Started

### Prerequisites

- Node.js (version 18.0.0 or higher recommended)
- npm or yarn package manager
- Expo Go on an Android or iOS device, or an active Android Emulator / iOS Simulator / Web browser

### Installation

1. Clone the repository and switch to the `expo-transfer` branch:
   ```bash
   git clone https://github.com/Saksham-Mann/KernelCraft.git
   cd KernelCraft
   git checkout expo-transfer
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Validate TypeScript compilation:
   ```bash
   npx tsc --noEmit
   ```

### Running the Application

- Start the Expo development server:
  ```bash
  npm start
  ```

- Run on Android:
  ```bash
  npm run android
  ```

- Run on iOS:
  ```bash
  npm run ios
  ```

- Run in Web browser:
  ```bash
  npm run web
  ```

## Development and Branch Information

- Active Branch: `expo-transfer`
- The project was migrated from an Android Jetpack Compose prototype into a cross-platform Expo React Native application featuring interactive visualizers and zero runtime warnings.
