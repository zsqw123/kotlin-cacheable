plugins {
    kotlin("jvm") version "1.9.22"
    id("insidePublish")
    id("java-gradle-plugin")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
//    implementation(project(":kotlin-cacheable-kcp"))
    implementation(kotlin("gradle-plugin-api"))
}

tasks.test {
    useJUnitPlatform()
}
