import com.android.build.api.artifact.SingleArtifact
import com.oneloneraven.deaftly.guard.VerifyReleaseManifestTask
import com.oneloneraven.deaftly.help.CopyHelpTask
import com.oneloneraven.deaftly.nativelibs.FetchArchiveTask

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

val ncnnAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")

val fetchNcnn =
    tasks.register<FetchArchiveTask>("fetchNcnn") {
        url.set("https://github.com/Tencent/ncnn/releases/download/20261020/ncnn-20261020-android.zip")
        sha256.set("62a9edb493ceb42a5c9cbf2d3a79b3f44bff06c625134ffb64d183dcc42c25fc")
        includes.set(ncnnAbis.map { "*/$it/**" })
        outputDir.set(ncnnDist)
    }

val sherpaSources = rootProject.layout.projectDirectory.dir("third_party/sherpa-ncnn/src")

val fetchSherpaNcnn =
    tasks.register<FetchArchiveTask>("fetchSherpaNcnn") {
        url.set("https://github.com/k2-fsa/sherpa-ncnn/archive/refs/tags/v2.1.15.tar.gz")
        sha256.set("f4bb8bdd3cea20049cff97aa90dec1cee048b7fdac9a92464ca64df7efe5f27b")
        includes.set(listOf("*/sherpa-ncnn/csrc/**", "*/LICENSE"))
        outputDir.set(sherpaSources.dir("sherpa-ncnn"))
    }

val fetchKaldiNativeFbank =
    tasks.register<FetchArchiveTask>("fetchKaldiNativeFbank") {
        url.set("https://github.com/csukuangfj/kaldi-native-fbank/archive/refs/tags/v1.21.5.tar.gz")
        sha256.set("499966d4cdeb05060df2b26a0c171f92a6a2a119e7747b69e621e62c7fad0e9c")
        includes.set(listOf("*/kaldi-native-fbank/csrc/**", "*/LICENSE"))
        outputDir.set(sherpaSources.dir("kaldi-native-fbank"))
    }

val fetchKissFft =
    tasks.register<FetchArchiveTask>("fetchKissFft") {
        url.set("https://github.com/mborgerding/kissfft/archive/febd4caeed32e33ad8b2e0bb5ea77542c40f18ec.zip")
        sha256.set("497103e664168ebe39580b757adbe616f6cf85a16572af581ca7bc42d0ab13fd")
        includes.set(listOf("*/*.c", "*/*.h", "*/COPYING"))
        outputDir.set(sherpaSources.dir("kissfft"))
    }

val fetchSherpaSources =
    tasks.register("fetchSherpaSources") {
        dependsOn(fetchSherpaNcnn, fetchKaldiNativeFbank, fetchKissFft)
    }

val fetchSpeechModel =
    tasks.register<FetchArchiveTask>("fetchSpeechModel") {
        url.set(
            "https://github.com/oneloneraven/deaftly/releases/download/speech-model-libri-giga-2023-04-04/speech-model-libri-giga-2023-04-04.zip",
        )
        sha256.set("f2fcb07c3971fd49730ab1d0ab6c5228ef44f962a2dfa8cfa9a93767ee01704a")
        stripFirstDir.set(false)
        prefix.set("speech")
        outputDir.set(layout.buildDirectory.dir("generated/speechModel"))
    }

val releaseKeystore = providers.environmentVariable("ANDROID_KEYSTORE_PATH")

fun signingEnv(name: String): String =
    requireNotNull(providers.environmentVariable(name).orNull) {
        "$name must be set when ANDROID_KEYSTORE_PATH is"
    }

android {
    namespace = "com.oneloneraven.deaftly"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.oneloneraven.deaftly"
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
                    "-DSHERPA_NCNN_SRC=${sherpaSources.dir("sherpa-ncnn").asFile.absolutePath}",
                    "-DKALDI_FBANK_SRC=${sherpaSources.dir("kaldi-native-fbank").asFile.absolutePath}",
                    "-DKISSFFT_SRC=${sherpaSources.dir("kissfft").asFile.absolutePath}",
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
        // English variants are overrides of values/, so most keys are missing from them by design; SpellingTest checks what matters.
        disable += "MissingTranslation"
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
    androidTestImplementation(libs.androidx.test.rules)
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
        variant.sources.assets?.addGeneratedSourceDirectory(fetchSpeechModel, FetchArchiveTask::outputDir)
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
    .configureEach { dependsOn(fetchNcnn, fetchSherpaSources) }
