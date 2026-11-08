package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(alias(libs.plugins.kotlin.android))
        project.plugins.apply(alias(libs.plugins.compose.compiler))
        project.plugins.apply(alias(libs.plugins.kotlin.serialization))

        project.configure<com.android.build.api.dsl.BaseExtension> {
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
            }
            packagingOptions {
                resources {
                    excludes += "/META-INF/*"
                }
            }
        }

        project.dependencies {
            implementation(platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.compose.animation)
            implementation(libs.androidx.compose.runtime)
            implementation(libs.androidx.compose.runtime.livedata)
            implementation(libs.androidx.navigation.compose)
            debugImplementation(libs.androidx.compose.ui.tooling)
            debugImplementation(libs.androidx.compose.ui.test.manifest)
        }
    }
}