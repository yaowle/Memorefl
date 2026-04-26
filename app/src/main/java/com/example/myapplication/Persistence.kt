package com.example.myapplication

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 节点类型枚举
 */
@Serializable
enum class NodeType {
    CATEGORY, // 分类目录
    NOTE,     // 便签内容
    CALENDAR  // 日历日程
}

/**
 * 日历事件数据类
 */
@Serializable
data class CalendarEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val time: String, // "HH:mm"
    val title: String,
    val isDone: Boolean = false
)

/**
 * 内存中的节点模型（保持递归结构方便 UI 使用）
 */
@Serializable
data class KnowledgeNode(
    val id: String,
    val title: String,
    val weight: Float = 1f,
    val children: List<KnowledgeNode> = emptyList(),
    val isDefault: Boolean = false,
    val limitDisabled: Boolean = false,
    val nodeType: NodeType = NodeType.CATEGORY,
    val content: String = "",
    val sharedCalendarEnabled: Boolean = false
)

/**
 * 方案实体：存储方案元数据
 */
@Entity(tableName = "knowledge_schemes")
data class KnowledgeScheme(
    @PrimaryKey val name: String,
    val jsonContent: String, // 暂时保留以兼容旧数据或作为备份
    val sharedCalendarJson: String = "[]",
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * 关系型节点实体：用于高性能局部更新
 */
@Entity(
    tableName = "nodes",
    foreignKeys = [
        ForeignKey(
            entity = KnowledgeScheme::class,
            parentColumns = ["name"],
            childColumns = ["schemeName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("schemeName"), Index("parentId")]
)
data class NodeEntity(
    @PrimaryKey val id: String,
    val schemeName: String,
    val parentId: String?, // 父节点 ID，根节点为 null
    val title: String,
    val weight: Float,
    val nodeType: NodeType,
    val content: String,
    val isDefault: Boolean,
    val limitDisabled: Boolean,
    val sharedCalendarEnabled: Boolean,
    val sortOrder: Int // 用于保持子节点顺序
)

/**
 * 数据库操作接口
 */
@Dao
interface KnowledgeDao {
    // 方案管理
    @Query("SELECT * FROM knowledge_schemes ORDER BY lastModified DESC")
    fun getAllSchemes(): Flow<List<KnowledgeScheme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheme(scheme: KnowledgeScheme)

    @Query("SELECT * FROM knowledge_schemes WHERE name = :name")
    suspend fun getScheme(name: String): KnowledgeScheme?

    @Query("SELECT * FROM knowledge_schemes ORDER BY lastModified DESC LIMIT 1")
    suspend fun getLatestScheme(): KnowledgeScheme?

    @Delete
    suspend fun deleteScheme(scheme: KnowledgeScheme)

    @Query("DELETE FROM knowledge_schemes WHERE name = :name")
    suspend fun deleteSchemeByName(name: String)

    // 关系型节点管理
    @Query("SELECT * FROM nodes WHERE schemeName = :schemeName ORDER BY sortOrder ASC")
    suspend fun getNodesForScheme(schemeName: String): List<NodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<NodeEntity>)

    @Query("DELETE FROM nodes WHERE schemeName = :schemeName")
    suspend fun deleteNodesByScheme(schemeName: String)
    
    @Update
    suspend fun updateNode(node: NodeEntity)

    @Transaction
    suspend fun updateSchemeTree(schemeName: String, rootNode: KnowledgeNode) {
        val scheme = getScheme(schemeName) ?: return
        deleteNodesByScheme(schemeName)
        insertNodes(rootNode.toEntities(schemeName))
        insertScheme(scheme.copy(jsonContent = rootNode.toJson(), lastModified = System.currentTimeMillis()))
    }
}

/**
 * 数据库单例配置
 */
@Database(
    entities = [KnowledgeScheme::class, NodeEntity::class], 
    version = 4, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeDao(): KnowledgeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knowledge_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- 辅助工具函数 ---

private val dbJson = Json { ignoreUnknownKeys = true }
private val exportJson = Json { ignoreUnknownKeys = true; prettyPrint = true }

fun KnowledgeNode.toJson(pretty: Boolean = false): String = 
    if (pretty) exportJson.encodeToString(this) else dbJson.encodeToString(this)

fun String.toNode(): KnowledgeNode = dbJson.decodeFromString(this)

fun KnowledgeNode.toCalendarEvents(): List<CalendarEvent> {
    if (nodeType != NodeType.CALENDAR || content.isBlank()) return emptyList()
    return try { dbJson.decodeFromString<List<CalendarEvent>>(content) } catch (e: Exception) { emptyList() }
}

fun List<CalendarEvent>.toJsonString(): String = dbJson.encodeToString(this)

/**
 * 递归将内存树转换为扁平实体列表
 */
fun KnowledgeNode.toEntities(schemeName: String, parentId: String? = null, order: Int = 0): List<NodeEntity> {
    val current = NodeEntity(
        id = id,
        schemeName = schemeName,
        parentId = parentId,
        title = title,
        weight = weight,
        nodeType = nodeType,
        content = content,
        isDefault = isDefault,
        limitDisabled = limitDisabled,
        sharedCalendarEnabled = sharedCalendarEnabled,
        sortOrder = order
    )
    val result = mutableListOf(current)
    children.forEachIndexed { index, child ->
        result.addAll(child.toEntities(schemeName, id, index))
    }
    return result
}

/**
 * 将扁平实体列表还原为树形结构
 */
fun List<NodeEntity>.toTree(): KnowledgeNode? {
    val nodeMap = this.associateBy { it.id }
    val childrenMap = this.groupBy { it.parentId }
    
    fun buildNode(entity: NodeEntity): KnowledgeNode {
        return KnowledgeNode(
            id = entity.id,
            title = entity.title,
            weight = entity.weight,
            nodeType = entity.nodeType,
            content = entity.content,
            isDefault = entity.isDefault,
            limitDisabled = entity.limitDisabled,
            sharedCalendarEnabled = entity.sharedCalendarEnabled,
            children = childrenMap[entity.id]?.sortedBy { it.sortOrder }?.map { buildNode(it) } ?: emptyList()
        )
    }
    
    // 假设根节点的 parentId 为空
    val rootEntity = this.find { it.parentId == null } ?: return null
    return buildNode(rootEntity)
}
