package zsu.cacheable.kcp.test

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import zsu.cacheable.kcp.CacheableKCP

@ExperimentalCompilerApi
class FakeOverrideTest {
    @Test
    fun `fake override cacheable`() {
        val kotlinSource = SourceFile.kotlin(
            "FakeOverride.kt", """
        package zsu.test.fake

        import zsu.cacheable.Cacheable

        interface FakeInterface {
            val v: List<String>
        }

        abstract class AbsOverride : FakeInterface {
            override val v: List<String> @Cacheable get() = emptyList()
        }

        class Child: AbsOverride(), FakeInterface
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
    }
}