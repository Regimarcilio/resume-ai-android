pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    // Use convention plugins from build-logic
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.resumeapp.convention") {
                useModule("com.resumeapp.convention:build-logic:1.0.0")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Para SQLCipher se necessário
    }
    // Define version catalog ONCE at root level for all projects
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "resume-ai-android"

// Core modules
include("core:common")
include("core:data")
include("core:domain")
include("core:ai")

// Feature modules
include("features:resume")
include("features:ats")
include("features:ai-assist")
include("features:templates")
include("features:settings")
include("features:onboarding")

// App module
include("app")

// Build logic (convention plugins)
include("build-logic:convention")

// Security tools
include("kalimaster-sast")