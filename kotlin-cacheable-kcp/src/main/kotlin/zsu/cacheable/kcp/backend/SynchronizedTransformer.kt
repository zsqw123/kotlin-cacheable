package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

class SynchronizedTransformer private constructor(
    private val cacheableTransformContext: CacheableTransformContext,
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
        synchronizedBlock(cacheableTransformContext) {
            // if (created) return cachedField
            +irIfThen(irBuiltIns.unitType, getIsCreated, irReturn(getCachedField))
            computeCache()
        }
    }

    companion object : Creator {
        override fun create(context: CacheableTransformContext) = SynchronizedTransformer(context)
    }
}
