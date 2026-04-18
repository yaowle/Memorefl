package com.example.myapplication

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "knowledge_tree")
data class KnowledgeTreeEntity(
    @PrimaryKey val id: String = "root_tree",
    val jsonContent: String
)

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_tree WHERE id = :id")
    fun getTree(id: String): Flow<KnowledgeTreeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTree(tree: KnowledgeTreeEntity)
}

@Database(entities = [KnowledgeTreeEntity::class], version = 1)
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
                ).build()
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
