package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(alias(libs.plugins.ktlint))

        project.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(libs.versions.ktlint.get())
            android = true
            reporters {
                create("plain") {
                    reporterType.set("plain")
                }
                create("checkstyle") {
                    reporterType.set("checkstyle")
                }
            }
        }

        project.tasks.named("ktlintCheck") {
            group = "verification"
            description = "Check Kotlin code style with ktlint"
        }
        project.tasks.named("ktlintFormat") {
            group = "formatting"
            description = "Format Kotlin code with ktlint"
        }

        project.tasks.named("check") {
            dependsOn("ktlintCheck")
        }
    }
}