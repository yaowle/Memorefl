package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.CalendarEvent
import com.example.myapplication.KnowledgeNode
import com.example.myapplication.TreeLogic
import java.util.UUID

/**
 * 树结构编辑器：允许用户可视化编辑节点树（添加、删除、重命名节点，调整权重和类型）。
 * 支持递归缩进展示，并具备退出前未保存更改的确认提醒。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeEditor(
    currentSchemeName: String,
    editingTree: KnowledgeNode,
    sharedCalendarEvents: List<CalendarEvent>,
    onSave: (KnowledgeNode) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onUpdateSharedCalendar: (List<CalendarEvent>) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    var currentEditingTree by remember(editingTree) { mutableStateOf(editingTree) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showGlobalCalendarEditor by remember { mutableStateOf(false) }

    // 本地撤销/重做栈（用于未保存前的实时编辑）
    val localUndoStack = remember { mutableStateListOf<KnowledgeNode>() }
    val localRedoStack = remember { mutableStateListOf<KnowledgeNode>() }

    // 辅助函数：更新树并记录历史
    val updateTreeWithHistory: (KnowledgeNode) -> Unit = { newNode ->
        if (newNode != currentEditingTree) {
            localUndoStack.add(currentEditingTree)
            if (localUndoStack.size > 50) localUndoStack.removeAt(0)
            localRedoStack.clear()
            currentEditingTree = newNode
        }
    }

    val handleUndo = {
        if (localUndoStack.isNotEmpty()) {
            val last = localUndoStack.removeAt(localUndoStack.size - 1)
            localRedoStack.add(currentEditingTree)
            currentEditingTree = last
        } else {
            onUndo() // 如果本地没得撤销了，尝试调用全局撤销
        }
    }

    val handleRedo = {
        if (localRedoStack.isNotEmpty()) {
            val next = localRedoStack.removeAt(localRedoStack.size - 1)
            localUndoStack.add(currentEditingTree)
            currentEditingTree = next
        } else {
            onRedo()
        }
    }

    val flatNodes = remember(currentEditingTree) { 
        TreeLogic.flattenTree(currentEditingTree) 
    }
    
    var collapsedIds by remember { mutableStateOf(setOf<String>()) }

    val visibleNodes = remember(flatNodes, collapsedIds) {
        flatNodes.filter { item -> item.path.none { collapsedIds.contains(it) } }
    }

    val hasChanges = remember(currentEditingTree, editingTree) {
        currentEditingTree != editingTree
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("确认退出？") },
            text = { Text("您有未保存的更改，退出将丢失这些更改。") },
            confirmButton = {
                TextButton(onClick = { onClose() }) { Text("确认退出", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("继续编辑") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenSettings() }
                    ) {
                        Text(currentSchemeName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Settings, "设置", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showExitConfirm = true else onClose()
                    }) { Icon(Icons.Default.Close, "取消") }
                },
                actions = {
                    IconButton(
                        onClick = handleUndo,
                        enabled = localUndoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo, 
                            "撤销",
                            tint = if (localUndoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(
                        onClick = handleRedo,
                        enabled = localRedoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo, 
                            "重做",
                            tint = if (localRedoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(onClick = { showGlobalCalendarEditor = true }) {
                        Icon(Icons.Default.CalendarMonth, "全局日历", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (hasChanges) {
                        TextButton(
                            onClick = { onSave(currentEditingTree) }
                        ) {
                            Text("保存更改", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Check, "完成", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (showGlobalCalendarEditor) {
            CalendarEditorDialog(
                title = "编辑全局共享日历",
                initialEvents = sharedCalendarEvents,
                onSave = {
                    onUpdateSharedCalendar(it)
                    showGlobalCalendarEditor = false
                },
                onDismiss = { showGlobalCalendarEditor = false }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            items(
                items = visibleNodes,
                key = { it.node.id }
            ) { item ->
                if (item.level == 0) {
                    OutlinedButton(
                        onClick = {
                            val newNode = KnowledgeNode(id = UUID.randomUUID().toString(), title = "新分类")
                            updateTreeWithHistory(currentEditingTree.copy(children = currentEditingTree.children + newNode))
                        },
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Text(" 添加根分类")
                    }
                } else {
                    NodeEditItem(
                        node = item.node,
                        level = item.level,
                        isCollapsed = collapsedIds.contains(item.node.id),
                        onToggleCollapse = { 
                            collapsedIds = if (collapsedIds.contains(item.node.id)) collapsedIds - item.node.id else collapsedIds + item.node.id 
                        },
                        onChanged = { updatedNode ->
                            updateTreeWithHistory(TreeLogic.updateNodeInTree(currentEditingTree, updatedNode))
                        },
                        onDelete = {
                            updateTreeWithHistory(TreeLogic.deleteNodeFromTree(currentEditingTree, item.node.id))
                        },
                        onMoveUp = if (!item.isFirst) { { updateTreeWithHistory(TreeLogic.moveNodeInTree(currentEditingTree, item.node.id, true)) } } else null,
                        onMoveDown = if (!item.isLast) { { updateTreeWithHistory(TreeLogic.moveNodeInTree(currentEditingTree, item.node.id, false)) } } else null
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
