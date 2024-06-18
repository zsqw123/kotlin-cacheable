package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.backend.jvm.fullValueParameterList
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.name.Name
import zsu.cacheable.kcp.defaultValueForType

open class TrackArgsTransformer protected constructor(cacheableTransformContext: CacheableTransformContext) :
    CacheableFunctionTransformer(cacheableTransformContext) {
    private val args = originFunction.fullValueParameterList

    /**
     * ```kotlin
     * if (created && compareArgs(arg0, arg1, arg2)) return cachedField
     * oldArg0 = arg0
     * oldArg1 = arg1
     * oldArg2 = arg2
     * return computeCache(arg0, arg1, arg2)
     * ```
     */
    open fun modifyBody(
        compareFunction: IrSimpleFunction, oldArgs: List<IrField>
    ): IrBody = funcBuilder.irBlockBody {
        +returnIfHitCache(compareFunction)
        assignOldArgs(oldArgs)
        computeCache()
    }

    protected fun IrStatementsBuilder<*>.assignOldArgs(oldArgs: List<IrField>) {
        for ((i, oldArg) in oldArgs.withIndex()) {
            +irSetField(functionThisReceiver, oldArg, irGet(args[i]))
        }
    }

    // if (created && compareArgs(arg0, arg1, arg2)) return cachedField
    protected fun IrStatementsBuilder<*>.returnIfHitCache(
        compareFunction: IrSimpleFunction,
    ): IrExpression {
        // compareArgs(arg0, arg1, arg2)
        val callCompareFunction = irCall(compareFunction).apply {
            extensionReceiver = originFunction.extensionReceiverParameter?.let { irGet(it) }
            dispatchReceiver = originFunction.dispatchReceiverParameter?.let { irGet(it) }
            for ((index, irValueParameter) in originFunction.valueParameters.withIndex()) {
                putValueArgument(index, irGet(irValueParameter))
            }
        }
        // if (created && compareArgs(arg0, arg1, arg2)) return cachedField
        return irIfThen(
            irBuiltIns.unitType, getIsCreated andWith callCompareFunction,
            irReturn(getCachedField),
        )
    }

    override fun doTransform(): IrBody {
        // add old args variable for compare with new args
        val oldArgs = addArgs()
        // add compare functions
        val compareFunction = addCompareFunction()
        compareFunction.body = compareFunction.builder().irBlockBody {
            +irReturn(compareArgsExpression(compareFunction, oldArgs))
        }
        return modifyBody(compareFunction, oldArgs)
    }

    private fun addArgs(): List<IrField> {
        val backendFieldName = backendField.name.identifier
        val originStatic = originFunction.isStatic
        val result = ArrayList<IrField>(args.size)
        for ((i, valueParameter) in args.withIndex()) {
            val paramType = valueParameter.type
            result += parentClass.addField {
                isStatic = originStatic
                isFinal = false
                name = Name.identifier("$backendFieldName$$i")
                type = paramType
                visibility = DescriptorVisibilities.PRIVATE
            }.also {
                val builder = it.builder()
                val defaultExpr = builder.defaultValueForType(paramType)
                it.initializer = builder.irExprBody(defaultExpr)
            }
        }
        return result
    }

    private fun addCompareFunction(): IrSimpleFunction = parentClass.addFunction {
        containerSource = originFunction.containerSource
        name = compareFunctionName
        returnType = irBuiltIns.booleanType
        visibility = DescriptorVisibilities.PRIVATE
    }.apply {
        dispatchReceiverParameter = originFunction.dispatchReceiverParameter
        extensionReceiverParameter = originFunction.extensionReceiverParameter
        contextReceiverParametersCount = originFunction.contextReceiverParametersCount
        copyParameterDeclarationsFrom(originFunction)
    }

    /**
     * ```
     * arg1 == oldArg1 &&
     *   arg2 == oldArg2 &&
     *   arg3 == oldArg3
     * ```
     */
    private fun IrBuilderWithScope.compareArgsExpression(
        compareFunction: IrSimpleFunction,
        oldArgs: List<IrField>,
    ): IrExpression {
        val compareFunctionArgs = compareFunction.valueParameters
        val thisReceiver = compareFunction.dispatchReceiverParameter?.let { irGet(it) }
        val firstOldArg = oldArgs.first()
        val firstNewArg = compareFunctionArgs.first()
        var expression = irGetField(thisReceiver, firstOldArg) eqWith irGet(firstNewArg)
        var currentIndex = 1
        while (currentIndex < compareFunctionArgs.size) {
            val currentParam = compareFunctionArgs[currentIndex]
            val singleJudgement = irGetField(thisReceiver, oldArgs[currentIndex]) eqWith irGet(currentParam)
            expression = expression andWith singleJudgement
            currentIndex++
        }
        return expression
    }

    context(IrBuilderWithScope)
    private infix fun IrExpression.eqWith(right: IrExpression): IrExpression {
        return irEquals(this, right)
    }

    context(IrBuilderWithScope)
    private infix fun IrExpression.andWith(right: IrExpression): IrExpression {
        return irIfThenElse(irBuiltIns.booleanType, this, right, irFalse())
    }

    private val compareFunctionName = Name.identifier(originFunction.name.identifier + "\$compare")

    companion object : Creator {
        override fun create(context: CacheableTransformContext): CacheableFunctionTransformer {
            if (context.originFunction.fullValueParameterList.isEmpty()) {
                return NormalTransformer.create(context)
            }
            return TrackArgsTransformer(context)
        }
    }
}

