package zsu.cacheable.kcp.common

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.Name
import zsu.cacheable.kcp.CacheableTransformError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class CacheableFunc(
    val origin: IrSimpleFunction,
) {
    val returnType = origin.returnType
    private val funcIdentifier = origin.name.asStringStripSpecialMarkers()
    private val funcDesc = "${funcIdentifier}_${origin.generateDesc()}"
    val copiedOriginFunctionName = Name.identifier("cachedOrigin\$$funcIdentifier")
    val backendFieldName = Name.identifier("cachedField\$$funcDesc")
    val createdFlagFieldName = Name.identifier("cacheCreated\$$funcDesc")
}

@OptIn(ExperimentalContracts::class)
fun IrFunction.validationForCacheable(parentClass: IrClass) {
    contract {
        returns() implies (this@validationForCacheable is IrSimpleFunction)
    }
    val function = this
    if (parentClass.isInterface) throw CacheableTransformError(
        "@Cacheable not available for interface: ${parentClass.kotlinFqName.asString()}"
    )
    if (function !is IrSimpleFunction) throw CacheableTransformError(
        "@Cacheable only supports simple functions, not support for current input: $function"
    )
    return
}

private fun IrSimpleFunction.generateDesc(): String {
    val argDesc = valueParameters.joinToString("_") {
        it.type.classFqName?.asString().orEmpty()
    }
    return argDesc.replace('.', '_')
}
