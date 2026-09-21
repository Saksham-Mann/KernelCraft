cmake_minimum_required(VERSION 3.22.1)

project("kernelcraft_secure" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Hardened Compiler Flags
add_compile_options(
    -Wall
    -Wextra
    -fvisibility=hidden
    -fvisibility-inlines-hidden
    -fstack-protector-strong
    -D_FORTIFY_SOURCE=2
    -fPIE
    -O3
)

# Stripping, 16KB Page Alignment, and Linker Hardening Flags
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--strip-all -Wl,--exclude-libs,ALL -Wl,--gc-sections -s -Wl,-z,max-page-size=16384")

add_library(
    kernelcraft_secure
    SHARED
    anti_debug.cpp
    anti_dump.cpp
    jni_bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    kernelcraft_secure
    ${log-lib}
    ${android-lib}
)
