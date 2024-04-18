package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.backend.jvm.fullValueParameterList
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody

class TrackArgsSyncTransformer private constructor(
    private val cacheableContext: CacheableTransformContext
) : TrackArgsTransformer(cacheableContext) {
    /**
     * ```kotlin
     * if (created && compareArgs(arg0, arg1, arg2)) return cachedField
     * synchronized(this) {
     *   if (created && compareArgs(arg0, arg1, arg2)) return cachedField
     *   oldArg0 = arg0
     *   oldArg1 = arg1
     *   oldArg2 = arg2
     *   return computeCache(arg0, arg1, arg2)
     * }
     * ```
     */
    override fun modifyBody(
        compareFunction: IrSimpleFunction, oldArgs: List<IrField>
    ): IrBody = funcBuilder.irBlockBody {
        +returnIfHitCache(compareFunction)
        synchronizedBlock(cacheableContext) {
            +returnIfHitCache(compareFunction)
            assignOldArgs(oldArgs)
            computeCache()
        }
    }

    companion object : Creator {
        override fun create(context: CacheableTransformContext): CacheableFunctionTransformer {
            if (context.originFunction.fullValueParameterList.isEmpty()) {
                return SynchronizedTransformer.create(context)
            }
            return TrackArgsSyncTransformer(context)
        }
    }
}

