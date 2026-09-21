plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.kernelcraft"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kernelcraft"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20")
                arguments += listOf(
                    "-DANDROID_STL=c++_static"
                )
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        create("release") {
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
            val keystoreFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "dummy_keystore_password"
                keyAlias = System.getenv("KEY_ALIAS") ?: "kernelcraft"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "dummy_key_password"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            proguardFiles("benchmark-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        checkAllWarnings = true
        warningsAsErrors = true
        showAll = true
        explainIssues = true
        textReport = true
        textOutput = file("build/reports/lint-results.txt")
        htmlReport = true
        htmlOutput = file("build/reports/lint-results.html")
        xmlReport = true
        xmlOutput = file("build/reports/lint-results.xml")

        fatal += setOf(
            "HardcodedDebugMode",
            "ExportedPreferenceProvider",
            "ExportedReceiver",
            "ExportedService",
            "ExportedContentProvider",
            "UnprotectedSMSBroadcastReceiver",
            "BadHostnameVerifier",
            "TrustAllX509TrustManager",
            "InsecureBaseConfiguration",
            "HardwareIds",
            "SecureRandom",
            "GetInstance",
            "TrulyRandom",
            "PackageVisibility",
            "GrantUriPermission",
            "SetWorldReadable",
            "SetWorldWritable"
        )
        enable += setOf(
            "Security",
            "NetworkSecurityConfig",
            "ProtectedPermissions"
        )
    }
}

dependencies {
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    implementation(libs.androidx.security.crypto)
    implementation(libs.play.integrity)

    testImplementation("junit:junit:4.13.2")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

apply(from = rootProject.file("gradle/security-audit.gradle.kts"))

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.register("generateSbom") {
    group = "verification"
    description = "Generates an automated CycloneDX-compliant Software Bill of Materials (SBOM)."
    val bomFile = file("build/reports/bom.json")

    doLast {
        bomFile.parentFile.mkdirs()
        val dependenciesJson = mutableListOf<String>()

        configurations.findByName("releaseRuntimeClasspath")?.resolvedConfiguration?.lenientConfiguration?.allModuleDependencies?.forEach { dep ->
            dependenciesJson.add(
                """    {
      "type": "library",
      "group": "${dep.moduleGroup}",
      "name": "${dep.moduleName}",
      "version": "${dep.moduleVersion}",
      "purl": "pkg:maven/${dep.moduleGroup}/${dep.moduleName}@${dep.moduleVersion}"
    }"""
            )
        }

        // Use a safe timestamp generation method that works in Gradle Kotlin DSL
        val timestamp = System.currentTimeMillis().toString()

        bomFile.writeText(
            """{
  "bomFormat": "CycloneDX",
  "specVersion": "1.5",
  "version": 1,
  "metadata": {
    "timestamp": "$timestamp",
    "component": {
      "type": "application",
      "name": "KernelCraft",
      "version": "1.0"
    }
  },
  "components": [
${dependenciesJson.joinToString(",\n")}
  ]
}"""
        )
        println("\n[SBOM] Successfully generated CycloneDX Software Bill of Materials at: ${bomFile.path}\n")
    }
}
