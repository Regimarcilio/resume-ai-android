package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.dependencies {
            "implementation"(platform(libs.androidx.compose.bom))
            "implementation"(libs.androidx.compose.ui)
            "implementation"(libs.androidx.compose.ui.graphics)
            "implementation"(libs.androidx.compose.ui.tooling.preview)
            "implementation"(libs.androidx.compose.material3)
            "implementation"(libs.androidx.compose.foundation)
            "implementation"(libs.androidx.compose.animation)
            "implementation"(libs.androidx.compose.runtime)
            "implementation"(libs.androidx.navigation.compose)
            "implementation"(libs.hilt.navigation.compose)
            "implementation"(libs.kotlinx.coroutines.core)
            "implementation"(libs.kotlinx.coroutines.android)
            "implementation"(libs.kotlinx.serialization.core)
            "implementation"(libs.kotlinx.serialization.json)
            "implementation"(libs.kotlinx.datetime)
            "implementation"(libs.okhttp)
            "implementation"(libs.okhttp.logging)
            "implementation"(libs.retrofit)
            "implementation"(libs.retrofit.moshi)
            "implementation"(libs.moshi)
            "implementation"(libs.moshi.kotlin)
            "implementation"(libs.coil)

            "testImplementation"(libs.junit)
            "testImplementation"(libs.kotlin.test.junit)
            "testImplementation"(libs.mockk)
            "testImplementation"(libs.truth)
            "testImplementation"(libs.robolectric)
            "androidTestImplementation"(libs.androidx.test.core)
            "androidTestImplementation"(libs.androidx.test.rules)
            "androidTestImplementation"(libs.androidx.test.runner)
            "androidTestImplementation"(libs.androidx.test.ext.junit)
            "androidTestImplementation"(libs.espresso.core)
            "androidTestImplementation"(libs.espresso.intents)
            "androidTestImplementation"(libs.compose.ui.test.junit4)
            "androidTestImplementation"(libs.compose.ui.test.manifest)
            "debugImplementation"(libs.androidx.compose.ui.tooling)
            "debugImplementation"(libs.androidx.compose.ui.test.manifest)
        }
    }
}