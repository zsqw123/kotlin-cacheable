plugins {
    kotlin("jvm")
    id("com.gradle.plugin-publish") version "1.2.1"
    id("insidePublish")
    id("com.github.gmazzo.buildconfig")
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

val exGroup: String by project
val exVersion: String by project

version = exVersion
group = exGroup

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

buildConfig {
    val kcpProject = project(":kotlin-cacheable-kcp")
    val runtimeProject = project(":kotlin-cacheable-runtime")
    packageName(exGroup)
    buildConfigField("String", "GROUP", "\"$exGroup\"")
    buildConfigField("String", "KCP_NAME", "\"${kcpProject.name}\"")
    buildConfigField("String", "RUNTIME_NAME", "\"${runtimeProject.name}\"")
    buildConfigField("String", "VERSION", "\"$exVersion\"")
}

