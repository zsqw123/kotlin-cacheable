package zsu.cacheable.kcp

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
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

    fun doTransform() {
        moduleFragment.transformChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: Any?): IrStatement {
        val originLogic = super.visitFunction(declaration, data)
        val cacheable = declaration.annotations.any {
            it.annotationClass.kotlinFqName.asString() == CACHEABLE_FQN
        }
        if (!cacheable) return originLogic
        val parentClass = declaration.parentClassOrNull ?: throw TransformError(
            "@Cacheable only available on class's function now, not support file level method currently."
        )
        if (parentClass.isInterface) throw TransformError(
            "@Cacheable not available for interface: ${parentClass.kotlinFqName.asString()}"
        )
        val copiedFunction = copyOriginFunction(parentClass, declaration)
        val backendField = addBackendField(parentClass, declaration)
        rewriteWithCachingLogic(declaration, backendField, copiedFunction)
        return declaration
    }

    private fun copyOriginFunction(
        parentClass: IrClass, originFunction: IrFunction,
    ) = originFunction.deepCopyWithVariables().apply {
        name = name.cachedOriginFunctionName()
    }.also { parentClass.addMember(it) }

    private fun addBackendField(
        parentClass: IrClass, originFunction: IrFunction,
    ) = parentClass.addField {
        isFinal = false
        name = originFunction.name.cachedBackendFieldName()
        type = originFunction.returnType.makeNullable()
    }

    private fun rewriteWithCachingLogic(
        originFunction: IrFunction, backendField: IrField, copiedFunction: IrFunction,
    ) {
        val factory = originFunction.factory
        val builder = originFunction.symbol.builder(irBuiltIns)
        val fieldInitializer = builder.irExprBody(builder.irGetField(null, backendField))
        originFunction.body = builder.irBlockBody {
            val cachedNowField = factory.buildField {
                isFinal = false
                type = originFunction.returnType.makeNullable()
            }.also { it.initializer = fieldInitializer }
            val thenInitializeBlock = irBlock {
                irSetField(
                    null, cachedNowField,
                    irCall(copiedFunction.symbol, copiedFunction.returnType).apply {
                        valueArgumentsCount = 1
                    }
                )
            }

            // get current cached first
            +cachedNowField
            // returns cache if not null
            builder.irIfNull(
                irBuiltIns.unitType,
                builder.irGetField(null, cachedNowField),
                thenInitializeBlock,
                builder.irGetField(null, cachedNowField),
            )
        }

    }

    private fun Name.cachedOriginFunctionName() = Name.identifier("cachedOrigin$$identifier")
    private fun Name.cachedBackendFieldName() = Name.identifier("cachedField$$identifier")

    class TransformError(message: String) : IllegalArgumentException(message)
}