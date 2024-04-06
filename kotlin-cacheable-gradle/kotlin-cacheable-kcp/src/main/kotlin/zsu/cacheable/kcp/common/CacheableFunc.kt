package zsu.cacheable.kcp.common

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.Name
import zsu.cacheable.kcp.CacheableTransformError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

data class CacheableFunc(
    val origin: IrSimpleFunction,
) {
    private val funcIdentifier = "${origin.name.identifier}_${origin.generateDesc()}"
    val copiedOriginFunctionName = Name.identifier("cachedOrigin\$$funcIdentifier")
    val backendFieldName = Name.identifier("cachedField\$$funcIdentifier")
    val createdFlagFieldName = Name.identifier("cacheCreated\$$funcIdentifier")

    @OptIn(ExperimentalContracts::class)
    fun validation(parentClass: IrClass, function: IrFunction) {
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
}

private fun IrSimpleFunction.generateDesc(): String {
    val argDesc = valueParameters.joinToString("_") {
        it.type.classFqName?.asString().orEmpty()
    }
    return argDesc.replace('.', '_')
}
