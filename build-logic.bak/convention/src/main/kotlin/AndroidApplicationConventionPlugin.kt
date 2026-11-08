package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("com.android.application")
        project.plugins.apply("org.jetbrains.kotlin.android")
        project.plugins.apply("org.jetbrains.kotlin.parcelize")
        project.plugins.apply("org.jetbrains.kotlin.serialization")
        project.plugins.apply("com.google.devtools.ksp")
        project.plugins.apply("com.google.dagger.hilt.android")
        project.plugins.apply("com.resumeapp.convention.compose")
        project.plugins.apply("com.resumeapp.convention.ktlint")
        project.plugins.apply("com.resumeapp.convention.detekt")
        project.plugins.apply("com.resumeapp.convention.testing")

        project.configure<com.android.build.api.dsl.ApplicationExtension> {
            namespace = "com.resumeapp"
            compileSdk = project.providers.gradleProperty("compileSdk").map { it.toInt() }!!
            defaultConfig {
                applicationId = "com.resumeapp"
                minSdk = project.providers.gradleProperty("minSdk").map { it.toInt() }!!
                targetSdk = project.providers.gradleProperty("targetSdk").map { it.toInt() }!!
                versionCode = 1
                versionName = "1.0.0"
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables {
                    useSupportLibrary = true
                }
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
            packagingOptions {
                resources {
                    excludes += "/META-INF/*"
                }
            }
        }
    }
}