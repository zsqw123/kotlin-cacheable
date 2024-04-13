package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.types.makeNullable

class NormalTransformer(
    cacheableTransformContext: CacheableTransformContext
) : CacheableFunctionTransformer(cacheableTransformContext) {
    override fun doTransform() {
        // modify origin function, use origin function's symbol.
        val funcBuilder = originFunction.builder()
        val functionThisReceiver = originFunction.dispatchReceiverParameter?.let { funcBuilder.irGet(it) }
        val fieldInitializer = funcBuilder.irGetField(functionThisReceiver, backendField)

        val cachedNowVal = funcBuilder.scope.createTmpVariable(
            irType = functionType.makeNullable(),
            nameHint = "cachedNow",
            initializer = fieldInitializer
        )

        originFunction.body = funcBuilder.irBlockBody {
            val thenInitializeBlock = irBlock {
                val calculatedVal = valInitByOrigin()
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

