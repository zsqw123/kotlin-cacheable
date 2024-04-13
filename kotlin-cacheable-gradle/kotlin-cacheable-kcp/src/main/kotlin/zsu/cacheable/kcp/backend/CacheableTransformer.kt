package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.ir.util.isStatic
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import zsu.cacheable.CacheMode
import zsu.cacheable.kcp.CACHEABLE_FQN
import zsu.cacheable.kcp.CacheableTransformError
import zsu.cacheable.kcp.builder
import zsu.cacheable.kcp.common.CacheableFunc
import zsu.cacheable.kcp.common.validationForCacheable
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
        val symbols = CacheableSymbols(moduleFragment, irBuiltIns)
        val originLogic = super.visitFunction(declaration, data)
        val cacheable = declaration.annotations.firstOrNull {
            it.annotationClass.kotlinFqName.asString() == CACHEABLE_FQN
        }?.readCacheable() ?: return originLogic

        // assertions
        val parentClass = declaration.parentClassOrNull ?: throw CacheableTransformError(
            "@Cacheable only available on class's function now, not support file level method currently."
        )
        validation(parentClass, declaration)

        val cacheableFunc = CacheableFunc(declaration)
        val copiedFunction = moveOriginFunction(parentClass, cacheableFunc)
        val backendField = addBackendField(parentClass, cacheableFunc)
        val createdFlagField = addCreatedFlagField(parentClass, cacheableFunc)
        val cacheableTransformContext = CacheableTransformContext(
            symbols, parentClass, declaration, backendField, copiedFunction, createdFlagField,
        )
        // modify origin function
        when (cacheable.cacheMode) {
            CacheMode.SYNCHRONIZED -> SynchronizedTransformer(cacheableTransformContext)
            CacheMode.NONE -> NormalTransformer(cacheableTransformContext)
            else -> TODO() // unsupported now :{
        }.doTransform()
        return declaration
    }

    private fun addCreatedFlagField(
        parentClass: IrClass, cacheableFunc: CacheableFunc
    ) = parentClass.addField {
        isStatic = cacheableFunc.origin.isStatic
        isFinal = false
        name = cacheableFunc.createdFlagFieldName
        type = irBuiltIns.booleanType
    }.also {
        val builder = it.builder()
        it.initializer = builder.irExprBody(builder.irFalse())
    }

    @OptIn(ExperimentalContracts::class)
    private fun validation(parentClass: IrClass, function: IrFunction) {
        contract {
            returns() implies (function is IrSimpleFunction)
        }
        function.validationForCacheable(parentClass)
    }

    private fun moveOriginFunction(
        parentClass: IrClass, cacheableFunc: CacheableFunc,
    ) = parentClass.addFunction {
        updateFrom(cacheableFunc.origin)
        name = cacheableFunc.copiedOriginFunctionName
        returnType = cacheableFunc.returnType
    }.apply {
        val originFunction = cacheableFunc.origin
        dispatchReceiverParameter = originFunction.dispatchReceiverParameter
        extensionReceiverParameter = originFunction.extensionReceiverParameter
        contextReceiverParametersCount = originFunction.contextReceiverParametersCount
        copyParameterDeclarationsFrom(originFunction)
        body = originFunction.moveBodyTo(this)
    }

    private fun addBackendField(
        parentClass: IrClass, cacheableFunc: CacheableFunc,
    ) = parentClass.addField {
        isStatic = cacheableFunc.origin.isStatic
        isFinal = false
        name = cacheableFunc.backendFieldName
        type = cacheableFunc.origin.returnType
    }.also {
        val builder = it.builder()
        val returnType = cacheableFunc.origin.returnType
        val defaultExpr = IrConstImpl.defaultValueForType(builder.startOffset, builder.endOffset, returnType)
        it.initializer = builder.irExprBody(defaultExpr)
    }

    private fun IrSymbolOwner.builder() = symbol.builder(irBuiltIns)
}