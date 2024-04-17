package zsu.cacheable.kcp.test

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import zsu.cacheable.kcp.CacheableKCP

@OptIn(ExperimentalCompilerApi::class)
class SimpleCacheableTest {
    @Test
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

    @Test
    fun `track args cacheable`() {
        val kotlinSource = SourceFile.kotlin(
            "Foo.kt", """
        package zsu.test

        import zsu.cacheable.Cacheable
        import zsu.cacheable.CacheMode

        class CacheableTest {
            private var a = 1
        
            @Cacheable(cacheMode = CacheMode.TRACK_ARGS)
            fun foo(param0: Int, param1: Boolean): Int {
                return ++a
            }

            private var b = 1
            @Cacheable(cacheMode = CacheMode.TRACK_ARGS_SYNCHRONIZED)
            fun bar(param0: String, param1: Int): Int {
                return ++b
            }
        }

        class Entry {
            fun foo(): Int {
                val testEnvClass = CacheableTest()
                testEnvClass.foo(0, false) // 2
                testEnvClass.foo(0, false) // 2
                testEnvClass.foo(1, false) // 3
                testEnvClass.foo(1, false) // 3
                testEnvClass.foo(2, true) // 4
                return testEnvClass.foo(2, false) // 5
            }

            fun bar(): Int {
                val testEnvClass = CacheableTest()
                testEnvClass.bar("a", 0) // 2
                testEnvClass.bar("a", 0) // 2
                testEnvClass.bar("b", 1) // 3
                testEnvClass.bar("b", 1) // 3
                testEnvClass.bar("c", 2) // 4
                return testEnvClass.bar("c", 2) // 4
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
        Assertions.assertEquals(5, fooResult)
        Assertions.assertEquals(4, barResult)
    }
}