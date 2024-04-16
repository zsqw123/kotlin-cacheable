package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.starProjectedType

class SynchronizedTransformer private constructor(
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
    override fun doTransform(): IrBlockBody = funcBuilder.irBlockBody {
        // if (created) return cachedField
        +irIfThen(irBuiltIns.unitType, getIsCreated, irReturn(getCachedField))
        +synchronizedBlock {
            // if (created) return cachedField
            +irIfThen(irBuiltIns.unitType, getIsCreated, irReturn(getCachedField))
            computeCache()
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

    companion object : Creator {
        override fun create(context: CacheableTransformContext) = SynchronizedTransformer(context)
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
