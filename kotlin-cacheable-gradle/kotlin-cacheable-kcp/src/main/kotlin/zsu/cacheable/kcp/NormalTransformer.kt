package zsu.cacheable.kcp

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.makeNullable

class NormalTransformer(
    irBuiltIns: CacheableSymbols,
    originFunction: IrSimpleFunction,
    backendField: IrField,
    copiedFunction: IrSimpleFunction,
) : CacheableFunctionTransformer(
    irBuiltIns, originFunction, backendField, copiedFunction,
) {
    override fun doTransform() {
        // modify origin function, use origin function's symbol.
        val funcBuilder = originFunction.builder()
        val functionThisReceiver = originFunction.dispatchReceiverParameter?.let { funcBuilder.irGet(it) }
        val fieldInitializer = funcBuilder.irGetField(functionThisReceiver, backendField)

        val cachedNowVal = funcBuilder.scope.createTmpVariable(
            irType = originFunction.returnType.makeNullable(),
            nameHint = "cachedNow",
            initializer = fieldInitializer
        )

        originFunction.body = funcBuilder.irBlockBody {
            val thenInitializeBlock = irBlock {
                val calculatedVal = valInitByOrigin(originFunction, copiedFunction)
                +calculatedVal
                val getResultVal = irGet(calculatedVal)
                +irSetField(functionThisReceiver, backendField, getResultVal)
                +irReturn(getResultVal)
            }
            // get current cached first
            +cachedNowVal
            // returns cache if not null
            +irIfNull(
                irBuiltIns.unitType,
                irGet(cachedNowVal),
                thenInitializeBlock,
                irReturn(irGet(cachedNowVal)),
            )
        }
    }
}

