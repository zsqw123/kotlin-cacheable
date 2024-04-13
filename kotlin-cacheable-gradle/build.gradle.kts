plugins {
    kotlin("jvm")
//    id("insidePublish")
    id("com.gradle.plugin-publish") version "1.2.1"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
}

tasks.test {
    useJUnitPlatform()
}

val exVersion: String by project

version = exVersion
group = "host.bytedance"

gradlePlugin {
    website = "https://github.com/zsqw123/kotlin-cacheable"
    vcsUrl = "https://github.com/zsqw123/kotlin-cacheable"
    plugins {
        create("kotlinCacheable") {
            id = "host.bytedance.kotlin-cacheable"
            displayName = "Plugin for cache all logic in kotlin through KCP."
            description = "Plugin for cache all logic in kotlin through KCP."
            tags = listOf("kcp", "kotlin")
            implementationClass = "zsu.cacheable.kcp.CacheableGradlePlugin"
        }
    }
}

