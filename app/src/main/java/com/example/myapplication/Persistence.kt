package com.example.myapplication

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class NodeType {
    CATEGORY, // 普通分类目录
    NOTE,     // 便签页（原尾页）
    CALENDAR  // 日历页
}

@Serializable
data class CalendarEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val time: String, // 格式 "HH:mm"
    val title: String,
    val isDone: Boolean = false
)

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
    val sharedCalendarEnabled: Boolean = false // 新增：是否关联全局共享日历
)

@Entity(tableName = "knowledge_schemes")
data class KnowledgeScheme(
    @PrimaryKey val name: String,
    val jsonContent: String,
    val sharedCalendarJson: String = "[]", // 新增：每个方案独立的全局共享日历
    val lastModified: Long = System.currentTimeMillis()
)

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_schemes ORDER BY lastModified DESC")
    fun getAllSchemes(): Flow<List<KnowledgeScheme>>

    @Query("SELECT * FROM knowledge_schemes WHERE name = :name")
    suspend fun getScheme(name: String): KnowledgeScheme?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheme(scheme: KnowledgeScheme)

    @Delete
    suspend fun deleteScheme(scheme: KnowledgeScheme)

    @Query("DELETE FROM knowledge_schemes WHERE name = :name")
    suspend fun deleteSchemeByName(name: String)

    @Query("SELECT * FROM knowledge_schemes ORDER BY lastModified DESC LIMIT 1")
    suspend fun getLatestScheme(): KnowledgeScheme?
}

@Database(entities = [KnowledgeScheme::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeDao(): KnowledgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "knowledge_database"
                )
                .fallbackToDestructiveMigration() // 为简化演示，使用破坏性迁移
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

private val dbJson = Json { 
    ignoreUnknownKeys = true 
}

private val exportJson = Json { 
    ignoreUnknownKeys = true 
    prettyPrint = true
}

fun KnowledgeNode.toJson(pretty: Boolean = false): String = 
    if (pretty) exportJson.encodeToString(this) else dbJson.encodeToString(this)

fun String.toNode(): KnowledgeNode = dbJson.decodeFromString(this)

// 日历数据转换扩展
fun KnowledgeNode.toCalendarEvents(): List<CalendarEvent> {
    if (nodeType != NodeType.CALENDAR || content.isBlank()) return emptyList()
    return try {
        dbJson.decodeFromString<List<CalendarEvent>>(content)
    } catch (e: Exception) {
        emptyList()
    }
}

fun List<CalendarEvent>.toJsonString(): String = dbJson.encodeToString(this)
