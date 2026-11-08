package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class TestingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Apply common testing dependencies to all modules
        project.dependencies {
            testImplementation(libs.junit)
            testImplementation(libs.kotlin.test.junit)
            testImplementation(libs.mockk)
            testImplementation(libs.truth)
            testImplementation(libs.robolectric)
            androidTestImplementation(libs.androidx.test.core)
            androidTestImplementation(libs.androidx.test.rules)
            androidTestImplementation(libs.androidx.test.runner)
            androidTestImplementation(libs.androidx.test.ext.junit)
            androidTestImplementation(libs.espresso.core)
            androidTestImplementation(libs.espresso.intents)
            androidTestImplementation(libs.compose.ui.test.junit4)
            androidTestImplementation(libs.compose.ui.test.manifest)
            debugImplementation(libs.androidx.compose.ui.tooling)
            debugImplementation(libs.androidx.compose.ui.test.manifest)
        }

        // Configure test options
        project.configure<com.android.build.api.dsl.BaseExtension> {
            testOptions {
                unitTests {
                    isIncludeAndroidResources = true
                    returnDefaultValues = true
                }
                execution = "ANDROIDX_TEST_ORCHESTRATOR"
            }
        }
    }
}