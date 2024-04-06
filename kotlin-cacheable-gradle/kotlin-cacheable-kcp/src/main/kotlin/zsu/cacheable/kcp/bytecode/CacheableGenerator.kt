package zsu.cacheable.kcp.bytecode

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import zsu.cacheable.kcp.common.CacheableFunc

class CacheableGenerator(
    private val cacheableFunc: CacheableFunc,
) {
    fun copiedOriginFunc(originNode: MethodNode): MethodNode {
        val copiedOriginFunctionName = cacheableFunc.copiedOriginFunctionName.identifier
        val copiedMethodNode = MethodNode(
            originNode.access, copiedOriginFunctionName,
            originNode.desc, originNode.signature,
            originNode.exceptions.toTypedArray(),
        )

        // todo

        return copiedMethodNode
    }

    fun generateBackendField(originNode: MethodNode): FieldNode {
        val backendFieldName = cacheableFunc.backendFieldName.identifier
        return FieldNode(
            Opcodes.ACC_PRIVATE, backendFieldName, originNode.desc,
            originNode.desc, // signature can be same with desc.
            null,
        )
    }

    fun modifiedMethodNode(originNode: MethodNode): MethodNode {
        val modifiedNode = MethodNode(
            originNode.access, originNode.name,
            originNode.desc, originNode.signature,
            originNode.exceptions.toTypedArray(),
        )

        // todo

        return modifiedNode
    }
}