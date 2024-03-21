package zsu.cacheable.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.types.makeNullable
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

        val copiedFunction = copyOriginFunction(parentClass, declaration)
        val backendField = addBackendField(parentClass, declaration)
        rewriteWithCachingLogic(declaration, backendField, copiedFunction)
        return declaration
    }

    private fun copyOriginFunction(
        parentClass: IrClass, originFunction: IrSimpleFunction,
    ) = originFunction.deepCopyWithVariables().apply {
        name = name.cachedOriginFunctionName()
    }.also { parentClass.addMember(it) }

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
        originFunction: IrSimpleFunction, backendField: IrField, copiedFunction: IrSimpleFunction,
    ) {
        // modify origin function, use origin function's symbol.
        val builder = originFunction.builder()
        val fieldInitializer = builder.irExprBody(builder.irGetField(backendField))

        val cachedNowField = factory.buildField {
            name = Name.identifier("cachedNow")
            isFinal = true
            type = originFunction.returnType.makeNullable()
        }.also { it.initializer = fieldInitializer }

        originFunction.body = builder.irBlockBody {
            val thenInitializeBlock = irBlock {
                val calculatedVal = calculatedVal(copiedFunction)
                +calculatedVal
                val getResultField = irGetField(calculatedVal)
                +irSetField(null, backendField, getResultField)
                +irReturn(getResultField)
            }
            // get current cached first
            +cachedNowField
            // returns cache if not null
            +irIfNull(
                irBuiltIns.unitType,
                irReturn(irGetField(cachedNowField)),
                thenInitializeBlock,
                irReturn(irGetField(cachedNowField)),
            )
        }
    }

    private fun calculatedVal(callee: IrSimpleFunction) = factory.buildField {
        name = Name.identifier("result")
        isFinal = true
        type = callee.returnType
    }.apply {
        val builder = builder()
        initializer = builder.irExprBody(builder.callCopiedFunction(callee))
    }

    private fun IrBuilderWithScope.callCopiedFunction(
        callee: IrSimpleFunction
    ) = irCall(callee.symbol, callee.returnType).apply {
        for ((index, irValueParameter) in callee.valueParameters.withIndex()) {
            putValueArgument(index, irGet(irValueParameter))
        }
    }

    private fun Name.cachedOriginFunctionName() = Name.identifier("cachedOrigin$$identifier")
    private fun Name.cachedBackendFieldName() = Name.identifier("cachedField$$identifier")

    class TransformError(message: String) : IllegalArgumentException(message)

    private fun IrSymbolOwner.builder() = symbol.builder(irBuiltIns)
    private fun IrBuilderWithScope.irGetField(field: IrField) = irGetField(null, field)
}