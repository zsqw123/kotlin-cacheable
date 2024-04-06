package zsu.cacheable.kcp.bytecode

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import zsu.cacheable.Cacheable
import zsu.cacheable.kcp.common.CacheableFunc

class CacheableGenerator(
    private val cacheableFunc: CacheableFunc,
    private val originNode: MethodNode,
    private val cacheable: Cacheable,
) {
    private val copiedOriginFunctionName = cacheableFunc.copiedOriginFunctionName.identifier
    private val backendFieldName = cacheableFunc.backendFieldName.identifier
    private val createdFlagFieldName = cacheableFunc.createdFlagFieldName.identifier
    private val originNodeDesc = originNode.desc
    private val originNodeTypeDesc = Type.getType(originNode.desc).returnType.descriptor

    fun copiedOriginFunc(): MethodNode {
        val copiedMethodNode = MethodNode(
            originNode.access, copiedOriginFunctionName,
            originNode.desc, originNode.signature,
            originNode.exceptions.toTypedArray(),
        ).apply {
            parameters = originNode.parameters
            visibleAnnotations = emptyList()
            invisibleAnnotations = emptyList()
            visibleTypeAnnotations = originNode.visibleTypeAnnotations
            invisibleTypeAnnotations = originNode.invisibleTypeAnnotations
            attrs = originNode.attrs
            annotationDefault = originNode.annotationDefault
            visibleParameterAnnotations = originNode.visibleParameterAnnotations
            invisibleParameterAnnotations = originNode.invisibleParameterAnnotations
            instructions = originNode.instructions
            tryCatchBlocks = originNode.tryCatchBlocks
            maxStack = originNode.maxStack
            maxLocals = originNode.maxLocals
            localVariables = originNode.localVariables
            visibleLocalVariableAnnotations = originNode.visibleLocalVariableAnnotations
            invisibleLocalVariableAnnotations = originNode.invisibleLocalVariableAnnotations
        }
        return copiedMethodNode
    }

    fun generateBackendField(): FieldNode {
        return FieldNode(
            Opcodes.ACC_PRIVATE, backendFieldName, originNodeTypeDesc,
            originNodeTypeDesc, // signature can be same with desc.
            null,
        )
    }

    fun createdBoolField(): FieldNode {
        return FieldNode(
            Opcodes.ACC_PRIVATE and Opcodes.ACC_VOLATILE,
            createdFlagFieldName, booleanDesc,
            booleanDesc, false,
        )
    }

    fun modifiedMethodNode(): MethodNode {
        val modifiedNode = MethodNode(
            originNode.access, originNode.name,
            originNode.desc, originNode.signature,
            originNode.exceptions.toTypedArray(),
        )

        // todo

        return modifiedNode
    }
}

private val booleanDesc = Type.BOOLEAN_TYPE.descriptor

var c: IntArray? = intArrayOf()
fun a(): IntArray {
    var tmp = c
    if (tmp != null) {
        return tmp
    } else {
        tmp = a()
        c = tmp
        return tmp
    }
}
