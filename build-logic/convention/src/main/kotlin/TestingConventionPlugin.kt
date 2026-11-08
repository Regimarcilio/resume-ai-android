package com.resumeapp.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.tasks
import org.gradle.api.tasks.testing.Test

class TestingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
                exceptionFormat = "full"
                showStandardStreams = true
            }
            maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
            minHeapSize = "256m"
            maxHeapSize = "1024m"
        }

        project.tasks.named("test", Test::class) {
            systemProperty("kotlinx.coroutines.debug", "off")
            jvmArgs("-Xmx1024m", "-XX:+HeapDumpOnOutOfMemoryError")
        }
    }
}