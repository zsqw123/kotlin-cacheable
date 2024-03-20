plugins {
    kotlin("jvm") version "1.9.22"
    id("insidePublish")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {

}

tasks.test {
    useJUnitPlatform()
}
