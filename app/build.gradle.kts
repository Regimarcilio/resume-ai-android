import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
    id("com.google.dagger.hilt.android")
}

// Hilt cross-module aggregation: required so the :app module's Dagger graph
// sees @HiltViewModel classes declared in the :features:* library modules.
hilt {
    enableAggregatingTask = true
}

// ---------------------------------------------------------------------------
// Release signing.
//
// The keystore lives outside version control: "keystore.properties" points to
// it and is git-ignored. When the file is absent (CI, fresh clone) the release
// build still works, it just produces an unsigned APK.
// Copy "keystore.properties.template" and fill it in to sign locally.
// ---------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystoreProperties.getProperty("storeFile") != null &&
    rootProject.file(keystoreProperties.getProperty("storeFile")).exists()

android {
    namespace = "com.resumeapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.resumeapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/*"
        }
    }
}

// ---------------------------------------------------------------------------
// Distribution: every generated APK is copied inside the project so it is
// always easy to find next to the sources.
//
//   ./gradlew apk         ->  dist/resume-ai-<version>-debug.apk     (debug)
//   ./gradlew apkRelease  ->  release/resume-ai-<version>-release.apk (producao)
// ---------------------------------------------------------------------------
val distDir = rootProject.layout.projectDirectory.dir("dist")
val releaseDir = rootProject.layout.projectDirectory.dir("release")

val copyDebugApkToDist by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copies the debug APK to <project-root>/dist/"
    dependsOn("assembleDebug")
    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include("*.apk")
    }
    into(distDir)
    rename { "resume-ai-${android.defaultConfig.versionName}-debug.apk" }
    doLast {
        logger.lifecycle("APK (debug) copied to ${distDir.asFile.path}")
    }
}

val copyReleaseApkToReleaseDir by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copies the release (production) APK to <project-root>/release/"
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
        exclude("**/baselineProfiles/**")
    }
    into(releaseDir)
    rename { "resume-ai-${android.defaultConfig.versionName}-release.apk" }
    doLast {
        val unsigned = releaseDir.asFile
            .listFiles { f -> f.name.endsWith(".apk") }
            ?.all { f -> f.name.contains("unsigned") } ?: true
        logger.lifecycle(
            "APK (release) copied to ${releaseDir.asFile.path}" +
                if (unsigned) "  [UNSIGNED: configure keystore.properties to sign]" else ""
        )
    }
}

// AGP registers the variant tasks lazily, so the wiring must be lazy too
// (tasks.named(...) at configuration time fails with "task not found").
tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(copyDebugApkToDist)
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(copyReleaseApkToReleaseDir)
}

dependencies {
    // Core modules
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ai"))

    // Feature modules
    implementation(project(":features:resume"))
    implementation(project(":features:ats"))
    implementation(project(":features:ai-assist"))
    implementation(project(":features:templates"))
    implementation(project(":features:settings"))
    implementation(project(":features:onboarding"))

    // Compose (using BOM)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Hilt - KSP annotation processing
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // javax.inject for @Singleton / @Inject annotations
    implementation("javax.inject:javax.inject:1")

    // AndroidX libs used directly by the app module DI layer
    // (core:data exposes them as `implementation`, so they are NOT transitive)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-core:1.0.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Kotlin Coroutines & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("io.coil-kt:coil-compose:2.5.0")

    // PDF (local generation with pdfbox for offline)
    implementation("org.apache.pdfbox:pdfbox:3.0.2")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.20")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.robolectric:robolectric:4.11")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.1")
}


