package zsu.cacheable.kcp.bytecode

import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import zsu.cacheable.kcp.CACHEABLE_FQN
import zsu.cacheable.kcp.common.CacheableFunc
import zsu.cacheable.kcp.common.cacheableFuncValidation
import zsu.cacheable.kcp.logger
import zsu.cacheable.kcp.readCacheable

class CacheableClassGenerator(
    private val compilerConfiguration: CompilerConfiguration,
    private val origin: ClassGenerator,
    private val irClass: IrClass?,
) : ClassGenerator by origin {
    override fun newMethod(
        declaration: IrFunction?, access: Int, name: String, desc: String,
        signature: String?, exceptions: Array<out String>?,
    ): MethodVisitor {
        val originMethodVisitor = origin.newMethod(
            declaration, access, name, desc, signature, exceptions
        )

        if (declaration == null) return originMethodVisitor
        val cacheable = declaration.annotations.firstOrNull {
            it.annotationClass.kotlinFqName.asString() == CACHEABLE_FQN
        }?.readCacheable() ?: return originMethodVisitor

        // validation
        require(declaration is IrSimpleFunction && irClass != null) {
            "unsupported function: $name in class: ${irClass?.kotlinFqName}"
        }
        declaration.cacheableFuncValidation(irClass)

        // create generator
        val originMethodNode = MethodNode(
            access, name, desc, signature, exceptions,
        ).apply { accept(originMethodVisitor) }
        val generator = CacheableGenerator(
            CacheableFunc(declaration), originMethodNode, cacheable,
        )

        // copy origin function to a new method node
        generator.copiedOriginFunc().attach()

        // generate backend
        generator.generateBackendField().attach()
        generator.createdBoolField().attach()

        // modify origin function
        val modified = generator.modifiedMethodNode()
        compilerConfiguration.logger.log("Cacheable cached method: ${irClass.name}#$name")
        return modified
    }

    private fun MethodNode.attach() = accept(
        origin.newMethod(
            null, access, name, desc, signature, exceptions.toTypedArray(),
        )
    )

    private fun FieldNode.attach() {
        val newFieldVisitor = origin.newField(
            null, access, name, desc, signature, null
        )
        acceptField(this, newFieldVisitor)
    }
}

private fun acceptField(fieldNode: FieldNode, fieldVisitor: FieldVisitor) {
    fieldNode.visibleAnnotations?.acceptEach {
        fieldVisitor.visitAnnotation(desc, true)
    }
    fieldNode.invisibleAnnotations?.acceptEach {
        fieldVisitor.visitAnnotation(desc, false)
    }
    fieldNode.visibleTypeAnnotations?.acceptEach {
        fieldVisitor.visitTypeAnnotation(typeRef, typePath, desc, true)
    }
    fieldNode.invisibleTypeAnnotations?.acceptEach {
        fieldVisitor.visitTypeAnnotation(typeRef, typePath, desc, false)
    }
    fieldNode.attrs?.forEach { attr ->
        fieldVisitor.visitAttribute(attr)
    }
    fieldNode.visitEnd()
}

private inline fun <T : AnnotationNode> List<T>.acceptEach(
    visitor: T.() -> AnnotationVisitor
) = forEach { annotationNode ->
    annotationNode.accept(visitor(annotationNode))
}
