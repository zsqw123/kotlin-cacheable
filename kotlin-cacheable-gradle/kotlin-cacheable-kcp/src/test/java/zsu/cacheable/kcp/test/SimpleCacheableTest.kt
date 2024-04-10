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
        package zsu.test

        import zsu.cacheable.Cacheable
        import zsu.cacheable.CacheMode

        class CacheableTest {
            private var a = 1
        
            @Cacheable(cacheMode = CacheMode.NONE)
            fun foo(): Int {
                return ++a
            }

            private var b = 1
            @Cacheable
            fun bar(): Int {
                synchronized(this) { ++a }
                return ++b
            }
        }

        class Entry {
            fun foo(): Int {
                val testEnvClass = CacheableTest()
                testEnvClass.foo()
                testEnvClass.foo()
                testEnvClass.foo()
                return testEnvClass.foo()
            }

            fun bar(): Int {
                val testEnvClass = CacheableTest()
                testEnvClass.bar()
                testEnvClass.bar()
                testEnvClass.bar()
                return testEnvClass.bar()
            }
        }
    """
        )

        val compilation = KotlinCompilation().apply {
            sources = listOf(kotlinSource)
            compilerPluginRegistrars = listOf(CacheableKCP())
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
        println(compilation.generatedFiles)
        Assertions.assertEquals(KotlinCompilation.ExitCode.OK, compilation.exitCode)

        val entryClass = compilation.classLoader.loadClass("zsu.test.Entry")
        val entry = entryClass.newInstance()

        val fooMethod = entryClass.getMethod("foo")
        val fooResult = fooMethod.invoke(entry) as Int
        val barMethod = entryClass.getMethod("bar")
        val barResult = barMethod.invoke(entry) as Int
        Assertions.assertEquals(2, fooResult)
        Assertions.assertEquals(2, barResult)
    }
}