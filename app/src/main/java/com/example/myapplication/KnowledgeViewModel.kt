package com.example.myapplication

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

class KnowledgeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).knowledgeDao()

    val allSchemes: Flow<List<KnowledgeScheme>> = dao.getAllSchemes()

    private val _uiState = MutableStateFlow<KnowledgeUiState>(KnowledgeUiState.Loading)
    val uiState: StateFlow<KnowledgeUiState> = _uiState

    // 内部使用的防抖保存 Flow (Node树)
    private val saveRequestFlow = MutableSharedFlow<Pair<String, KnowledgeNode>>(replay = 0)
    // 内部使用的防抖保存 Flow (共享日历)
    private val saveCalendarFlow = MutableSharedFlow<Pair<String, List<CalendarEvent>>>(replay = 0)

    // 导航栈管理
    private val _navigationStack = mutableStateListOf<KnowledgeNode>()
    val navigationStack: List<KnowledgeNode> = _navigationStack

    fun pushNode(node: KnowledgeNode) {
        _navigationStack.add(node)
    }

    fun popNode() {
        if (_navigationStack.size > 1) {
            _navigationStack.removeAt(_navigationStack.size - 1)
        }
    }

    fun resetStack(rootNode: KnowledgeNode) {
        _navigationStack.clear()
        _navigationStack.add(rootNode)
        val defaultNode = TreeLogic.findDefaultNode(rootNode)
        if (defaultNode != null && defaultNode != rootNode) {
            _navigationStack.add(defaultNode)
        }
    }

    init {
        viewModelScope.launch {
            allSchemes.collect { schemes ->
                if (schemes.isEmpty()) {
                    createDefaultScheme()
                    return@collect
                }

                val currentState = _uiState.value
                if (currentState is KnowledgeUiState.Loading) {
                    // 第一次加载：取最新修改的方案
                    val firstScheme = schemes.first()
                    // 尝试从关系表中加载
                    val nodes = dao.getNodesForScheme(firstScheme.name)
                    val rootNode = nodes.toTree() ?: firstScheme.jsonContent.toNode()
                    
                    val events = try {
                        Json.decodeFromString<List<CalendarEvent>>(firstScheme.sharedCalendarJson)
                    } catch (e: Exception) {
                        emptyList<CalendarEvent>()
                    }
                    _uiState.value = KnowledgeUiState.Success(
                        currentSchemeName = firstScheme.name,
                        rootNode = rootNode,
                        sharedCalendarEvents = events
                    )
                    resetStack(rootNode)
                } else if (currentState is KnowledgeUiState.Success) {
                    val currentFromDb = schemes.find { it.name == currentState.currentSchemeName }
                    if (currentFromDb != null) {
                        val nodes = dao.getNodesForScheme(currentFromDb.name)
                        val newNode = nodes.toTree() ?: currentFromDb.jsonContent.toNode()
                        
                        val newEvents = try {
                            Json.decodeFromString<List<CalendarEvent>>(currentFromDb.sharedCalendarJson)
                        } catch (e: Exception) {
                            emptyList<CalendarEvent>()
                        }
                        if (newNode != currentState.rootNode || newEvents != currentState.sharedCalendarEvents) {
                            _uiState.value = currentState.copy(rootNode = newNode, sharedCalendarEvents = newEvents)
                            syncNavigationStack(newNode)
                        }
                    }
                }
            }
        }

        // 实现防抖保存：500ms 内没有新更改才写入数据库
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            saveRequestFlow
                .debounce(500)
                .collect { (name, node) ->
                    dao.updateSchemeTree(name, node)
                }
        }

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            saveCalendarFlow
                .debounce(500)
                .collect { (name, events) ->
                    val scheme = dao.getScheme(name)
                    if (scheme != null) {
                        dao.insertScheme(scheme.copy(sharedCalendarJson = events.toJsonString()))
                    }
                }
        }
    }

    fun updateSharedCalendar(name: String, newEvents: List<CalendarEvent>) {
        val currentState = _uiState.value
        if (currentState is KnowledgeUiState.Success) {
            _uiState.value = currentState.copy(sharedCalendarEvents = newEvents)
            viewModelScope.launch {
                saveCalendarFlow.emit(name to newEvents)
            }
        }
    }

    private suspend fun createDefaultScheme() {
        val defaultRoot = KnowledgeNode(
            id = "root",
            title = "默认方案",
            children = listOf(
                KnowledgeNode("1", "工作", weight = 3f, children = listOf(
                    KnowledgeNode("1-1", "会议"),
                    KnowledgeNode("1-2", "文档")
                )),
                KnowledgeNode("2", "生活", weight = 2f, children = listOf(
                    KnowledgeNode("2-1", "运动"),
                    KnowledgeNode("2-2", "娱乐")
                ))
            )
        )
        dao.insertScheme(KnowledgeScheme("默认方案", defaultRoot.toJson()))
        dao.updateSchemeTree("默认方案", defaultRoot)
    }

    fun switchScheme(name: String, allSchemes: List<KnowledgeScheme>) {
        viewModelScope.launch {
            val scheme = allSchemes.find { it.name == name } ?: return@launch
            val nodes = dao.getNodesForScheme(name)
            val rootNode = nodes.toTree() ?: scheme.jsonContent.toNode()
            val events = try {
                Json.decodeFromString<List<CalendarEvent>>(scheme.sharedCalendarJson)
            } catch (e: Exception) {
                emptyList<CalendarEvent>()
            }
            
            // 关键修复 1：先重置导航栈，确保栈中是新方案的节点
            resetStack(rootNode)
            
            _uiState.value = KnowledgeUiState.Success(
                currentSchemeName = name,
                rootNode = rootNode,
                sharedCalendarEvents = events
            )
            
            // 关键修复 2：切换方案时更新时间戳，确保下次启动时能正确恢复
            dao.insertScheme(scheme.copy(lastModified = System.currentTimeMillis()))
        }
    }

    fun updateRootNode(name: String, newNode: KnowledgeNode) {
        val currentState = _uiState.value
        if (currentState is KnowledgeUiState.Success) {
            _uiState.value = currentState.copy(rootNode = newNode)
            // 关键修复：同步更新导航栈中的节点，防止显示旧快照
            syncNavigationStack(newNode)
            viewModelScope.launch {
                saveRequestFlow.emit(name to newNode)
            }
        }
    }

    private fun syncNavigationStack(root: KnowledgeNode) {
        for (i in _navigationStack.indices) {
            val oldNode = _navigationStack[i]
            val newNode = TreeLogic.findNodeById(root, oldNode.id)
            if (newNode != null) {
                _navigationStack[i] = newNode
            }
        }
    }

    fun deleteScheme(scheme: KnowledgeScheme) {
        viewModelScope.launch {
            dao.deleteScheme(scheme)
            // 删除后尝试获取最新的一个方案
            val latest = dao.getLatestScheme()
            if (latest != null) {
                val nodes = dao.getNodesForScheme(latest.name)
                val rootNode = nodes.toTree() ?: latest.jsonContent.toNode()
                val events = try {
                    Json.decodeFromString<List<CalendarEvent>>(latest.sharedCalendarJson)
                } catch (e: Exception) {
                    emptyList()
                }
                _uiState.value = KnowledgeUiState.Success(latest.name, rootNode, events)
                resetStack(rootNode)
            } else {
                createDefaultScheme()
            }
        }
    }

    fun createNewScheme(name: String) {
        createNewSchemeFromNode(name, KnowledgeNode(id = UUID.randomUUID().toString(), title = name))
    }

    fun createNewSchemeFromNode(name: String, rootNode: KnowledgeNode) {
        viewModelScope.launch {
            dao.insertScheme(KnowledgeScheme(name, rootNode.toJson()))
            dao.updateSchemeTree(name, rootNode)
            
            // 修复：重置导航栈并更新 UI 状态
            resetStack(rootNode)
            _uiState.value = KnowledgeUiState.Success(name, rootNode, emptyList())
        }
    }


    fun renameScheme(oldName: String, newName: String, jsonContent: String) {
        viewModelScope.launch {
            val oldScheme = dao.getScheme(oldName)
            val sharedJson = oldScheme?.sharedCalendarJson ?: "[]"
            
            // 先删除旧的，再插入新的（避免主键冲突且名称改变）
            dao.deleteSchemeByName(oldName)
            val newScheme = KnowledgeScheme(newName, jsonContent, sharedJson)
            dao.insertScheme(newScheme)
            
            val newNode = jsonContent.toNode()
            dao.updateSchemeTree(newName, newNode)

            val events = try {
                Json.decodeFromString<List<CalendarEvent>>(sharedJson)
            } catch (e: Exception) {
                emptyList()
            }
            
            _uiState.value = KnowledgeUiState.Success(newName, newNode, events)
            // 修复：重构后也同步下导航栈，确保根节点引用正确
            syncNavigationStack(newNode)
        }
    }
}

sealed class KnowledgeUiState {
    object Loading : KnowledgeUiState()
    data class Success(
        val currentSchemeName: String,
        val rootNode: KnowledgeNode,
        val sharedCalendarEvents: List<CalendarEvent> = emptyList()
    ) : KnowledgeUiState()
}
