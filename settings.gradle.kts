pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
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