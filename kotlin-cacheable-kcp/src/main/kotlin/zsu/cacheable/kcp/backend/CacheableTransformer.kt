package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irFalse
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import zsu.cacheable.CacheMode
import zsu.cacheable.kcp.*
import zsu.cacheable.kcp.common.CacheableFunc
import zsu.cacheable.kcp.common.validationForCacheable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class CacheableTransformer(
    private val moduleFragment: IrModuleFragment,
    private val pluginContext: IrPluginContext
) : IrElementTransformer<Any?> {
    private val irBuiltIns = pluginContext.symbols.irBuiltIns
    private val symbols = CacheableSymbols(moduleFragment, pluginContext)

    fun doTransform() {
        moduleFragment.transformChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction, data: Any?): IrStatement {
        val originLogic = super.visitFunction(declaration, data)
        // skip fake override functions
        if (declaration.isFakeOverride) return originLogic
        val cacheable = declaration.annotations.firstOrNull {
            it.annotationClass.kotlinFqName.asString() == CACHEABLE_FQN
        }?.readCacheable() ?: return originLogic

        // assertions
        val parentClass = declaration.parentClassOrNull ?: throw CacheableTransformError(
            "@Cacheable only available on class's function now, not support file level method currently. " +
                    "Use object to achieve similar behaviors."
        )
        validation(parentClass, declaration)

        val cacheableFunc = CacheableFunc(declaration)
        val copiedFunction = moveOriginFunction(parentClass, cacheableFunc)
        val backendField = addBackendField(parentClass, cacheableFunc)
        val createdFlagField = addCreatedFlagField(parentClass, cacheableFunc)
        val cacheableTransformContext = CacheableTransformContext(
            symbols, parentClass, declaration,
            backendField, copiedFunction, createdFlagField,
        )
        // modify origin function
        declaration.body = when (cacheable.cacheMode) {
            CacheMode.SYNCHRONIZED -> SynchronizedTransformer
            CacheMode.TRACK_ARGS -> TrackArgsTransformer
            CacheMode.TRACK_ARGS_SYNCHRONIZED -> TrackArgsSyncTransformer
            CacheMode.NONE -> NormalTransformer
        }.create(cacheableTransformContext).doTransform()

        return declaration
    }

    private fun addCreatedFlagField(
        parentClass: IrClass, cacheableFunc: CacheableFunc
    ) = parentClass.addField {
        isStatic = cacheableFunc.origin.isStatic
        isFinal = false
        name = cacheableFunc.createdFlagFieldName
        type = irBuiltIns.booleanType
        visibility = DescriptorVisibilities.PRIVATE
    }.also {
        val builder = it.builder()
        it.annotations += builder.volatileAnnotation(symbols)
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
        containerSource = cacheableFunc.origin.containerSource
        name = cacheableFunc.copiedOriginFunctionName
        returnType = cacheableFunc.returnType
        visibility = DescriptorVisibilities.PRIVATE
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
        visibility = DescriptorVisibilities.PRIVATE
    }.also {
        val builder = it.builder()
        val returnType = cacheableFunc.origin.returnType
        val defaultExpr = builder.defaultValueForType(returnType)
        it.initializer = builder.irExprBody(defaultExpr)
    }

    private fun IrSymbolOwner.builder() = symbol.builder(irBuiltIns, startOffset, endOffset)
}