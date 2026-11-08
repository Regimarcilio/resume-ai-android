package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.tasks

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("io.gitlab.arturbosch.detekt")

        project.configure<io.gitlab.arturbosch.detekt.DetektExtension> {
            toolVersion.set(project.providers.gradleProperty("detekt")!!)
            config.set(project.rootProject.file("config/detekt/detekt.yml"))
            baseline.set(project.rootProject.file("config/detekt/baseline.xml"))
            buildUponDefaultConfig.set(true)
            allRules.set(true)
        }

        project.tasks.withType<io.gitlab.arturbosch.detekt.Detekt> {
            reports {
                html.isEnabled = true
                html.destination = project.file("${project.buildDir}/reports/detekt/detekt.html")
                xml.isEnabled = true
                xml.destination = project.file("${project.buildDir}/reports/detekt/detekt.xml")
                txt.isEnabled = true
                txt.destination = project.file("${project.buildDir}/reports/detekt/detekt.txt")
            }
        }

        project.tasks.named("check") {
            dependsOn("detekt")
        }
    }
}