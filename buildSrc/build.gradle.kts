plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}

// Gradle builds buildSrc with jar only, so its tests are tied to it to run on every fresh build.
tasks.named("jar") {
    finalizedBy(tasks.named("test"))
}
