plugins {
    id("inside") apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.22" apply false
    id("com.github.gmazzo.buildconfig") version "5.3.5" apply false
}

subprojects {
    apply(plugin = "inside")
}
