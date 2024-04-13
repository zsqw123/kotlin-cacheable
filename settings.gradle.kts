pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kotlin-cacheable"
includeBuild("insidePlugin")
include("kotlin-cacheable-gradle")
include(":kotlin-cacheable-kcp")

include(":kotlin-cacheable-runtime")
include(":demo")
