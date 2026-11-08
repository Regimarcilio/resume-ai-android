package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(alias(libs.plugins.detekt))

        project.configure<io.gitlab.arturbosch.detekt.DetektExtension> {
            toolVersion = libs.versions.detekt.get()
            config = project.files("config/detekt/detekt.yml")
            baseline = project.file("config/detekt/baseline.xml")
            buildUponDefaultConfig = true
            reports {
                xml.required.set(true)
                html.required.set(true)
                txt.required.set(false)
                sarif.required.set(true)
            }
        }

        project.tasks.named("detekt") {
            group = "verification"
            description = "Run static analysis with detekt"
        }

        project.tasks.named("check") {
            dependsOn("detekt")
        }

        // Generate default config if not exists
        project.tasks.register("detektGenerateConfig") {
            group = "configuration"
            description = "Generates default detekt config"
            doLast {
                val configDir = project.file("config/detekt")
                configDir.mkdirs()
                val defaultConfig = project.resources.getResourceAsStream("default-detekt-config.yml")?.readText() ?: ""
                configDir.resolve("detekt.yml").writeText(defaultConfig)
                println("Generated detekt config at ${configDir}/detekt.yml")
            }
        }
    }
}