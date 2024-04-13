package zsu.cacheable.kcp.backend

import org.jetbrains.kotlin.backend.common.ir.addExtensionReceiver
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class CacheableSymbols(
    private val moduleFragment: IrModuleFragment,
    val irBuiltIns: IrBuiltIns,
) {
    private val irFactory = irBuiltIns.irFactory
    private val kotlinJvmInternalUnsafeFqn = FqName("kotlin.jvm.internal.unsafe")

    val monitorEnter = irBuiltIns.findFunctions(
        Name.identifier("monitorEnter"), kotlinJvmInternalUnsafeFqn,
    ).first()

    val monitorExit = irBuiltIns.findFunctions(
        Name.identifier("monitorExit"), kotlinJvmInternalUnsafeFqn,
    ).first()

    private val javaLang: IrPackageFragment = createPackage("java.lang")

    private val kotlinJvm: IrPackageFragment = createPackage("kotlin.jvm")

    val javaLangClass: IrClassSymbol = createClass(
        javaLang, "Class", ClassKind.CLASS, Modality.FINAL
    )

    val javaLangClassType = javaLangClass.starProjectedType
    val kotlinClassType = irBuiltIns.kClassClass.starProjectedType

    val kClassJavaGetterSymbol: IrSimpleFunctionSymbol

    val kClassJava: IrPropertySymbol = irFactory.buildProperty {
        name = Name.identifier("java")
    }.apply {
        parent = kotlinJvm
        kClassJavaGetterSymbol = addGetter().apply {
            addExtensionReceiver(irBuiltIns.kClassClass.starProjectedType)
            returnType = javaLangClass.defaultType
        }.symbol
    }.symbol

    private fun createPackage(packageName: String): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
            moduleFragment.descriptor,
            FqName(packageName)
        )

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        classModality: Modality
    ): IrClassSymbol = irFactory.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }.symbol
}