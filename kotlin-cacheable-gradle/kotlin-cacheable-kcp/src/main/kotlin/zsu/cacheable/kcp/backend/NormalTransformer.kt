package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.*

class NormalTransformer(
    context: CacheableTransformContext
) : CacheableFunctionTransformer(context) {
    /**
     * ```kotlin
     * if (created) return cachedField
     * else {
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
        val functionThisReceiver = originFunction.dispatchReceiverParameter?.let { funcBuilder.irGet(it) }

        val getCachedField = funcBuilder.irGetField(functionThisReceiver, backendField)
        val createdVal = funcBuilder.valIsCreated(functionThisReceiver)

        originFunction.body = funcBuilder.irBlockBody {
            val elseInitializeBlock = irBlock {
                // val origin = originFunction()
                val calculatedVal = valInitByOrigin()
                +calculatedVal
                // cachedField = origin
                val getResultVal = irGet(calculatedVal)
                +irSetField(functionThisReceiver, backendField, getResultVal)
                // created = true
                +irSetField(functionThisReceiver, createdFlagField, getResultVal)
                // return origin
                +irReturn(getResultVal)
            }
            +irIfThenElse(
                irBuiltIns.unitType, irGet(createdVal), irReturn(getCachedField), elseInitializeBlock
            )
        }
    }
}

