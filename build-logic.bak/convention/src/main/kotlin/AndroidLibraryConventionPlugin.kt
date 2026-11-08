package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withId("com.android.library") {
            project.plugins.apply("org.jetbrains.kotlin.android")
            project.plugins.apply("org.jetbrains.kotlin.parcelize")
            project.plugins.apply("org.jetbrains.kotlin.serialization")
            project.plugins.apply("com.google.devtools.ksp")
            project.plugins.apply("com.resumeapp.convention.ktlint")
            project.plugins.apply("com.resumeapp.convention.detekt")
            project.plugins.apply("com.resumeapp.convention.testing")

            project.configure<com.android.build.api.dsl.LibraryExtension> {
                namespace = "com.resumeapp.${project.name.replace(":core:", "").replace(":features:", "").replace("/", ".")}"
                compileSdk = project.providers.gradleProperty("compileSdk").map { it.toInt() }!!
                defaultConfig {
                    minSdk = project.providers.gradleProperty("minSdk").map { it.toInt() }!!
                    targetSdk = project.providers.gradleProperty("targetSdk").map { it.toInt() }!!
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                kotlinOptions {
                    jvmTarget = "17"
                }
                buildFeatures {
                    compose = true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion = project.providers.gradleProperty("composeCompiler")!!
                }
            }

            project.dependencies {
                "api"(libs.androidx.core.ktx)
                "api"(libs.androidx.lifecycle.runtime.ktx)
                "api"(libs.androidx.lifecycle.viewmodel.ktx)
                "api"(libs.androidx.lifecycle.viewmodel.compose)
                "api"(libs.kotlinx.coroutines.android)
                "api"(libs.kotlinx.serialization.json)
                "api"(libs.kotlinx.datetime)
            }
        }
    }
}