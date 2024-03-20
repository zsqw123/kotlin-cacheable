plugins {
    kotlin("jvm") version "1.9.22"
    id("insidePublish")
    id("java-gradle-plugin")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    ksp(D.autoServiceKsp)
    implementation(project(":kotlin-cacheable-kcp"))
    implementation(D.autoService)
    implementation(kotlin("gradle-plugin-api"))
    compileOnly(D.compilerEmbeddable)
}

tasks.test {
    useJUnitPlatform()
}
