package zsu.cacheable.kcp.test

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import zsu.cacheable.kcp.CacheableKCP

class SimpleCacheableTest {


    @Test
    @OptIn(ExperimentalCompilerApi::class)
    fun `simple cacheable`() {
        val kotlinSource = SourceFile.kotlin(
            "Foo.kt", """
        import zsu.cacheable.Cacheable

        class CacheableTest {
            private var a = 1
        
            @Cacheable
            fun foo(): Int {
                val a = a + 1
                this.a = a
                return a
            }
        }

        class Foo {
            fun bar() {
                val testEnvClass = CacheableTest() 
            }
        }
    """
        )

        val result = KotlinCompilation().apply {
            sources = listOf(kotlinSource)
            compilerPluginRegistrars = listOf(CacheableKCP())
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
        Assertions.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        println(result.outputDirectory)

    }
}