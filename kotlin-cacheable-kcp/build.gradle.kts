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
    implementation(project(":kotlin-cacheable-runtime"))

    ksp(D.autoServiceKsp)
    implementation(D.autoService)
    compileOnly(D.compilerEmbeddable)

    testImplementation(D.jUnitJupiterApi)
    testRuntimeOnly(D.jUnitJupiterEngine)
    testImplementation(D.compileTesting)
}

tasks.test {
    useJUnitPlatform()
}
