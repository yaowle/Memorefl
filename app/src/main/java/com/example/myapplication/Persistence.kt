package com.example.myapplication

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "knowledge_schemes")
data class KnowledgeScheme(
    @PrimaryKey val name: String,
    val jsonContent: String,
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
}

@Database(entities = [KnowledgeScheme::class], version = 2, exportSchema = false)
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

private val json = Json { 
    ignoreUnknownKeys = true 
    prettyPrint = true
}

fun KnowledgeNode.toJson(): String = json.encodeToString(this)
fun String.toNode(): KnowledgeNode = json.decodeFromString(this)

fun KnowledgeNode.toCsv(): String {
    val sb = StringBuilder()
    // CSV Header
    sb.append("层级,ID,标题,权重,是否终点,内容\n")
    fun traverse(node: KnowledgeNode, level: Int) {
        val escapedContent = node.content.replace("\"", "\"\"")
        sb.append("${level},${node.id},${node.title},${node.weight.toInt()},${if (node.isEndPage) "是" else "否"},\"$escapedContent\"\n")
        node.children.forEach { traverse(it, level + 1) }
    }
    traverse(this, 0)
    return sb.toString()
}
