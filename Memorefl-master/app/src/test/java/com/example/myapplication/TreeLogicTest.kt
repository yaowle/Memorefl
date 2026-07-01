package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class TreeLogicTest {

    private val root = KnowledgeNode(
        id = "root",
        title = "Root",
        children = listOf(
            KnowledgeNode("1", "Child 1", weight = 1f),
            KnowledgeNode("2", "Child 2", weight = 2f, children = listOf(
                KnowledgeNode("2-1", "Grandchild 2-1", isDefault = true)
            ))
        )
    )

    @Test
    fun testFindDefaultNode() {
        val found = TreeLogic.findDefaultNode(root)
        assertNotNull(found)
        assertEquals("2-1", found?.id)
    }

    @Test
    fun testFindNodeById() {
        val found = TreeLogic.findNodeById(root, "2-1")
        assertNotNull(found)
        assertEquals("Grandchild 2-1", found?.title)

        val notFound = TreeLogic.findNodeById(root, "non-existent")
        assertNull(notFound)
    }

    @Test
    fun testUpdateNodeInTree() {
        val updatedChild = KnowledgeNode("1", "Updated Child 1")
        val newTree = TreeLogic.updateNodeInTree(root, updatedChild)
        
        val found = TreeLogic.findNodeById(newTree, "1")
        assertEquals("Updated Child 1", found?.title)
    }

    @Test
    fun testDeleteNodeFromTree() {
        val newTree = TreeLogic.deleteNodeFromTree(root, "1")
        assertEquals(1, newTree.children.size)
        assertNull(TreeLogic.findNodeById(newTree, "1"))
    }

    @Test
    fun testMoveNodeInTree() {
        // Move "2" up (swap with "1")
        val newTree = TreeLogic.moveNodeInTree(root, "2", up = true)
        assertEquals("2", newTree.children[0].id)
        assertEquals("1", newTree.children[1].id)
    }

    @Test
    fun testFlattenTree() {
        val flat = TreeLogic.flattenTree(root)
        // Root + 2 Children + 1 Grandchild = 4
        assertEquals(4, flat.size)
        assertEquals("root", flat[0].node.id)
        assertEquals(0, flat[0].level)
        assertEquals("2-1", flat[3].node.id)
        assertEquals(2, flat[3].level)
    }
}
