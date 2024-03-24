package zsu.cacheable.kcp.bytecode

import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.ir.declarations.IrClass

class CacheableClassTransform : ClassGeneratorExtension {
    override fun generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator {
        TODO("Not yet implemented")
    }

    companion object {
        @OptIn(ExperimentalCompilerApi::class)
        fun register(storage: CompilerPluginRegistrar.ExtensionStorage) {
            with(storage) {
                ClassGeneratorExtension.registerExtension(CacheableClassTransform())
            }
        }
    }
}