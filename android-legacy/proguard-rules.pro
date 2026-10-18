# ==============================================================================
# KernelCraft Production R8 & ProGuard Optimization Rules
# ==============================================================================

# 1. Native JNI & MediaCodec Surface Bindings
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class androidx.media3.exoplayer.mediacodec.** {
    public *;
}

-keep class androidx.media3.exoplayer.video.** {
    public *;
}

-keep class androidx.media3.ui.PlayerView {
    public *;
}

-keep class androidx.media3.ui.AspectRatioFrameLayout {
    public *;
}

# 2. Dead-Code Stripping for Unused ExoPlayer Subsystems
# Strip unused network streaming protocols (HLS, DASH, SmoothStreaming, RTSP)
-dontwarn androidx.media3.exoplayer.dash.**
-dontwarn androidx.media3.exoplayer.hls.**
-dontwarn androidx.media3.exoplayer.smoothstreaming.**
-dontwarn androidx.media3.exoplayer.rtsp.**

# Strip audio decoders not needed for silent video scrubbing
-dontwarn androidx.media3.extractor.flac.**
-dontwarn androidx.media3.extractor.ogg.**
-dontwarn androidx.media3.extractor.ts.**
-dontwarn androidx.media3.extractor.wav.**
-dontwarn androidx.media3.extractor.mkv.**

# Preserve MP4 / All-Intra H.264 video extractor
-keep class androidx.media3.extractor.mp4.** {
    public *;
}

-keep class androidx.media3.exoplayer.video.MediaCodecVideoRenderer {
    public *;
}

# 3. Reflection-Free Serialization & Data Class Integrity
-keepclassmembers class com.kernelcraft.feature.teardown.TeardownUiState { *; }
-keepclassmembers class com.kernelcraft.feature.teardown.TeardownMilestone { *; }
-keepclassmembers class com.kernelcraft.feature.teardown.annotation.** { *; }
-keepclassmembers class com.kernelcraft.feature.kernel.KernelStackModel** { *; }
-keepclassmembers class com.kernelcraft.feature.kernel.OsTier { *; }
-keepclassmembers class com.kernelcraft.feature.kernel.CpuGovernorMode { *; }
-keepclassmembers class com.kernelcraft.feature.kernel.ClusterType { *; }
-keepclassmembers class com.kernelcraft.feature.kernel.BinderTransactionPhase { *; }
-keepclassmembers class com.kernelcraft.feature.detail.ui.CameraLensModule { *; }
-keepclassmembers class com.kernelcraft.feature.detail.ui.ChargeMode { *; }
-keepclassmembers class com.kernelcraft.core.player.PlayerMetrics { *; }

# 4. Jetpack Compose Runtime & State Optimization
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keepclassmembers class androidx.compose.runtime.Recomposer { *; }
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# 5. Baseline Profile & ProfileInstaller
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**

# 6. General Code Shrinking & Aggressive Obfuscation Directives
-repackageclasses 'com.kernelcraft.a'
-allowaccessmodification
-mergeinterfacesaggressively

# 7. Anti-Tamper & Binary Protection: Strip Source Metadata, Line Numbers & Debug Logs
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable,!Deprecated

# Strip development logs in release build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
