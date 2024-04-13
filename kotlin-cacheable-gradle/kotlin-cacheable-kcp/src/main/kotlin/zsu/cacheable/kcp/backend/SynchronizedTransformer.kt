package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.makeNullable

class SynchronizedTransformer(
    cacheableTransformContext: CacheableTransformContext,
) : CacheableFunctionTransformer(cacheableTransformContext) {
    override fun doTransform() {
        // modify origin function, use origin function's symbol.
        val funcBuilder = originFunction.builder()
        val functionThisReceiver = funcBuilder.irGet(originFunction.dispatchReceiverParameter!!)

        val newBody = funcBuilder.irBlockBody {
            checkNullAndInitial(
                functionThisReceiver, synchronizedInitial(functionThisReceiver),
            )
        }
        originFunction.body = newBody
    }

    private fun IrBuilderWithScope.createCachedNowVal(
        thisReceiverExpression: IrExpression,
    ): IrVariable = scope.createTmpVariable(
        irType = functionType.makeNullable(),
        nameHint = "cachedNow",
        initializer = irGetField(thisReceiverExpression, backendField)
    )

    private fun IrBuilderWithScope.synchronizedInitial(
        functionThisReceiver: IrGetValue,
    ): IrExpression = irBlock {
        val lock = createTmpVariable(functionThisReceiver, "lock")
        // monitorEnter
        +irCall(
            cacheableSymbols.monitorEnter, irBuiltIns.unitType,
            valueArgumentsCount = 1, typeArgumentsCount = 0,
        ).apply {
            putValueArgument(0, irGet(lock))
        }

        // re-check null and normal initial
        val cachedNowVal = createCachedNowVal(functionThisReceiver)
        // get current cached first
        +cachedNowVal

        // returns cache if not null
        val rejudgeAndInitialize = irIfNull(
            functionType,
            irGet(cachedNowVal),
            normalInitial(functionThisReceiver),
            irReturn(irGet(cachedNowVal)),
        )

        // monitorExit
        val monitorExit = irCall(
            cacheableSymbols.monitorExit, irBuiltIns.unitType,
            valueArgumentsCount = 1, typeArgumentsCount = 0,
        ).apply {
            putValueArgument(0, irGet(lock))
        }
        +irTry(irBuiltIns.unitType, rejudgeAndInitialize, emptyList(), monitorExit)
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
        val calculatedVal = valInitByOrigin()
        +calculatedVal
        val getResultVal = irGet(calculatedVal)
        +irSetField(functionThisReceiver, backendField, getResultVal)
        +irReturn(getResultVal)
    }
}
