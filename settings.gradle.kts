pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://s01.oss.sonatype.org/content/repositories/releases/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "ksu-miuix"
include(":app")

// Disable AAR metadata check for miuix-ui which requires compileSdk 37
gradle.projectsEvaluated {
    it.extra.set("android.experimental.checkAarMetadata", "false")
}
