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
    private val repository = KnowledgeRepository(AppDatabase.getDatabase(application).knowledgeDao())

    val allSchemes: Flow<List<KnowledgeScheme>> = repository.allSchemes

    private val _uiState = MutableStateFlow<KnowledgeUiState>(KnowledgeUiState.Loading)
    val uiState: StateFlow<KnowledgeUiState> = _uiState

    // 撤销/重做栈
    private val undoStack = mutableListOf<KnowledgeNode>()
    private val redoStack = mutableListOf<KnowledgeNode>()

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
                    val firstScheme = schemes.first()
                    val rootNode = repository.loadRootNode(firstScheme.name) ?: firstScheme.jsonContent.toNode()
                    
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
                        val newNode = repository.loadRootNode(currentFromDb.name) ?: currentFromDb.jsonContent.toNode()
                        
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

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            saveRequestFlow
                .debounce(500)
                .collect { (name, node) ->
                    repository.saveSchemeTree(name, node)
                }
        }

        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            saveCalendarFlow
                .debounce(500)
                .collect { (name, events) ->
                    repository.saveSharedCalendar(name, events)
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
        val appContext = getApplication<Application>().applicationContext
        val defaultName = appContext.getString(R.string.default_scheme_name)
        val defaultRoot = KnowledgeNode(
            id = "root",
            title = defaultName,
            children = listOf(
                KnowledgeNode("1", appContext.getString(R.string.sample_daily), weight = 4f),
                KnowledgeNode("2", appContext.getString(R.string.sample_body_state), weight = 3f, children = listOf(
                    KnowledgeNode("2-1", appContext.getString(R.string.sample_body_time)),
                    KnowledgeNode("2-2", appContext.getString(R.string.sample_body_env)),
                    KnowledgeNode("2-3", appContext.getString(R.string.sample_body_sense))
                )),
                KnowledgeNode("3", appContext.getString(R.string.sample_idle), weight = 2f, children = listOf(
                    KnowledgeNode("3-1", appContext.getString(R.string.sample_idle_alone), nodeType = NodeType.NOTE),
                    KnowledgeNode("3-2", appContext.getString(R.string.sample_idle_group), nodeType = NodeType.NOTE)
                )),
                KnowledgeNode("4", appContext.getString(R.string.sample_notes), weight = 3f)
            )
        )
        repository.createScheme(defaultName, defaultRoot)
    }

    fun switchScheme(name: String, allSchemes: List<KnowledgeScheme>) {
        viewModelScope.launch {
            val scheme = allSchemes.find { it.name == name } ?: return@launch
            val rootNode = repository.loadRootNode(name) ?: scheme.jsonContent.toNode()
            val events = try {
                Json.decodeFromString<List<CalendarEvent>>(scheme.sharedCalendarJson)
            } catch (e: Exception) {
                emptyList<CalendarEvent>()
            }
            
            resetStack(rootNode)
            clearHistory() // 切换方案清空历史
            
            _uiState.value = KnowledgeUiState.Success(
                currentSchemeName = name,
                rootNode = rootNode,
                sharedCalendarEvents = events
            )
        }
    }

    fun updateRootNode(name: String, newNode: KnowledgeNode) {
        val currentState = _uiState.value
        if (currentState is KnowledgeUiState.Success) {
            // 保存旧状态到撤销栈
            undoStack.add(currentState.rootNode)
            if (undoStack.size > 20) undoStack.removeAt(0)
            redoStack.clear() // 新操作清空重做栈

            _uiState.value = currentState.copy(rootNode = newNode)
            syncNavigationStack(newNode)
            viewModelScope.launch {
                saveRequestFlow.emit(name to newNode)
            }
        }
    }

    fun undo() {
        val currentState = _uiState.value
        if (currentState is KnowledgeUiState.Success && undoStack.isNotEmpty()) {
            val previousNode = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(currentState.rootNode)
            
            _uiState.value = currentState.copy(rootNode = previousNode)
            syncNavigationStack(previousNode)
            viewModelScope.launch {
                saveRequestFlow.emit(currentState.currentSchemeName to previousNode)
            }
        }
    }

    fun redo() {
        val currentState = _uiState.value
        if (currentState is KnowledgeUiState.Success && redoStack.isNotEmpty()) {
            val nextNode = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(currentState.rootNode)
            
            _uiState.value = currentState.copy(rootNode = nextNode)
            syncNavigationStack(nextNode)
            viewModelScope.launch {
                saveRequestFlow.emit(currentState.currentSchemeName to nextNode)
            }
        }
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
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
            repository.deleteScheme(scheme)
            val latest = repository.getLatestScheme()
            if (latest != null) {
                val rootNode = repository.loadRootNode(latest.name) ?: latest.jsonContent.toNode()
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
            repository.createScheme(name, rootNode)
            resetStack(rootNode)
            clearHistory()
            _uiState.value = KnowledgeUiState.Success(name, rootNode, emptyList())
        }
    }


    fun renameScheme(oldName: String, newName: String, jsonContent: String) {
        viewModelScope.launch {
            val newNode = jsonContent.toNode()
            repository.renameScheme(oldName, newName, newNode)

            val oldScheme = repository.allSchemes.firstOrNull()?.find { it.name == newName }
            val events = try {
                Json.decodeFromString<List<CalendarEvent>>(oldScheme?.sharedCalendarJson ?: "[]")
            } catch (e: Exception) {
                emptyList()
            }
            
            _uiState.value = KnowledgeUiState.Success(newName, newNode, events)
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
