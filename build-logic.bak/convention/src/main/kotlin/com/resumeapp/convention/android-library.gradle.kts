package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(alias(libs.plugins.android.library))
        project.plugins.apply(alias(libs.plugins.kotlin.android))
        project.plugins.apply(alias(libs.plugins.kotlin.parcelize))
        project.plugins.apply(alias(libs.plugins.kotlin.serialization))
        project.plugins.apply(alias(libs.plugins.ksp))
        project.plugins.apply(alias(libs.plugins.hilt))
        project.plugins.apply(alias(libs.plugins.compose.compiler))
        project.plugins.apply(alias(libs.plugins.ktlint))
        project.plugins.apply(alias(libs.plugins.detekt))

        project.configure<com.android.build.api.dsl.LibraryExtension> {
            namespace = "com.resumeapp.${project.name.replace(":", ".")}"
            compileSdk = libs.versions.compileSdk.get().toInt()
            defaultConfig {
                minSdk = libs.versions.minSdk.get().toInt()
                targetSdk = libs.versions.targetSdk.get().toInt()
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
                getByName("debug") {
                    isMinifyEnabled = false
                    isShrinkResources = false
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
                kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
            }
            packagingOptions {
                resources {
                    excludes += "/META-INF/*"
                }
            }
        }

        project.dependencies {
            // Core Android & Kotlin
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.lifecycle.livedata.ktx)
            implementation(libs.androidx.activity.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.fragment.ktx)

            // Compose (using BOM)
            implementation(platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.compose.animation)
            implementation(libs.androidx.compose.runtime)
            implementation(libs.androidx.navigation.compose)

            // Hilt
            implementation(libs.hilt.android)
            ksp(libs.hilt.compiler)

            // Kotlin
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Testing
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
    }
}