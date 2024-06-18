package zsu.cacheable.kcp

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import zsu.cacheable.kcp.backend.CacheableIrGenerationExtension

/**
 * there are 3 ways to generate codes.
 * 1. Generate jvm class bytecode through [ClassGeneratorExtension], this is the easiest way,
 *  and jvm bytecode is very stable.
 * 2. Generate correspond FIR through [FirTransformer], not stable and seems it didn't have any
 *  extension points
 * 3. Generate backend IR through [IrGenerationExtension], I have tried, and seems it has some
 *  issues when calling inline function.
 *
 * Overall, although using IR to generates something can make plugin comes to every platform, but
 * some lock features may need some atomic library to create synchronized safely. So now we
 * support only JVM.
 */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class CacheableKCP : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        CacheableIrGenerationExtension.register(this)
    }
}


