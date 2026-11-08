plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

group = "com.resumeapp.domain"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // Kotlin stdlib
    implementation(libs.kotlin.stdlib)
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
}

// Ktlint configuration
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(libs.versions.ktlint.get())
    reporters {
        create("plain") {
            reporterType.set("plain")
        }
        create("checkstyle") {
            reporterType.set("checkstyle")
        }
    }
}
tasks.named("ktlintCheck") {
    group = "verification"
    description = "Check Kotlin code style with ktlint"
}
tasks.named("ktlintFormat") {
    group = "formatting"
    description = "Format Kotlin code with ktlint"
}
tasks.named("check") {
    dependsOn("ktlintCheck")
}

// Detekt configuration
configure<io.gitlab.arturbosch.detekt.DetektExtension> {
    toolVersion = libs.versions.detekt.get()
    config = files("config/detekt/detekt.yml")
    baseline = file("config/detekt/baseline.xml")
    buildUponDefaultConfig = true
    reports {
        xml.required.set(true)
        html.required.set(true)
        txt.required.set(false)
        sarif.required.set(true)
    }
}
tasks.named("detekt") {
    group = "verification"
    description = "Run static analysis with detekt"
}
tasks.named("check") {
    dependsOn("detekt")
}