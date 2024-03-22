package zsu.cacheable.kcp

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import zsu.cacheable.CacheMode
import zsu.cacheable.Cacheable

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
