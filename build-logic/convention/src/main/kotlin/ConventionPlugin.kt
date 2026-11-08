package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class ConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val projectName = project.name

        when {
            projectName == "app" -> project.plugins.apply("com.resumeapp.convention.android-application")
            projectName.startsWith("core:") -> project.plugins.apply("com.resumeapp.convention.android-library")
            projectName.startsWith("features:") -> project.plugins.apply("com.resumeapp.convention.android-library")
            else -> {}
        }
    }
}