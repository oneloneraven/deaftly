plugins {
    alias(libs.plugins.android.application)
}

// Spike for #285. Not for merge. Native libraries come from the sherpa-ncnn v2.1.15 Android release,
// unpacked outside the repo; pass their folder with -PsherpaJniLibs=<dir>.
val sherpaJniLibs = providers.gradleProperty("sherpaJniLibs")

android {
    namespace = "com.ravendarque.deaftly.spikestt"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ravendarque.deaftly.spikestt"
        minSdk = 33
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets["main"].jniLibs.directories.add(sherpaJniLibs.getOrElse("jniLibs"))
}

dependencies {
    implementation("com.alphacephei:vosk-android:0.3.75")
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}
