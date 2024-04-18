package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import zsu.cacheable.kcp.backend.CacheableFunctionTransformer.Creator
import zsu.cacheable.kcp.builder

abstract class CacheableFunctionTransformer(
    private val cacheableTransformContext: CacheableTransformContext,
) {
    // modify origin function through this function
    abstract fun doTransform(): IrBody

    private val cacheableSymbols = cacheableTransformContext.cacheableSymbols
    protected val parentClass = cacheableTransformContext.parentClass
    protected val originFunction = cacheableTransformContext.originFunction
    protected val backendField = cacheableTransformContext.backendField
    private val copiedFunction = cacheableTransformContext.copiedFunction
    private val createdFlagField = cacheableTransformContext.createdFlagField

    protected val irBuiltIns = cacheableSymbols.irBuiltIns

    protected fun IrSymbolOwner.builder() = symbol.builder(irBuiltIns, startOffset, endOffset)

    /** create a val which initialized by call origin function. */
    private fun IrBuilderWithScope.valInitByOrigin() = scope.createTemporaryVariable(
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


    val funcBuilder = originFunction.builder()
    val functionThisReceiver = originFunction.dispatchReceiverParameter?.let {
        funcBuilder.irGet(it)
    }
    val getCachedField = funcBuilder.irGetField(functionThisReceiver, backendField)
    val getIsCreated = funcBuilder.irGetField(functionThisReceiver, createdFlagField)

    protected fun IrStatementsBuilder<*>.computeCache() {
        // val origin = originFunction()
        val calculatedVal = valInitByOrigin()
        +calculatedVal
        // cachedField = origin
        val getResultVal = irGet(calculatedVal)
        +irSetField(functionThisReceiver, backendField, getResultVal)
        // created = true
        +irSetField(functionThisReceiver, createdFlagField, irTrue())
        // return origin
        +irReturn(getResultVal)
    }

    fun interface Creator {
        fun create(context: CacheableTransformContext): CacheableFunctionTransformer
    }
}
