plugins {
    kotlin("jvm") version "2.0.0"
    `kotlin-dsl`
    id("java-gradle-plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("inside") {
            id = "inside"
            implementationClass = "stub.inside.InsidePlugin"
        }
    }
}
