package zsu.cacheable.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name

class CacheableTransformer(
    private val moduleFragment: IrModuleFragment,
    private val pluginContext: IrPluginContext
) : IrElementTransformer<Any?> {
    private val irBuiltIns = pluginContext.symbols.irBuiltIns
    private val factory = irBuiltIns.irFactory

    fun doTransform() {
        moduleFragment.transformChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: Any?): IrStatement {
        val originLogic = super.visitFunction(declaration, data)
        val cacheable = declaration.annotations.any {
            it.annotationClass.kotlinFqName.asString() == CACHEABLE_FQN
        }
        if (!cacheable) return originLogic

        // assertions
        val parentClass = declaration.parentClassOrNull ?: throw TransformError(
            "@Cacheable only available on class's function now, not support file level method currently."
        )
        if (parentClass.isInterface) throw TransformError(
            "@Cacheable not available for interface: ${parentClass.kotlinFqName.asString()}"
        )
        if (declaration !is IrSimpleFunction) throw TransformError(
            "@Cacheable only supports simple functions, not support for current input: $declaration"
        )

        val copiedFunction = moveOriginFunction(parentClass, declaration)
        val backendField = addBackendField(parentClass, declaration)
        rewriteWithCachingLogic(declaration, backendField, copiedFunction)
        return declaration
    }

    private fun moveOriginFunction(
        parentClass: IrClass, originFunction: IrSimpleFunction,
    ) = parentClass.addFunction {
        updateFrom(originFunction)
        name = originFunction.name.cachedOriginFunctionName()
        returnType = originFunction.returnType
    }.apply {
        copyParameterDeclarationsFrom(originFunction)
        body = originFunction.moveBodyTo(this)
    }

    private fun addBackendField(
        parentClass: IrClass, originFunction: IrFunction,
    ) = parentClass.addField {
        isFinal = false
        name = originFunction.name.cachedBackendFieldName()
        type = originFunction.returnType.makeNullable()
    }.also {
        val builder = it.builder()
        it.initializer = builder.irExprBody(builder.irNull())
    }

    private fun rewriteWithCachingLogic(
        originFunction: IrSimpleFunction,
        backendField: IrField,
        copiedFunction: IrSimpleFunction,
    ) {
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
                val calculatedVal = calculatedVal(originFunction, copiedFunction)
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

    private fun IrBlockBuilder.calculatedVal(
        callFrom: IrSimpleFunction,
        callee: IrSimpleFunction,
    ) = scope.createTemporaryVariable(
        callCopiedFunction(callFrom, callee),
        nameHint = "result",
    )

    private fun IrBuilderWithScope.callCopiedFunction(
        callFrom: IrSimpleFunction,
        callee: IrSimpleFunction,
    ) = irCall(callee.symbol, callee.returnType).apply {
        extensionReceiver = callFrom.extensionReceiverParameter?.let { irGet(it) }
        dispatchReceiver = callFrom.dispatchReceiverParameter?.let { irGet(it) }
        for ((index, irValueParameter) in callFrom.valueParameters.withIndex()) {
            putValueArgument(index, irGet(irValueParameter))
        }
    }

    private fun Name.cachedOriginFunctionName() = Name.identifier("cachedOrigin$$identifier")
    private fun Name.cachedBackendFieldName() = Name.identifier("cachedField$$identifier")

    class TransformError(message: String) : IllegalArgumentException(message)

    private fun IrSymbolOwner.builder() = symbol.builder(irBuiltIns)
}