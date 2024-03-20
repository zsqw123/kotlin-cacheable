plugins {
    kotlin("jvm")
    id("insidePublish")
}

dependencies {

}

tasks.test {
    useJUnitPlatform()
}
