package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.IrBlockBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import zsu.cacheable.kcp.builder

abstract class CacheableFunctionTransformer(
    protected val cacheableSymbols: CacheableSymbols,
    protected val originFunction: IrSimpleFunction,
    protected val backendField: IrField,
    protected val copiedFunction: IrSimpleFunction,
) {
    abstract fun doTransform()

    protected val irBuiltIns = cacheableSymbols.irBuiltIns

    protected fun IrSymbolOwner.builder() = symbol.builder(irBuiltIns)

    /** create a val which initialized by call origin function. */
    protected fun IrBlockBuilder.valInitByOrigin(
        callFrom: IrSimpleFunction,
        callee: IrSimpleFunction,
    ) = scope.createTemporaryVariable(
        callCopiedFunction(callFrom, callee),
        nameHint = "origin",
    )

    private fun IrBuilderWithScope.callCopiedFunction(
        callFrom: IrSimpleFunction,
        callee: IrSimpleFunction,
    ) = irCall(callee.symbol, callee.returnType).apply {
        extensionReceiver = callFrom.extensionReceiverParameter?.let { irGet(it) }
        dispatchReceiver = callFrom.dispatchReceiverParameter?.let { irGet(it) }
        for ((index, irValueParameter) in callFrom.valueParameters.withIndex()) {
            putValueArgument(index, irGet(irValueParameter))
        }
    }
}