package zsu.cacheable.kcp

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.symbols.IrSymbol

const val CACHEABLE_FQN = "zsu.cacheable.Cacheable"

fun IrSymbol.builder(irBuiltIns: IrBuiltIns): IrBuilderWithScope {
    return CacheableIRBuilder(irBuiltIns, this)
}
