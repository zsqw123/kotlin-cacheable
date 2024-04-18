package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.starProjectedType

/**
 * ```
 * monitorEnter(this)
 * try {
 *   tryBlock()
 * } finally {
 *   monitorExit(this)
 * }
 * ```
 */
fun IrBlockBodyBuilder.synchronizedBlock(
    context: CacheableTransformContext,
    tryBlock: IrBlockBuilder.() -> Unit,
) {
    val cacheableSymbols = context.cacheableSymbols
    val irBuiltIns = cacheableSymbols.irBuiltIns
    // monitorEnter
    +irCall(
        cacheableSymbols.monitorEnter, irBuiltIns.unitType,
        valueArgumentsCount = 1, typeArgumentsCount = 0,
    ).apply {
        putValueArgument(0, lockExpression(context))
    }

    // monitorExit
    val monitorExit = irCall(
        cacheableSymbols.monitorExit, irBuiltIns.unitType,
        valueArgumentsCount = 1, typeArgumentsCount = 0,
    ).apply {
        putValueArgument(0, lockExpression(context))
    }

    +irTry(
        irBuiltIns.unitType,
        irBlock { tryBlock() },
        emptyList(),
        irBlock { +monitorExit },
    )
}

private fun IrBuilderWithScope.lockExpression(
    context: CacheableTransformContext
): IrExpression {
    val originFunction = context.originFunction
    val cacheableSymbols = context.cacheableSymbols
    val parentClass = context.parentClass
    val originThisReceiver = originFunction.dispatchReceiverParameter
    if (originThisReceiver != null) return irGet(originThisReceiver)
    return javaClassReference(
        cacheableSymbols, parentClass.symbol.starProjectedType,
    )
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

