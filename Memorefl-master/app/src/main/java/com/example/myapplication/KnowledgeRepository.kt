package com.example.myapplication

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/**
 * 数据仓库：负责业务模型 (KnowledgeNode) 与 数据库实体 (NodeEntity/KnowledgeScheme) 之间的转换
 */
class KnowledgeRepository(private val dao: KnowledgeDao) {

    val allSchemes: Flow<List<KnowledgeScheme>> = dao.getAllSchemes()

    suspend fun loadRootNode(schemeName: String): KnowledgeNode? {
        val nodes = dao.getNodesForScheme(schemeName)
        if (nodes.isEmpty()) {
            return dao.getScheme(schemeName)?.jsonContent?.toNode()
        }
        return nodes.toTree()
    }

    suspend fun saveSchemeTree(schemeName: String, rootNode: KnowledgeNode) {
        dao.updateSchemeTree(schemeName, rootNode)
    }

    suspend fun saveSharedCalendar(schemeName: String, events: List<CalendarEvent>) {
        val scheme = dao.getScheme(schemeName)
        if (scheme != null) {
            dao.insertScheme(scheme.copy(sharedCalendarJson = events.toJsonString(), lastModified = System.currentTimeMillis()))
        }
    }

    suspend fun createScheme(name: String, rootNode: KnowledgeNode) {
        dao.insertScheme(KnowledgeScheme(name, rootNode.toJson()))
        dao.updateSchemeTree(name, rootNode)
    }

    suspend fun deleteScheme(scheme: KnowledgeScheme) {
        dao.deleteScheme(scheme)
    }

    suspend fun renameScheme(oldName: String, newName: String, rootNode: KnowledgeNode) {
        val oldScheme = dao.getScheme(oldName)
        val sharedJson = oldScheme?.sharedCalendarJson ?: "[]"
        
        dao.deleteSchemeByName(oldName)
        dao.insertScheme(KnowledgeScheme(newName, rootNode.toJson(), sharedJson))
        dao.updateSchemeTree(newName, rootNode)
    }

    suspend fun getLatestScheme(): KnowledgeScheme? = dao.getLatestScheme()
}
