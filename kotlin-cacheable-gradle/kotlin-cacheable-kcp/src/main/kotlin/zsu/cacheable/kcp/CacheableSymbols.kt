package zsu.cacheable.kcp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class CacheableSymbols(
    val irBuiltIns: IrBuiltIns,
    val moduleFragment: IrModuleFragment,
) {
    private val irFactory: IrFactory = IrFactoryImpl

    private val kotlinPackage = createPackage(moduleFragment, "kotlin")

    private val standardKt = createClass(kotlinPackage, "StandardKt")

    val synchronizedFun = standardKt.addFunction {
        name = Name.identifier("synchronized")
    }.apply {
        val typeParameter = addTypeParameter("R", irBuiltIns.anyNType)
        val typeParameterType = typeParameter.defaultType
        returnType = typeParameterType
        addValueParameter(Name.identifier("lock"), irBuiltIns.anyType)
        val blockType = irBuiltIns.functionN(1).typeWith(listOf(typeParameterType, returnType))
        addValueParameter(Name.identifier("block"), blockType)
    }

    private fun createPackage(moduleFragment: IrModuleFragment, packageName: String): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
            moduleFragment.descriptor,
            FqName(packageName)
        )

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
    ): IrClass = createClass(irPackage, shortName, ClassKind.CLASS, Modality.FINAL)

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        classModality: Modality
    ): IrClass = irFactory.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
    }.apply {
        parent = irPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }
}