import com.android.build.api.artifact.SingleArtifact
import com.ravendarque.deaftly.guard.VerifyReleaseManifestTask
import com.ravendarque.deaftly.help.CopyHelpTask
import com.ravendarque.deaftly.nativelibs.FetchNcnnTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.androidx.baselineprofile)
}

val appVersion = providers.gradleProperty("appVersion").orElse("0.0.0").get()
val appVersionParts =
    requireNotNull(Regex("""(\d{1,3})\.(\d{1,3})\.(\d{1,3})""").matchEntire(appVersion)) {
        "appVersion must be X.Y.Z with each part 0-999, not $appVersion"
    }.destructured.toList().map(String::toInt)
val (appMajor, appMinor, appPatch) = appVersionParts

val ncnnDist = rootProject.layout.projectDirectory.dir("third_party/ncnn/dist")

val fetchNcnn =
    tasks.register<FetchNcnnTask>("fetchNcnn") {
        url.set("https://github.com/Tencent/ncnn/releases/download/20261020/ncnn-20261020-android.zip")
        sha256.set("62a9edb493ceb42a5c9cbf2d3a79b3f44bff06c625134ffb64d183dcc42c25fc")
        abis.set(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        outputDir.set(ncnnDist)
    }

val releaseKeystore = providers.environmentVariable("ANDROID_KEYSTORE_PATH")

fun signingEnv(name: String): String =
    requireNotNull(providers.environmentVariable(name).orNull) {
        "$name must be set when ANDROID_KEYSTORE_PATH is"
    }

android {
    namespace = "com.ravendarque.deaftly"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ravendarque.deaftly"
        minSdk = 26
        targetSdk = 36
        versionName = appVersion
        manifestPlaceholders["profileInstallExported"] = "false"
        versionCode = (appMajor * 1_000_000 + appMinor * 1_000 + appPatch).coerceAtLeast(1)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                arguments(
                    "-DNCNN_ROOT=${ncnnDist.asFile.absolutePath}",
                    "-DANDROID_STL=c++_static",
                )
            }
        }
    }

    ndkVersion = "30.0.16248370"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }

    signingConfigs {
        if (releaseKeystore.isPresent) {
            create("release") {
                storeFile = file(releaseKeystore.get())
                storePassword = signingEnv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = signingEnv("ANDROID_KEY_ALIAS")
                keyPassword = signingEnv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        }
        release {
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api36") {
                    device = "Pixel 6"
                    apiLevel = 36
                    systemImageSource = "aosp-atd"
                    testedAbi = "x86_64"
                }
            }
        }
        unitTests.isIncludeAndroidResources = true
        // Robolectric reaches into JDK internals: https://robolectric.org/getting-started/
        unitTests.all {
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            )
        }
    }

    lint {
        warningsAsErrors = true
        checkDependencies = true
        error +=
            setOf(
                "ClickableViewAccessibility",
                "ContentDescription",
                "KeyboardInaccessibleWidget",
                "LabelFor",
                "AllowBackup",
                "ExportedContentProvider",
                "ExportedPreferenceActivity",
                "ExportedReceiver",
                "ExportedService",
                "HardcodedDebugMode",
                "SetJavaScriptEnabled",
                "TrustAllX509TrustManager",
                "UnprotectedSMSBroadcastReceiver",
                "UnsafeProtectedBroadcastReceiver",
                "WorldReadableFiles",
                "WorldWriteableFiles",
            )
        // Dependabot moves versions; a new upstream release must not fail unrelated PRs.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable", "OldTargetApi")
        // ADR-0010: release APKs are arm64-v8a and armeabi-v7a only; ChromeOS translates them.
        disable += "ChromeOsAbiSupport"
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.markdown.renderer.m3)
    baselineProfile(project(":baselineprofile"))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(testFixtures(project(":core")))

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

baselineProfile {
    dexLayoutOptimization = true
}

roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

androidComponents {
    onVariants { variant ->
        val copyHelp =
            tasks.register<CopyHelpTask>("copy${variant.name.replaceFirstChar { it.uppercase() }}HelpPages") {
                pages.from(fileTree(rootProject.layout.projectDirectory.dir("help")) { include("*.md") })
            }
        variant.sources.assets?.addGeneratedSourceDirectory(copyHelp, CopyHelpTask::outputDir)
    }
    finalizeDsl { extension ->
        extension.buildTypes
            .filter { it.name == "benchmarkRelease" || it.name == "nonMinifiedRelease" }
            .forEach {
                it.applicationIdSuffix = ".benchmark"
                it.ndk.abiFilters += "x86_64"
                it.manifestPlaceholders["profileInstallExported"] = "true"
            }
    }
    onVariants(selector().withBuildType("release")) { variant ->
        val verify =
            tasks.register<VerifyReleaseManifestTask>("verifyReleaseManifest") {
                mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                appNamespace.set(variant.namespace)
                permissionAllowlist.set(rootProject.layout.projectDirectory.file("config/permissions-allowlist.txt"))
                report.set(layout.buildDirectory.file("reports/verifyReleaseManifest/result.txt"))
            }
        tasks.named("check") { dependsOn(verify) }
    }
}

tasks.named("check") {
    dependsOn("verifyRoborazziDebug", "assembleRelease")
}

tasks
    .matching { it.name.startsWith("configureCMake") || it.name.startsWith("generateJsonModel") }
    .configureEach { dependsOn(fetchNcnn) }
