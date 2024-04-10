package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class CacheableSymbols(
    val irBuiltIns: IrBuiltIns,
) {
    private val kotlinJvmInternalUnsafeFqn = FqName("kotlin.jvm.internal.unsafe")

    val monitorEnter = irBuiltIns.findFunctions(
        Name.identifier("monitorEnter"), kotlinJvmInternalUnsafeFqn,
    ).first()

    val monitorExit = irBuiltIns.findFunctions(
        Name.identifier("monitorExit"), kotlinJvmInternalUnsafeFqn,
    ).first()
}