package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.expressions.IrBody

class TrackArgsSyncTransformer private constructor(context: CacheableTransformContext) :
    CacheableFunctionTransformer(context) {
    private val args = originFunction.valueParameters
    override fun doTransform(): IrBody {
        if (args.isEmpty()) return transformTo(SynchronizedTransformer)
        TODO("Not yet implemented")
    }

    companion object : Creator {
        override fun create(context: CacheableTransformContext) = TrackArgsSyncTransformer(context)
    }
}

