package com.example.myapplication

/**
 * 树形结构的逻辑处理工具类
 */
object TreeLogic {

    fun findDefaultNode(node: KnowledgeNode): KnowledgeNode? {
        if (node.isDefault) return node
        for (child in node.children) {
            val found = findDefaultNode(child)
            if (found != null) return found
        }
        return null
    }

    fun flattenTree(root: KnowledgeNode): List<FlatNode> {
        val result = mutableListOf<FlatNode>()
        result.add(FlatNode(root, 0, emptyList(), isFirst = true, isLast = true))
        
        fun traverse(node: KnowledgeNode, level: Int, path: List<String>) {
            node.children.forEachIndexed { index, child ->
                result.add(FlatNode(child, level, path, index == 0, index == node.children.size - 1))
                traverse(child, level + 1, path + child.id)
            }
        }
        traverse(root, 1, emptyList())
        return result
    }

    fun updateNodeInTree(root: KnowledgeNode, updatedNode: KnowledgeNode): KnowledgeNode {
        if (root.id == updatedNode.id) return updatedNode
        return root.copy(children = root.children.map { updateNodeInTree(it, updatedNode) })
    }

    fun deleteNodeFromTree(root: KnowledgeNode, targetId: String): KnowledgeNode {
        return root.copy(children = root.children
            .filter { it.id != targetId }
            .map { deleteNodeFromTree(it, targetId) }
        )
    }

    fun moveNodeInTree(root: KnowledgeNode, targetId: String, up: Boolean): KnowledgeNode {
        val index = root.children.indexOfFirst { it.id == targetId }
        if (index != -1) {
            val newChildren = root.children.toMutableList()
            if (up && index > 0) {
                val tmp = newChildren[index]
                newChildren[index] = newChildren[index - 1]
                newChildren[index - 1] = tmp
                return root.copy(children = newChildren)
            } else if (!up && index < newChildren.size - 1) {
                val tmp = newChildren[index]
                newChildren[index] = newChildren[index + 1]
                newChildren[index + 1] = tmp
                return root.copy(children = newChildren)
            }
        }
        return root.copy(children = root.children.map { moveNodeInTree(it, targetId, up) })
    }

    fun findNodeById(root: KnowledgeNode, id: String): KnowledgeNode? {
        if (root.id == id) return root
        for (child in root.children) {
            val found = findNodeById(child, id)
            if (found != null) return found
        }
        return null
    }
}

data class FlatNode(
    val node: KnowledgeNode,
    val level: Int,
    val path: List<String>,
    val isFirst: Boolean,
    val isLast: Boolean
)
