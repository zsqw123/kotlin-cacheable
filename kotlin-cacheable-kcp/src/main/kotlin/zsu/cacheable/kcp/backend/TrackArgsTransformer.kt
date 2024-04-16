package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.backend.jvm.fullValueParameterList
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicOperatorExpressionImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.name.Name
import zsu.cacheable.kcp.defaultValueForType

class TrackArgsTransformer private constructor(cacheableTransformContext: CacheableTransformContext) :
    CacheableFunctionTransformer(cacheableTransformContext) {
    private val args = originFunction.valueParameters
    override fun doTransform(): IrBody {
        if (args.isEmpty()) return transformTo(NormalTransformer)
    }

    private fun addArgs(): List<IrField> {
        val backendFieldName = backendField.name.identifier
        val originStatic = originFunction.isStatic
        val args = originFunction.fullValueParameterList
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

    /**
     * ```
     * arg1 == oldArg1 &&
     *   arg2 == oldArg2 &&
     *   arg3 == oldArg3
     * ```
     */
    private fun IrBuilderWithScope.compareArgsExpression(
        oldArgs: List<IrField>,
    ): IrExpression {
        val args = originFunction.fullValueParameterList
        for (valueParameter in args) {
            valueParameter
        }
    }

    private fun IrBuilderWithScope.eqExpr(
        left: IrExpression, right: IrExpression
    ): IrExpression = operatorExpr(
        left, right, IrDynamicOperator.EQEQ, irBuiltIns.booleanType
    )

    private fun IrBuilderWithScope.andExpr(
        left: IrExpression, right: IrExpression
    ): IrExpression = operatorExpr(
        left, right, IrDynamicOperator.ANDAND, irBuiltIns.booleanType
    )

    private fun IrBuilderWithScope.operatorExpr(
        left: IrExpression, right: IrExpression, operator: IrDynamicOperator, exprType: IrType,
    ): IrExpression = IrDynamicOperatorExpressionImpl(
        startOffset, endOffset, exprType, operator
    ).apply {
        receiver = left
        arguments += right
    }

    companion object : Creator {
        override fun create(context: CacheableTransformContext): CacheableFunctionTransformer {
            return TrackArgsTransformer(context)
        }
    }
}

