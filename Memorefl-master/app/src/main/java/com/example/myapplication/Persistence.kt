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
 * 便签内容结构化模型：支持文本、图片、待办、文件
 */
@Serializable
sealed class NoteBlock {
    @Serializable
    data class Text(val text: String, val isHeading: Boolean = false) : NoteBlock()
    @Serializable
    data class Image(val uri: String, val caption: String? = null) : NoteBlock()
    @Serializable
    data class Todo(val text: String, val checked: Boolean) : NoteBlock()
    @Serializable
    data class File(val uri: String, val fileName: String, val mimeType: String) : NoteBlock()
}

@Serializable
data class NoteContent(
    val blocks: List<NoteBlock> = emptyList()
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
    val content: String = "", // 这里存储序列化后的 JSON (NoteContent 或 List<CalendarEvent>)
    val sharedCalendarEnabled: Boolean = false
)

/**
 * ==================================================================================
 * 🔴 数据库架构变更指南 (给 AI 和开发者的特别提醒):
 * 1. 如果你修改了下方任何 [Entity] 类的字段（增加、删除、重命名）：
 *    - 必须前往 [AppDatabase] 类，将 version 增加 1 (例如从 4 改为 5)。
 *    - 在 autoMigrations 列表中添加一个新的 AutoMigration 项。
 * 2. 所有的 KnowledgeNode 结构都会被序列化为 JSON 存储在 KnowledgeScheme 中作为备份。
 * ==================================================================================
 */

/**
 * 方案实体：存储方案元数据
 */
@Entity(tableName = "knowledge_schemes")
data class KnowledgeScheme(
    @PrimaryKey val name: String,
    val jsonContent: String, // 核心备份：即使 nodes 表结构损坏，也可以通过此字段恢复整个树
    val sharedCalendarJson: String = "[]",
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * 关系型节点实体：用于高性能局部查询和更新
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
    val parentId: String?,
    val title: String,
    val weight: Float,
    val nodeType: NodeType,
    val content: String,
    val isDefault: Boolean,
    val limitDisabled: Boolean,
    val sharedCalendarEnabled: Boolean,
    val sortOrder: Int
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

    @Query("SELECT id FROM nodes WHERE schemeName = :schemeName")
    suspend fun getNodeIdsForScheme(schemeName: String): List<String>

    @Query("DELETE FROM nodes WHERE id IN (:ids)")
    suspend fun deleteNodesByIds(ids: List<String>)

    @Query("DELETE FROM nodes WHERE schemeName = :schemeName")
    suspend fun deleteNodesByScheme(schemeName: String)
    
    @Update
    suspend fun updateNode(node: NodeEntity)

    @Transaction
    suspend fun updateSchemeTree(schemeName: String, rootNode: KnowledgeNode) {
        val scheme = getScheme(schemeName) ?: return
        
        // 1. 获取新树的所有实体
        val newEntities = rootNode.toEntities(schemeName)
        val newIds = newEntities.map { it.id }.toSet()

        // 2. 找出需要删除的节点（在 DB 中存在但不在新树中）
        val oldIds = getNodeIdsForScheme(schemeName)
        val idsToDelete = oldIds.filter { it !in newIds }

        // 3. 执行增量操作
        if (idsToDelete.isNotEmpty()) {
            deleteNodesByIds(idsToDelete)
        }
        
        // Room 的 REPLACE 策略会自动处理 Update 和 Insert
        insertNodes(newEntities)

        // 4. 更新方案元数据
        insertScheme(scheme.copy(
            jsonContent = rootNode.toJson(), 
            lastModified = System.currentTimeMillis()
        ))
    }
}

/**
 * 数据库单例配置
 */
@Database(
    entities = [KnowledgeScheme::class, NodeEntity::class], 
    version = 4, 
    // 自动迁移配置：当你把 version 改成 5 时，在此处添加 AutoMigration(from = 4, to = 5)
    autoMigrations = [
        // AutoMigration (from = 4, to = 5) 
    ],
    exportSchema = true
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
                // 如果自动迁移失败，允许破坏性迁移作为最后的保底（数据会丢失，但程序不崩溃）
                // 建议在开发阶段开启，发布上线前关闭
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
 * 将内容解析为结构化便签
 */
fun KnowledgeNode.toNoteContent(): NoteContent {
    if (nodeType != NodeType.NOTE || content.isBlank()) return NoteContent()
    return try {
        dbJson.decodeFromString<NoteContent>(content)
    } catch (e: Exception) {
        // 兼容旧版本的纯文本数据
        NoteContent(blocks = listOf(NoteBlock.Text(content)))
    }
}

/**
 * 提取便签内容的纯文本摘要用于预览
 */
fun KnowledgeNode.getNotePreview(maxChars: Int = 100): String {
    val note = this.toNoteContent()
    if (note.blocks.isEmpty()) return ""
    
    val sb = StringBuilder()
    for (block in note.blocks) {
        when (block) {
            is NoteBlock.Text -> sb.append(block.text)
            is NoteBlock.Todo -> sb.append("[ ] ${block.text}")
            is NoteBlock.Image -> sb.append("[图片]")
            is NoteBlock.File -> sb.append("[文件: ${block.fileName}]")
        }
        sb.append(" ")
        if (sb.length > maxChars) break
    }
    return sb.toString().trim()
}

fun NoteContent.toJsonString(): String = dbJson.encodeToString(this)

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
