package zsu.cacheable.kcp

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.symbols.IrSymbol

class CacheableIRBuilder(
    irBuiltIns: IrBuiltIns, symbol: IrSymbol
) : IrBuilderWithScope(
    IrGeneratorContextBase(irBuiltIns), Scope(symbol), UNDEFINED_OFFSET, UNDEFINED_OFFSET,
)