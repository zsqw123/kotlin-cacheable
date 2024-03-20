plugins {
    kotlin("jvm")
    id("insidePublish")
    id("com.google.devtools.ksp")
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
