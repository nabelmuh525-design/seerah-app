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
    }
}

rootProject.name = "Seerah"

include(
    ":app",
    ":core",
    ":database",
    ":domain",
    ":data",
    ":feature-home",
    ":feature-timeline",
    ":feature-atlas",
    ":feature-battles",
    ":feature-people",
    ":feature-places",
    ":feature-library",
    ":feature-search",
    ":feature-quran",
    ":feature-hadith",
    ":feature-settings"
)
