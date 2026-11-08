// Top-level build file - minimal, delegates to convention plugins
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    id("com.android.tools.build.jetifier") version "1.0.0" apply false
}

import org.gradle.api.GradleException

allprojects {
    group = "com.resumeapp"
    version = "1.0.0"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Version bump task for release
tasks.register("bumpVersion") {
    group = "release"
    description = "Bumps versionCode and versionName in app module"
    doLast {
        val versionName = project.findProperty("versionName") as String? ?: "1.0.0"
        val versionCode = (project.findProperty("versionCode") as String? ?: System.currentTimeMillis().toString()).toLong()
        
        val appBuildFile = file("app/build.gradle.kts")
        val content = appBuildFile.readText()
        
        val newContent = content
            .replace(Regex("versionName\\s*=\\s*\"[^\"]+\""), "versionName = \"$versionName\"")
            .replace(Regex("versionCode\\s*=\\s*\\d+"), "versionCode = $versionCode")
        
        appBuildFile.writeText(newContent)
        println("Updated version: $versionName ($versionCode)")
    }
}

// Kalimaster SAST task
tasks.register("kalimasterSast") {
    group = "verification"
    description = "Runs Kalimaster SAST security scan"
    doLast {
        val projectRoot = project.rootDir.absolutePath
        val outputFile = file("kalimaster-sast-report.json").absolutePath
        
        val classpath = project.configurations.runtimeClasspath.get().map { it.absolutePath }.joinToString(":")
        val mainClass = "com.resumeapp.sast.KalimasterSast"
        
        val process = java.lang.ProcessBuilder("java", "-cp", classpath, mainClass, projectRoot, outputFile)
            .directory(project.rootDir)
            .redirectErrorStream(true)
            .start()
        
        process.inputStream.bufferedReader().forEachLine { println(it) }
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            throw GradleException("Kalimaster SAST found blocking security issues. Report: kalimaster-sast-report.json")
        }
        
        println("✅ Kalimaster SAST completed successfully")
    }
}