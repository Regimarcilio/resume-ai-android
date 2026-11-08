plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.android)
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.parcelize)
    implementation(libs.kotlin.serialization)
    implementation(libs.ksp)
    implementation(libs.hilt.gradle.plugin)
    implementation(libs.detekt)
    implementation(libs.ktlint)
}