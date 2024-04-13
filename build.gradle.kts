plugins {
    id("inside") apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

subprojects {
    apply(plugin = "inside")
}
