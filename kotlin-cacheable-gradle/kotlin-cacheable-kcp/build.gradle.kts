plugins {
    kotlin("jvm") version "1.9.22"
    id("insidePublish")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    ksp(D.autoServiceKsp)
    implementation(D.autoService)
    compileOnly(D.compilerEmbeddable)
}

tasks.test {
    useJUnitPlatform()
}
