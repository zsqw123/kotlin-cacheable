package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

class CacheableTransformContext(
    val cacheableSymbols: CacheableSymbols,
    val originFunction: IrSimpleFunction,
    val backendField: IrField,
    val copiedFunction: IrSimpleFunction,
    val createdFlagField: IrField,
) {
    val functionType = originFunction.returnType
}
