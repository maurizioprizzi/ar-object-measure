pluginManagement {
    repositories {
        google() // Recommended to be first for Android projects
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ARObjectMeasure"
include(":app")

buildCache {
    local {
        isEnabled = true
    }
}
gradle.settingsEvaluated {
    gradle.startParameter.isParallelProjectExecutionEnabled = true
}