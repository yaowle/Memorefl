package com.example.myapplication

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class KnowledgeNode(
    val id: String,
    val title: String,
    val weight: Float = 1f,
    val children: List<KnowledgeNode> = emptyList(),
    val isDefault: Boolean = false,
    val limitDisabled: Boolean = false,
    val isEndPage: Boolean = false,
    val content: String = ""
)

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

    @Query("DELETE FROM knowledge_schemes WHERE name = :name")
    suspend fun deleteSchemeByName(name: String)

    @Query("SELECT * FROM knowledge_schemes ORDER BY lastModified DESC LIMIT 1")
    suspend fun getLatestScheme(): KnowledgeScheme?
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
