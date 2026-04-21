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

    init {
        viewModelScope.launch {
            allSchemes.collect { schemes ->
                if (schemes.isEmpty()) {
                    createDefaultScheme()
                } else {
                    if (_uiState.value is KnowledgeUiState.Loading) {
                        _uiState.value = KnowledgeUiState.Success(
                            currentSchemeName = schemes.first().name,
                            rootNode = schemes.first().jsonContent.toNode()
                        )
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
        _uiState.value = KnowledgeUiState.Success(
            currentSchemeName = name,
            rootNode = scheme.jsonContent.toNode()
        )
    }

    fun updateRootNode(name: String, newNode: KnowledgeNode) {
        val currentState = _uiState.value
        if (currentState is KnowledgeUiState.Success) {
            _uiState.value = currentState.copy(rootNode = newNode)
            viewModelScope.launch {
                saveRequestFlow.emit(name to newNode)
            }
        }
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
