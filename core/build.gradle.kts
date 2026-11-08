plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
    `java-test-fixtures`
}

kotlin {
    jvmToolchain(21)
    explicitApi()
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
