package zsu.cacheable.kcp.bytecode

import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrClass

class CacheableClassTransform(
    private val compilerConfiguration: CompilerConfiguration
) : ClassGeneratorExtension {
    override fun generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator {
        return CacheableClassGenerator(compilerConfiguration, generator, declaration)
    }

    companion object {
        @OptIn(ExperimentalCompilerApi::class)
        fun register(
            storage: CompilerPluginRegistrar.ExtensionStorage,
            compilerConfiguration: CompilerConfiguration
        ) = with(storage) {
            val transform = CacheableClassTransform(compilerConfiguration)
            ClassGeneratorExtension.registerExtension(transform)
        }
    }
}