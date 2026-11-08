package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.tasks

class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.jlleitschuh.gradle.ktlint")

        project.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(project.providers.gradleProperty("ktlint")!!)
            android.set(true)
            outputToConsole.set(true)
            ignoreFailures.set(false)
        }

        project.tasks.withType<org.jlleitschuh.gradle.ktlint.KotlinLint> {
            reports {
                html.isEnabled = true
                html.destination = project.file("${project.buildDir}/reports/ktlint/ktlint.html")
            }
        }

        project.tasks.named("check") {
            dependsOn("ktlintCheck")
        }
    }
}