@Suppress("ConstPropertyName")
object D {
    const val KT = "1.9.22"

    private const val KSP_VERSION = "1.9.22-1.0.17"
    const val ksp = "com.google.devtools.ksp:symbol-processing-api:$KSP_VERSION"

    private const val KT_COMPILER = "1.9.22"
    const val compilerEmbeddable = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$KT_COMPILER"

    const val autoService = "com.google.auto.service:auto-service-annotations:1.1.1"
    const val autoServiceKsp = "dev.zacsweers.autoservice:auto-service-ksp:1.1.0"
    const val asm = "org.ow2.asm:asm:9.6"

    const val compileTesting = "dev.zacsweers.kctfork:core:0.4.0"

    private const val jupiterVersion = "5.10.2"
    const val jUnitJupiterApi = "org.junit.jupiter:junit-jupiter-api:$jupiterVersion"
    const val jUnitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine:$jupiterVersion"
}
