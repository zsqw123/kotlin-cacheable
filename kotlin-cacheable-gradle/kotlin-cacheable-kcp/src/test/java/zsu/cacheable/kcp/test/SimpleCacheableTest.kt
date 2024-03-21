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
                val a = a + 1
                this.a = a
                return a
            }
        }

        class Foo {
            fun bar(): Int {
                val testEnvClass = CacheableTest()
                testEnvClass.foo()
                testEnvClass.foo()
                testEnvClass.foo()
                return testEnvClass.foo()
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
        Assertions.assertEquals(KotlinCompilation.ExitCode.OK, compilation.exitCode)

        val fooClass = compilation.classLoader.loadClass("zsu.test.Foo")
        val barMethod = fooClass.getMethod("bar")
        val result = barMethod.invoke(fooClass.newInstance()) as Int
        Assertions.assertEquals(2, result)
    }
}