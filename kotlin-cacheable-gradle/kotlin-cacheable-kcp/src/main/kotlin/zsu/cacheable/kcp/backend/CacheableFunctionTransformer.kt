package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrExpression
import zsu.cacheable.kcp.builder

abstract class CacheableFunctionTransformer(
    cacheableTransformContext: CacheableTransformContext,
) {
    // modify origin function through this function
    abstract fun doTransform()

    protected val cacheableSymbols = cacheableTransformContext.cacheableSymbols
    protected val parentClass = cacheableTransformContext.parentClass
    protected val originFunction = cacheableTransformContext.originFunction
    protected val backendField = cacheableTransformContext.backendField
    protected val copiedFunction = cacheableTransformContext.copiedFunction
    protected val createdFlagField = cacheableTransformContext.createdFlagField

    protected val functionType = cacheableTransformContext.functionType

    protected val irBuiltIns = cacheableSymbols.irBuiltIns

    protected fun IrSymbolOwner.builder() = symbol.builder(irBuiltIns)

    /** create a val which initialized by call origin function. */
    protected fun IrBuilderWithScope.valInitByOrigin() = scope.createTemporaryVariable(
        callCopiedFunction(),
        nameHint = "origin",
    )

    private fun IrBuilderWithScope.callCopiedFunction() = irCall(
        copiedFunction.symbol, copiedFunction.returnType
    ).apply {
        extensionReceiver = originFunction.extensionReceiverParameter?.let { irGet(it) }
        dispatchReceiver = originFunction.dispatchReceiverParameter?.let { irGet(it) }
        for ((index, irValueParameter) in originFunction.valueParameters.withIndex()) {
            putValueArgument(index, irGet(irValueParameter))
        }
    }

    protected fun IrBuilderWithScope.getIsCreated(
        receiver: IrExpression?,
    ) = irGetField(receiver, createdFlagField)
}