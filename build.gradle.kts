// Top-level build file - minimal
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
    id("org.jetbrains.kotlin.parcelize") version "1.9.20" apply false
}

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