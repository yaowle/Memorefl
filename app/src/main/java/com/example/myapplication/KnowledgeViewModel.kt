package com.example.myapplication

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class KnowledgeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).knowledgeDao()

    val allSchemes: Flow<List<KnowledgeScheme>> = dao.getAllSchemes()

    private val _uiState = MutableStateFlow<KnowledgeUiState>(KnowledgeUiState.Loading)
    val uiState: StateFlow<KnowledgeUiState> = _uiState

    // 内部使用的防抖保存 Flow
    private val saveRequestFlow = MutableSharedFlow<Pair<String, KnowledgeNode>>(replay = 0)

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
        val defaultNode = findDefaultNode(rootNode)
        if (defaultNode != null && defaultNode != rootNode) {
            _navigationStack.add(defaultNode)
        }
    }

    private fun findDefaultNode(node: KnowledgeNode): KnowledgeNode? {
        if (node.isDefault) return node
        for (child in node.children) {
            val found = findDefaultNode(child)
            if (found != null) return found
        }
        return null
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
                    val rootNode = firstScheme.jsonContent.toNode()
                    _uiState.value = KnowledgeUiState.Success(
                        currentSchemeName = firstScheme.name,
                        rootNode = rootNode
                    )
                    resetStack(rootNode)
                } else if (currentState is KnowledgeUiState.Success) {
                    // 已经在运行中：如果数据库中的当前方案内容变了（比如导入、外部修改），同步更新 UI
                    val currentFromDb = schemes.find { it.name == currentState.currentSchemeName }
                    if (currentFromDb != null) {
                        val newNode = currentFromDb.jsonContent.toNode()
                        if (newNode != currentState.rootNode) {
                            _uiState.value = currentState.copy(rootNode = newNode)
                            // 同步更新导航栈
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
                    dao.insertScheme(KnowledgeScheme(name, node.toJson()))
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
    }

    fun switchScheme(name: String, allSchemes: List<KnowledgeScheme>) {
        val scheme = allSchemes.find { it.name == name } ?: return
        val rootNode = scheme.jsonContent.toNode()
        _uiState.value = KnowledgeUiState.Success(
            currentSchemeName = name,
            rootNode = rootNode
        )
        resetStack(rootNode)
        
        // 关键修复：切换方案时更新时间戳，确保下次启动时能正确恢复
        viewModelScope.launch {
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
            val newNode = findNodeById(root, oldNode.id)
            if (newNode != null) {
                _navigationStack[i] = newNode
            }
        }
    }

    private fun findNodeById(root: KnowledgeNode, id: String): KnowledgeNode? {
        if (root.id == id) return root
        for (child in root.children) {
            val found = findNodeById(child, id)
            if (found != null) return found
        }
        return null
    }

    fun deleteScheme(scheme: KnowledgeScheme) {
        viewModelScope.launch {
            dao.deleteScheme(scheme)
            // 删除后尝试获取最新的一个方案
            val latest = dao.getLatestScheme()
            if (latest != null) {
                _uiState.value = KnowledgeUiState.Success(latest.name, latest.jsonContent.toNode())
            } else {
                createDefaultScheme()
            }
        }
    }

    fun createNewScheme(name: String) {
        viewModelScope.launch {
            val newNode = KnowledgeNode(id = UUID.randomUUID().toString(), title = name)
            dao.insertScheme(KnowledgeScheme(name, newNode.toJson()))
            // 创建后立即切换
            _uiState.value = KnowledgeUiState.Success(name, newNode)
        }
    }


    fun renameScheme(oldName: String, newName: String, jsonContent: String) {
        viewModelScope.launch {
            // 先删除旧的，再插入新的（避免主键冲突且名称改变）
            dao.deleteSchemeByName(oldName)
            val newScheme = KnowledgeScheme(newName, jsonContent)
            dao.insertScheme(newScheme)
            _uiState.value = KnowledgeUiState.Success(newName, jsonContent.toNode())
        }
    }
}

sealed class KnowledgeUiState {
    object Loading : KnowledgeUiState()
    data class Success(
        val currentSchemeName: String,
        val rootNode: KnowledgeNode
    ) : KnowledgeUiState()
}
