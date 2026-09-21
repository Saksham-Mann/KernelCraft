# prd.md — Product Requirements Document

## 1. Problem Statement
Operating systems concepts (memory management, syscalls, the kernel/userspace boundary, scheduling) are taught almost entirely through abstract diagrams and text. Learners rarely connect these abstractions to the physical silicon that executes them. KernelCraft closes that gap by literally taking the user on a visual journey — starting at a screw on a disassembled phone and ending at a syscall interrupting the CPU — so hardware and software feel like one continuous system rather than two disconnected subjects.

## 2. Target Audience
- **Primary:** CS undergraduates / self-taught engineers taking (or having taken) an intro OS course, who understand concepts abstractly but lack physical intuition.
- **Secondary:** Bootcamp grads and mobile/backend engineers who want a refresher on "what actually happens under the hood."
- **Tertiary:** Educators looking for a visual aid to demo in lecture (tablet/large-screen friendly).

Assumed device: mid-to-high-end Android phone/tablet, Android 10+ (API 29+), given ExoPlayer + heavy Compose animation requirements.

## 3. Core Modules

### 3.1 Selection Hub
Landing screen. A simple, colorful menu of learning "tracks" (e.g., "From Chip to Call", "Memory Deep Dive", future tracks). Each track is represented as a solid-color card. Selecting a track launches the scrollytelling experience for that track.

### 3.2 Hardware Teardown
A full-bleed ExoPlayer video of a physical device teardown (screws removed, back cover off, motherboard exposed, chip highlighted) that is **scroll-scrubbed** — video playback position is a direct function of scroll offset, not autoplay. The user "unscrews" the phone by scrolling.

### 3.3 Dive-In Transition
A short, GPU-cheap transition (using `graphicsLayer` scale/alpha/blur, NOT a second video) that visually zooms from the highlighted chip on the teardown video into an abstract representation of that chip's internals, bridging Module 3.2 and 3.4.

### 3.4 OS Architecture (Parallax Stack)
A vertically scrolling, parallax-layered diagram of the software stack (Hardware → Kernel → Drivers → System Call Interface → OS Services → Userspace Apps). Each layer is a full-viewport solid-color section with a white bottom-sheet explaining that layer, scrolling at a different speed than the background to create depth.

### 3.5 Syscall Simulator
An interactive, tap-driven (not scroll-driven) 5-step simulator showing a concrete syscall (e.g., `write()`) crossing from a userspace app, through libc, trapping into the kernel, executing, and returning. State-driven, fully interactive, no video.

## 4. Non-Goals
- Not a full OS course replacement — it's a conceptual/visual bridge, not exhaustive.
- Not cross-platform at v1 (Android-native only; no iOS/Flutter/KMP in scope).
- Not user-generated content — all tracks are authored, not user-built.

## 5. Success Metrics
| Metric | Target |
|---|---|
| Scroll-video frame drop rate | <5% dropped frames during scrubbing on mid-tier devices (e.g., Pixel 6a class) |
| Cold start → first video frame | <1.5s |
| Peak memory during Hardware Teardown | <250MB (no OOM on 4GB RAM devices) |
| Module completion rate (Hub → Simulator) | >60% of sessions that start Hardware Teardown reach Syscall Simulator |
| Syscall Simulator interaction rate | >80% of users who reach it tap through all 5 steps |
| Crash-free session rate | >99.5% |
| Qualitative: "I understood the hardware/software link better" | >4/5 avg in user testing survey |

## 6. Key Risks
- ExoPlayer scroll-scrubbing performance on low-end devices (mitigated via All-Intra encoding, see techspec.md).
- Parallax jank if too many layers recompose per scroll pixel (mitigated via derivedStateOf + snapshot reads, see rules.md).
- Video file size bloat from All-Intra encoding (mitigated via short clip duration + aggressive resolution/bitrate tuning, see techspec.md).