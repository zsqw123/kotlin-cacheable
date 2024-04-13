package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.starProjectedType

class SynchronizedTransformer(
    cacheableTransformContext: CacheableTransformContext,
) : CacheableFunctionTransformer(cacheableTransformContext) {
    /**
     * ```kotlin
     * if (created) return cachedField
     * synchronized(this) {
     *   if (created) return cachedField
     *   val origin = originFunction()
     *   cachedField = origin
     *   created = true
     *   return origin
     * }
     * ```
     */
    override fun doTransform() {
        // modify origin function, use origin function's symbol.
        val funcBuilder = originFunction.builder()
        val functionThisReceiver = funcBuilder.irGet(originFunction.dispatchReceiverParameter!!)

        val getCachedField = funcBuilder.irGetField(functionThisReceiver, backendField)
        val getIsCreated = funcBuilder.getIsCreated(functionThisReceiver)

        originFunction.body = funcBuilder.irBlockBody {
            // if (created) return cachedField
            +irIfThen(irBuiltIns.unitType, getIsCreated, irReturn(getCachedField))
            // synchronized(this) {
            +synchronizedBlock {
                // if (created) return cachedField
                irIfThen(irBuiltIns.unitType, getIsCreated, irReturn(getCachedField))
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
        }
    }

    private fun IrBuilderWithScope.lockExpression(): IrExpression {
        val originThisReceiver = originFunction.dispatchReceiverParameter
        if (originThisReceiver != null) return irGet(originThisReceiver)
        return javaClassReference(
            cacheableSymbols, parentClass.symbol.starProjectedType,
        )
    }

    /**
     * ```
     * {
     *   monitorEnter()
     *   try {
     *     tryBlock()
     *   } finally {
     *     monitorExit()
     *   }
     * }
     * ```
     */
    private fun IrBuilderWithScope.synchronizedBlock(
        tryBlock: IrBlockBuilder.() -> Unit,
    ) = irBlock {
        // monitorEnter
        +irCall(
            cacheableSymbols.monitorEnter, irBuiltIns.unitType,
            valueArgumentsCount = 1, typeArgumentsCount = 0,
        ).apply {
            putValueArgument(0, lockExpression())
        }


        // monitorExit
        val monitorExit = irCall(
            cacheableSymbols.monitorExit, irBuiltIns.unitType,
            valueArgumentsCount = 1, typeArgumentsCount = 0,
        ).apply {
            putValueArgument(0, lockExpression())
        }
        +irTry(
            irBuiltIns.unitType,
            irBlock { tryBlock() },
            emptyList(), monitorExit
        )
    }
}

private fun IrBuilderWithScope.kClassReference(
    cacheableSymbols: CacheableSymbols, classType: IrType
): IrClassReference = IrClassReferenceImpl(
    startOffset, endOffset,
    cacheableSymbols.kotlinClassType,
    context.irBuiltIns.kClassClass,
    classType
)

private fun IrBuilderWithScope.kClassToJavaClass(
    cacheableSymbols: CacheableSymbols, kClassReference: IrExpression
): IrCall = irGet(
    cacheableSymbols.javaLangClassType, null,
    cacheableSymbols.kClassJavaGetterSymbol
).apply {
    extensionReceiver = kClassReference
}

private fun IrBuilderWithScope.javaClassReference(
    cacheableSymbols: CacheableSymbols, classType: IrType
): IrCall = kClassToJavaClass(
    cacheableSymbols, kClassReference(cacheableSymbols, classType)
)
