@Suppress("ConstPropertyName")
object D {
    const val KT = "1.9.22"

    const val KSP_VERSION = "1.9.22-1.0.17"
    const val ksp = "com.google.devtools.ksp:symbol-processing-api:$KSP_VERSION"
    const val compilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$KT"

    const val autoService = "com.google.auto.service:auto-service-annotations:1.1.1"
    const val autoServiceKsp = "dev.zacsweers.autoservice:auto-service-ksp:1.1.0"
    const val asm = "org.ow2.asm:asm:9.6"

    const val junitBom = "org.junit:junit-bom:5.9.1"
    const val junitJupiter = "org.junit.jupiter:junit-jupiter"
}
