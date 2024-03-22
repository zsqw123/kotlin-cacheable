package zsu.cacheable.kcp

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.makeNullable

class SynchronizedTransformer(
    cacheableSymbols: CacheableSymbols,
    originFunction: IrSimpleFunction,
    backendField: IrField,
    copiedFunction: IrSimpleFunction,
) : CacheableFunctionTransformer(
    cacheableSymbols, originFunction, backendField, copiedFunction,
) {
    override fun doTransform() {
        // modify origin function, use origin function's symbol.
        val funcBuilder = originFunction.builder()
        val functionThisReceiver = funcBuilder.irGet(originFunction.dispatchReceiverParameter!!)

        originFunction.body = funcBuilder.irBlockBody {
            checkNullAndInitial(
                functionThisReceiver, synchronizedInitial(originFunction, functionThisReceiver),
            )
        }
    }

    private fun IrBuilderWithScope.createCachedNowVal(
        thisReceiverExpression: IrExpression,
    ): IrVariable = scope.createTmpVariable(
        irType = originFunction.returnType.makeNullable(),
        nameHint = "cachedNow",
        initializer = irGetField(thisReceiverExpression, backendField)
    )

    private fun IrBuilderWithScope.synchronizedInitial(
        callFrom: IrFunction,
        functionThisReceiver: IrGetValue,
    ) = irBlock {
        +irCall(synchronizedFunction).apply {
            // put lock
            putValueArgument(0, functionThisReceiver) // synchronized(this)
            // re-check null and normal initial
            putValueArgument(1, irBlock { // synchronized(this) {}
                checkNullAndInitial(
                    functionThisReceiver, normalInitial(functionThisReceiver)
                )
            })
        }
    }

    private fun IrStatementsBuilder<*>.checkNullAndInitial(
        functionThisReceiver: IrGetValue, initialBlock: IrExpression,
    ) {
        val cachedNowVal = createCachedNowVal(functionThisReceiver)
        // get current cached first
        +cachedNowVal
        // returns cache if not null
        +irIfNull(
            irBuiltIns.unitType,
            irGet(cachedNowVal),
            initialBlock,
            irReturn(irGet(cachedNowVal)),
        )
    }

    private fun IrBuilderWithScope.normalInitial(
        functionThisReceiver: IrGetValue,
    ) = irBlock {
        val calculatedVal = valInitByOrigin(originFunction, copiedFunction)
        +calculatedVal
        val getResultVal = irGet(calculatedVal)
        +irSetField(functionThisReceiver, backendField, getResultVal)
        +irReturn(getResultVal)
    }

    private val synchronizedFunction = cacheableSymbols.synchronizedFun
}
