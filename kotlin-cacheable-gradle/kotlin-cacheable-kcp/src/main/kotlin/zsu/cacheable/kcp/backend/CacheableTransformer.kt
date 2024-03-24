package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name
import zsu.cacheable.CacheMode
import zsu.cacheable.kcp.CACHEABLE_FQN
import zsu.cacheable.kcp.CacheableTransformError
import zsu.cacheable.kcp.builder
import zsu.cacheable.kcp.readCacheable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class CacheableTransformer(
    private val moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext
) : IrElementTransformer<Any?> {
    private val irBuiltIns = pluginContext.symbols.irBuiltIns

    fun doTransform() {
        moduleFragment.transformChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: Any?): IrStatement {
        val symbols = CacheableSymbols(irBuiltIns, moduleFragment)
        val originLogic = super.visitFunction(declaration, data)
        val cacheable = declaration.annotations.firstOrNull {
            it.annotationClass.kotlinFqName.asString() == CACHEABLE_FQN
        }?.readCacheable() ?: return originLogic

        // assertions
        val parentClass = declaration.parentClassOrNull ?: throw CacheableTransformError(
            "@Cacheable only available on class's function now, not support file level method currently."
        )
        validation(parentClass, declaration)

        val copiedFunction = moveOriginFunction(parentClass, declaration)
        val backendField = addBackendField(parentClass, declaration)
        // modify origin function
        when (cacheable.cacheMode) {
            CacheMode.SYNCHRONIZED -> SynchronizedTransformer(
                symbols, declaration, backendField, copiedFunction
            )

            CacheMode.NONE -> NormalTransformer(
                symbols, declaration, backendField, copiedFunction
            )
        }.doTransform()
        return declaration
    }

    @OptIn(ExperimentalContracts::class)
    private fun validation(parentClass: IrClass, function: IrFunction) {
        contract {
            returns() implies (function is IrSimpleFunction)
        }
        if (parentClass.isInterface) throw CacheableTransformError(
            "@Cacheable not available for interface: ${parentClass.kotlinFqName.asString()}"
        )
        if (function !is IrSimpleFunction) throw CacheableTransformError(
            "@Cacheable only supports simple functions, not support for current input: $function"
        )
        if (function.returnType.isNullable()) {
            val typeStr = function.returnType.dumpKotlinLike()
            val classFqn = parentClass.kotlinFqName.asString()
            val funcName = function.name
            throw CacheableTransformError(
                "@Cacheable not support nullable type($typeStr) current: $classFqn#$funcName"
            )
        }
        return
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

    private fun Name.cachedOriginFunctionName() = Name.identifier("cachedOrigin$$identifier")
    private fun Name.cachedBackendFieldName() = Name.identifier("cachedField$$identifier")

    private fun IrSymbolOwner.builder() = symbol.builder(irBuiltIns)
}