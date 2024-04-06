package zsu.cacheable.kcp

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger
import zsu.cacheable.CacheMode
import zsu.cacheable.Cacheable
import zsu.cacheable.kcp.backend.CacheableIRBuilder

const val CACHEABLE_FQN = "zsu.cacheable.Cacheable"

fun IrSymbol.builder(irBuiltIns: IrBuiltIns): IrBuilderWithScope {
    return CacheableIRBuilder(irBuiltIns, this)
}

class CacheableTransformError(message: String) : IllegalArgumentException(message)

private val defaultCacheable = Cacheable()

internal fun IrConstructorCall.readCacheable(): Cacheable {
    val arg = valueArguments.firstOrNull() ?: return defaultCacheable
    arg as IrGetEnumValue
    val mode = CacheMode.valueOf(arg.symbol.owner.name.asString())
    return Cacheable(mode)
}

/**
 * copied from [org.jetbrains.kotlin.ir.backend.js.resolverLogger]
 * but origin one is very looks like a js ability, so I copied that out.
 */
val CompilerConfiguration.logger: Logger
    get() = when (val messageLogger = this[IrMessageLogger.IR_MESSAGE_LOGGER]) {
        null -> DummyLogger
        else -> object : Logger {
            override fun log(message: String) = messageLogger.report(IrMessageLogger.Severity.INFO, message, null)
            override fun error(message: String) = messageLogger.report(IrMessageLogger.Severity.ERROR, message, null)
            override fun warning(message: String) =
                messageLogger.report(IrMessageLogger.Severity.WARNING, message, null)

            override fun fatal(message: String): Nothing {
                messageLogger.report(IrMessageLogger.Severity.ERROR, message, null)
                throw CompilationErrorException()
            }
        }
    }
