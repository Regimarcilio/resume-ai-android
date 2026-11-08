plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    // Version catalog not available in composite build, use hardcoded versions
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("com.android.tools.build:gradle:8.5.0")
    implementation("org.jetbrains.kotlin:kotlin-parcelize:1.9.22")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.9.22")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.20-1.0.14")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.48")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.4")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:1.1.1")
}