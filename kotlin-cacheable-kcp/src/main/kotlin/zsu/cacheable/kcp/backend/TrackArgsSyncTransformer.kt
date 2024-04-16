package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.expressions.IrBody

class TrackArgsSyncTransformer private constructor(context: CacheableTransformContext) :
    TrackArgsTransformer(context) {
    private val args = originFunction.valueParameters

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
    override fun modifyBody(): IrBody {
        return super.modifyBody()
    }

    override fun doTransform(): IrBody {
        if (args.isEmpty()) return transformTo(SynchronizedTransformer)
        // add old args variable for compare with new args
        val oldArgs = addArgs()
        // add compare functions
        val compareFunction = addCompareFunction()
        compareFunction.body = compareFunction.builder().irBlockBody {
            +irReturn(compareArgsExpression(oldArgs))
        }

        funcBuilder.irBlockBody {

        }
    }

    companion object : Creator {
        override fun create(context: CacheableTransformContext) = TrackArgsSyncTransformer(context)
    }
}

