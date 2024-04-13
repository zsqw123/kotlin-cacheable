package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

class NormalTransformer(
    context: CacheableTransformContext
) : CacheableFunctionTransformer(context) {
    /**
     * ```kotlin
     * if (created) return cachedField
     * val origin = originFunction()
     * cachedField = origin
     * created = true
     * return origin
     * ```
     */
    override fun doTransform(): IrBlockBody = funcBuilder.irBlockBody {
        // if (created) return cachedField
        +irIfThen(irBuiltIns.unitType, getIsCreated, irReturn(getCachedField))
        computeCache()
    }
}

