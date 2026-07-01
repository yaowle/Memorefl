package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class PersistenceConversionTest {

    @Test
    fun testTreeToEntitiesAndBack() {
        val root = KnowledgeNode(
            id = "root",
            title = "Root",
            children = listOf(
                KnowledgeNode("1", "Child 1", weight = 1f, nodeType = NodeType.NOTE, content = "Some note"),
                KnowledgeNode("2", "Child 2", weight = 2f, children = listOf(
                    KnowledgeNode("2-1", "Grandchild 2-1", isDefault = true)
                ))
            )
        )

        val schemeName = "TestScheme"
        val entities = root.toEntities(schemeName)

        // Root + 2 Children + 1 Grandchild = 4
        assertEquals(4, entities.size)
        
        // Verify parent-child relationships
        val rootEntity = entities.find { it.id == "root" }
        assertNull(rootEntity?.parentId)
        
        val child1Entity = entities.find { it.id == "1" }
        assertEquals("root", child1Entity?.parentId)
        assertEquals(NodeType.NOTE, child1Entity?.nodeType)
        assertEquals("Some note", child1Entity?.content)

        val grandchildEntity = entities.find { it.id == "2-1" }
        assertEquals("2", grandchildEntity?.parentId)
        assertTrue(grandchildEntity?.isDefault == true)

        // Convert back to tree
        val restoredRoot = entities.toTree()
        assertNotNull(restoredRoot)
        assertEquals(root.id, restoredRoot?.id)
        assertEquals(root.title, restoredRoot?.title)
        assertEquals(2, restoredRoot?.children?.size)
        assertEquals("2-1", restoredRoot?.children?.get(1)?.children?.get(0)?.id)
    }
}
