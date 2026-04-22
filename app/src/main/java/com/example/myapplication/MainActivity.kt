package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.window.Dialog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: KnowledgeViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val allSchemes by viewModel.allSchemes.collectAsState(initial = emptyList())

            KnowledgeAppTheme {
                when (val state = uiState) {
                    is KnowledgeUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is KnowledgeUiState.Success -> {
                        KnowledgeApp(
                            state = state,
                            allSchemes = allSchemes,
                            navigationStack = viewModel.navigationStack,
                            onPushNode = { viewModel.pushNode(it) },
                            onPopNode = { viewModel.popNode() },
                            onResetStack = { viewModel.resetStack(it) },
                            onNodeUpdated = { newNode -> 
                                viewModel.updateRootNode(state.currentSchemeName, newNode) 
                            },
                            onSchemeSwitch = { name -> 
                                viewModel.switchScheme(name, allSchemes) 
                            },
                            onDeleteScheme = { viewModel.deleteScheme(it) },
                            onNewScheme = { viewModel.createNewScheme(it) },
                            onRenameScheme = { old, new -> 
                                viewModel.renameScheme(old, new, state.rootNode.toJson()) 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgeApp(
    state: KnowledgeUiState.Success,
    allSchemes: List<KnowledgeScheme>,
    navigationStack: List<KnowledgeNode>,
    onPushNode: (KnowledgeNode) -> Unit,
    onPopNode: () -> Unit,
    onResetStack: (KnowledgeNode) -> Unit,
    onNodeUpdated: (KnowledgeNode) -> Unit,
    onSchemeSwitch: (String) -> Unit,
    onDeleteScheme: (KnowledgeScheme) -> Unit,
    onNewScheme: (String) -> Unit,
    onRenameScheme: (String, String) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var isSettingsMode by remember { mutableStateOf(false) }
    
    val rootNode = state.rootNode
    val currentSchemeName = state.currentSchemeName

    if (isSettingsMode) {
        SettingsPage(
            currentSchemeName = currentSchemeName,
            allSchemes = allSchemes,
            onSchemeSwitch = { 
                onSchemeSwitch(it)
                isSettingsMode = false 
            },
            onNewScheme = onNewScheme,
            onDeleteScheme = onDeleteScheme,
            onRenameScheme = onRenameScheme,
            editingTree = rootNode,
            onImport = { 
                onNodeUpdated(it)
                isSettingsMode = false
            },
            onClose = { isSettingsMode = false }
        )
    } else if (isEditMode) {
        TreeEditor(
            currentSchemeName = currentSchemeName,
            editingTree = rootNode,
            onSave = { 
                onNodeUpdated(it)
                isEditMode = false
            },
            onOpenSettings = { isSettingsMode = true },
            onClose = { isEditMode = false }
        )
    } else if (navigationStack.isNotEmpty()) {
        val currentNode = navigationStack.last()
        Scaffold(
            // 移除了悬浮按钮，将其整合进顶栏
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // 融入画面的顶部导航（整合返回、首页与编辑）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (navigationStack.size > 1) {
                                IconButton(
                                    onClick = onPopNode,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                
                                if (navigationStack.size > 2) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { onResetStack(navigationStack.first()) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = "首页",
                                            modifier = Modifier.size(26.dp),
                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        // 右上角编辑标志
                        IconButton(
                            onClick = { isEditMode = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "编辑",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (currentNode.nodeType != NodeType.CATEGORY) {
                            FunctionPageContent(currentNode, onNodeUpdated)
                        } else {
                            WeightedTileLayout(
                                nodes = currentNode.children,
                                isRootLevel = navigationStack.size <= 1,
                                onNodeClick = { node ->
                                    if (node.children.isNotEmpty() || node.nodeType != NodeType.CATEGORY) {
                                        onPushNode(node)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun findDefaultNode(node: KnowledgeNode): KnowledgeNode? {
    if (node.isDefault) return node
    for (child in node.children) {
        val found = findDefaultNode(child)
        if (found != null) return found
    }
    return null
}

@Composable
fun FunctionPageContent(node: KnowledgeNode, onNodeUpdated: (KnowledgeNode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Text(
            text = node.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        when (node.nodeType) {
            NodeType.NOTE -> {
                Text(
                    text = node.content.ifEmpty { "暂无便签内容" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                )
            }
            NodeType.CALENDAR -> {
                CalendarPageView(node, onNodeUpdated)
            }
            else -> {}
        }
    }
}

@Composable
fun CalendarPageView(node: KnowledgeNode, onNodeUpdated: (KnowledgeNode) -> Unit) {
    val events = remember(node.content) { node.toCalendarEvents() }
    val currentTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
    
    // 查找当前和下一项
    val currentOrNext = remember(events, currentTimeStr) {
        val sorted = events.sortedBy { it.time }
        val next = sorted.find { it.time >= currentTimeStr && !it.isDone }
        val current = sorted.lastOrNull { it.time <= currentTimeStr && !it.isDone }
        Pair(current, next)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 当前时间显示
        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val currentDate = java.text.SimpleDateFormat("MM月dd日 EEEE", java.util.Locale.getDefault()).format(java.util.Date())
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = currentDate, style = MaterialTheme.typography.titleMedium)
                    Text(text = currentTime, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }

        // 待办事项部分
        Text("日程摘要", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        TodoItemView(
            label = "进行中", 
            content = currentOrNext.first?.title ?: "暂无进行中事项", 
            time = currentOrNext.first?.time,
            isCurrent = true
        )
        TodoItemView(
            label = "下一项", 
            content = currentOrNext.second?.title ?: "今日已无后续安排", 
            time = currentOrNext.second?.time,
            isCurrent = false
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        var showFullCalendar by remember { mutableStateOf(false) }
        if (showFullCalendar) {
            FullCalendarDialog(
                node = node,
                events = events, 
                onChanged = onNodeUpdated,
                onClose = { showFullCalendar = false }
            )
        }

        OutlinedButton(
            onClick = { showFullCalendar = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CalendarToday, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("查看完整日程表 (${events.size})")
        }
    }
}

@Composable
fun FullCalendarDialog(
    node: KnowledgeNode,
    events: List<CalendarEvent>, 
    onChanged: (KnowledgeNode) -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("今日日程", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                if (events.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("暂无日程安排", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(events.sortedBy { it.time }) { event ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = event.time,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (event.isDone) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                } else {
                                    IconButton(
                                        onClick = {
                                            val updatedEvents = events.map { e -> if (e.id == event.id) e.copy(isDone = true) else e }
                                            onChanged(node.copy(content = updatedEvents.toJsonString()))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.alpha(0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItemView(label: String, content: String, time: String? = null, isCurrent: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = label, 
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray
                )
                if (time != null) {
                    Text(text = time, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeEditor(
    currentSchemeName: String,
    editingTree: KnowledgeNode,
    onSave: (KnowledgeNode) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    var currentEditingTree by remember(editingTree) { mutableStateOf(editingTree) }
    var showExitConfirm by remember { mutableStateOf(false) }
    
    // 性能优化：将树展平，避免递归渲染带来的 LazyColumn 性能问题
    val flatNodes = remember(currentEditingTree) { 
        flattenTree(currentEditingTree) 
    }
    
    var collapsedIds by remember { mutableStateOf(setOf<String>()) }

    // 过滤可见节点
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
                    // 根节点添加按钮
                    OutlinedButton(
                        onClick = {
                            val newNode = KnowledgeNode(id = UUID.randomUUID().toString(), title = "新分类")
                            currentEditingTree = currentEditingTree.copy(children = currentEditingTree.children + newNode)
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
                            currentEditingTree = updateNodeInTree(currentEditingTree, updatedNode)
                        },
                        onDelete = {
                            currentEditingTree = deleteNodeFromTree(currentEditingTree, item.node.id)
                        },
                        onMoveUp = if (!item.isFirst) { { currentEditingTree = moveNodeInTree(currentEditingTree, item.node.id, true) } } else null,
                        onMoveDown = if (!item.isLast) { { currentEditingTree = moveNodeInTree(currentEditingTree, item.node.id, false) } } else null
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// 展平后的节点包装类
data class FlatNode(
    val node: KnowledgeNode,
    val level: Int,
    val path: List<String>, // 记录父节点路径，用于判断是否可见
    val isFirst: Boolean,
    val isLast: Boolean
)

fun flattenTree(root: KnowledgeNode): List<FlatNode> {
    val result = mutableListOf<FlatNode>()
    result.add(FlatNode(root, 0, emptyList(), isFirst = true, isLast = true)) 
    
    fun traverse(node: KnowledgeNode, level: Int, path: List<String>) {
        node.children.forEachIndexed { index, child ->
            result.add(FlatNode(child, level, path, index == 0, index == node.children.size - 1))
            traverse(child, level + 1, path + child.id)
        }
    }
    traverse(root, 1, emptyList())
    return result
}

fun updateNodeInTree(root: KnowledgeNode, updatedNode: KnowledgeNode): KnowledgeNode {
    if (root.id == updatedNode.id) return updatedNode
    return root.copy(children = root.children.map { updateNodeInTree(it, updatedNode) })
}

fun deleteNodeFromTree(root: KnowledgeNode, targetId: String): KnowledgeNode {
    return root.copy(children = root.children.filter { it.id != targetId }.map { deleteNodeFromTree(it, targetId) })
}

fun moveNodeInTree(root: KnowledgeNode, targetId: String, up: Boolean): KnowledgeNode {
    val index = root.children.indexOfFirst { it.id == targetId }
    if (index != -1) {
        val newChildren = root.children.toMutableList()
        if (up && index > 0) {
            val tmp = newChildren[index]
            newChildren[index] = newChildren[index - 1]
            newChildren[index - 1] = tmp
            return root.copy(children = newChildren)
        } else if (!up && index < newChildren.size - 1) {
            val tmp = newChildren[index]
            newChildren[index] = newChildren[index + 1]
            newChildren[index + 1] = tmp
            return root.copy(children = newChildren)
        }
    }
    return root.copy(children = root.children.map { moveNodeInTree(it, targetId, up) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    currentSchemeName: String,
    allSchemes: List<KnowledgeScheme>,
    onSchemeSwitch: (String) -> Unit,
    onNewScheme: (String) -> Unit,
    onDeleteScheme: (KnowledgeScheme) -> Unit,
    onRenameScheme: (String, String) -> Unit,
    editingTree: KnowledgeNode,
    onImport: (KnowledgeNode) -> Unit,
    onClose: () -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }

    if (showImportDialog) {
        var importText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入方案 (JSON)") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("请在此粘贴 JSON 内容") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        if (importText.isNotBlank()) {
                            onImport(importText.toNode())
                            showImportDialog = false
                            android.widget.Toast.makeText(context, "导入成功", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "导入失败: 格式错误", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text("确认导入") }
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(currentSchemeName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名方案") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank() && newName != currentSchemeName) {
                        onRenameScheme(currentSchemeName, newName)
                    }
                    showRenameDialog = false
                }) { Text("确定") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("方案切换与管理", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            // 方案选择
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("当前: $currentSchemeName")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(
                    expanded = expanded, 
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    allSchemes.forEach { scheme ->
                        DropdownMenuItem(
                            text = { Text(scheme.name) }, 
                            onClick = { onSchemeSwitch(scheme.name); expanded = false },
                            leadingIcon = { Icon(Icons.Default.Book, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { 
                    // 解决新建方案重名导致无法新建的问题：使用时间戳或自增序号确保名称唯一
                    val timestamp = System.currentTimeMillis() % 10000
                    onNewScheme("新方案 $timestamp") 
                }) { 
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text(" 新建") 
                }
                OutlinedButton(onClick = { showRenameDialog = true }) { 
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Text(" 重命名") 
                }
                if (allSchemes.size > 1) {
                    OutlinedButton(
                        onClick = { 
                            onDeleteScheme(allSchemes.find { it.name == currentSchemeName }!!)
                            onClose()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) { 
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Text(" 删除") 
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("数据导入导出", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clip = android.content.ClipData.newPlainText("JSON", editingTree.toJson(pretty = false))
                        clipboardManager.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "已复制压缩版 JSON", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Text(" 复制(压缩)")
                }
                
                OutlinedButton(
                    onClick = {
                        val clip = android.content.ClipData.newPlainText("JSON", editingTree.toJson(pretty = true))
                        clipboardManager.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "已复制美化版 JSON", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, null, modifier = Modifier.size(16.dp))
                    Text(" 复制(美化)")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Input, null, modifier = Modifier.size(16.dp))
                Text(" 导入 JSON 数据")
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(32.dp))

            Text("关于软件", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("状态知识查询", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("极简主义 · 状态导向 · 知识树", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("版本: 1.0.0", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("一款基于树状结构的极简状态管理与知识查询工具。旨在帮助用户通过加权 Tile 布局直观地梳理和查询复杂的状态路径。", fontSize = 14.sp)
                }
            }
        }
    }
}



@Composable
fun NodeEditItem(
    node: KnowledgeNode,
    level: Int,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onChanged: (KnowledgeNode) -> Unit,
    onDelete: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    // 标题输入防抖
    var localTitle by remember(node.id) { mutableStateOf(node.title) }
    LaunchedEffect(node.title) {
        if (localTitle != node.title) localTitle = node.title
    }
    LaunchedEffect(localTitle) {
        if (localTitle != node.title) {
            delay(500)
            onChanged(node.copy(title = localTitle))
        }
    }

    // 内容输入防抖
    var localContent by remember(node.id) { mutableStateOf(node.content) }
    LaunchedEffect(node.content) {
        if (localContent != node.content) localContent = node.content
    }
    LaunchedEffect(localContent) {
        if (localContent != node.content) {
            delay(500)
            onChanged(node.copy(content = localContent))
        }
    }

    // 根据层级改变卡片色调
    val backgroundColor = if (level % 2 == 0) Color(0xFFFDFDFD) else Color.White
    val levelColor = when(level % 4) {
        1 -> Color(0xFF8DA47E)
        2 -> Color(0xFFC4C1A0)
        3 -> Color(0xFFD4E2D4)
        else -> Color(0xFFF3E9DD)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ((level - 1) * 16).dp)
            .drawBehind {
                // 绘制层级指示线
                val strokeWidth = 2.dp.toPx()
                val x = -4.dp.toPx()
                drawLine(
                    color = levelColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 顶部信息：层级数字与收起切换
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                // 层级小数字
                Surface(
                    color = levelColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "L$level",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // 收起/展开图标
                if (node.children.isNotEmpty()) {
                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (isCollapsed) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                            contentDescription = "切换收起",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // 排序按钮
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowUpward, "上移", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowDownward, "下移", modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }

                // 核心改动：将添加按钮整合进卡片内
                val canAdd = node.nodeType == NodeType.CATEGORY && (node.limitDisabled || (node.children.size < 3))
                if (canAdd) {
                    TextButton(
                        onClick = {
                            val newNode = KnowledgeNode(id = UUID.randomUUID().toString(), title = "新分类")
                            onChanged(node.copy(children = listOf(newNode) + node.children))
                        },
                        modifier = Modifier.height(28.dp).padding(end = 12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Text("添加子项", fontSize = 10.sp)
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, "删除", modifier = Modifier.size(18.dp), tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
                
                if (node.isDefault) {
                    Icon(Icons.Default.Star, "默认", tint = Color(0xFFFBC02D), modifier = Modifier.size(16.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = localTitle,
                    onValueChange = { localTitle = it },
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    label = { Text("名称", fontSize = 10.sp) },
                    singleLine = true
                )

                IconButton(onClick = { onChanged(node.copy(isDefault = !node.isDefault)) }) {
                    Icon(
                        if (node.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "默认进入",
                        tint = if (node.isDefault) Color(0xFFFBC02D) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isCompact = level >= 6

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!isCompact) {
                        Text("权重: ", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(32.dp))
                    }
                    
                    if (isCompact) {
                        // 紧凑模式：输入框
                        var weightText by remember(node.weight) { mutableStateOf(node.weight.toInt().toString()) }
                        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                        
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { input: String ->
                                if (input.isEmpty()) {
                                    weightText = ""
                                    return@OutlinedTextField
                                }
                                val filtered = input.filter { it.isDigit() }
                                if (filtered.isEmpty()) return@OutlinedTextField
                                val num = filtered.toInt()
                                val correctedNum = when {
                                    num < 1 -> 1
                                    num > 5 -> 5
                                    else -> num
                                }
                                weightText = correctedNum.toString()
                                onChanged(node.copy(weight = correctedNum.toFloat()))
                            },
                            modifier = Modifier
                                .width(64.dp)
                                .height(50.dp)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) weightText = "" 
                                    else if (weightText.isEmpty()) weightText = node.weight.toInt().toString()
                                },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp, 
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            label = { 
                                val weightLabel = when(node.weight.toInt()) {
                                    1 -> "极低"
                                    2 -> "低"
                                    3 -> "中"
                                    4 -> "高"
                                    5 -> "极高"
                                    else -> "权"
                                }
                                Text(weightLabel, fontSize = 9.sp) 
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )
                    } else {
                        // 普通模式：滑动条
                        Slider(
                            value = node.weight,
                            onValueChange = { onChanged(node.copy(weight = it)) },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.weight(1f)
                        )
                        Text(node.weight.toInt().toString(), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 0.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isCompact) {
                        Text(
                            text = if (node.nodeType == NodeType.CATEGORY) "设为功能页" else "功能页已开启",
                            fontSize = 10.sp,
                            color = if (node.nodeType != NodeType.CATEGORY) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    Switch(
                        checked = node.nodeType != NodeType.CATEGORY,
                        onCheckedChange = { isFunctionPage ->
                            onChanged(node.copy(
                                nodeType = if (isFunctionPage) NodeType.NOTE else NodeType.CATEGORY,
                                children = if (isFunctionPage) emptyList() else node.children,
                                content = if (!isFunctionPage) "" else node.content
                            ))
                        },
                        modifier = Modifier.scale(0.6f)
                    )
                }

                if (node.nodeType == NodeType.CATEGORY) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isCompact) {
                            Text("解除限制", fontSize = 10.sp, color = if(node.limitDisabled) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                        Checkbox(
                            checked = node.limitDisabled,
                            onCheckedChange = { onChanged(node.copy(limitDisabled = it)) },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }

            // 功能选择与内容编辑区
            if (node.nodeType != NodeType.CATEGORY) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // 功能类型选择 (便签/日历)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = node.nodeType == NodeType.NOTE,
                        onClick = { onChanged(node.copy(nodeType = NodeType.NOTE)) },
                        label = { Text("便签", fontSize = 11.sp) },
                        leadingIcon = if (node.nodeType == NodeType.NOTE) {
                            { Icon(Icons.Default.Note, null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = node.nodeType == NodeType.CALENDAR,
                        onClick = { onChanged(node.copy(nodeType = NodeType.CALENDAR)) },
                        label = { Text("日历", fontSize = 11.sp) },
                        leadingIcon = if (node.nodeType == NodeType.CALENDAR) {
                            { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (node.nodeType) {
                    NodeType.NOTE -> {
                        OutlinedTextField(
                            value = localContent,
                            onValueChange = { localContent = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
                            label = { Text("便签内容", fontSize = 10.sp) },
                            minLines = 3
                        )
                    }
                    NodeType.CALENDAR -> {
                        var showCalendarEditor by remember { mutableStateOf(false) }
                        val events = remember(node.content) { node.toCalendarEvents() }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("日历功能已启用", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("包含 ${events.size} 条日程安排。", fontSize = 11.sp, color = Color.Gray)
                                
                                TextButton(
                                    onClick = { showCalendarEditor = true },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("编辑日程内容", fontSize = 11.sp)
                                }
                            }
                        }

                        if (showCalendarEditor) {
                            CalendarEditorDialog(
                                initialEvents = events,
                                onSave = { updatedEvents ->
                                    onChanged(node.copy(content = updatedEvents.toJsonString()))
                                    showCalendarEditor = false
                                },
                                onDismiss = { showCalendarEditor = false }
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}


@Composable
fun CalendarEditorDialog(
    initialEvents: List<CalendarEvent>,
    onSave: (List<CalendarEvent>) -> Unit,
    onDismiss: () -> Unit
) {
    var events by remember { mutableStateOf(initialEvents) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("日程编辑器", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(events) { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var time by remember { mutableStateOf(event.time) }
                            var title by remember { mutableStateOf(event.title) }
                            
                            OutlinedTextField(
                                value = time,
                                onValueChange = { 
                                    time = it
                                    events = events.map { e -> if (e.id == event.id) e.copy(time = it) else e }
                                },
                                modifier = Modifier.width(80.dp),
                                label = { Text("时间") },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = title,
                                onValueChange = { 
                                    title = it
                                    events = events.map { e -> if (e.id == event.id) e.copy(title = it) else e }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("事项") },
                                singleLine = true
                            )
                            IconButton(onClick = { events = events.filter { it.id != event.id } }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                    }
                    
                    item {
                        TextButton(
                            onClick = { 
                                events = events + CalendarEvent(time = "08:00", title = "新事项")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Text("添加新日程")
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onSave(events) }) { Text("确认保存") }
                }
            }
        }
    }
}


@Composable
fun NodeTileContent(node: KnowledgeNode, weight: Float) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = node.title,
            style = if (weight > 0.3f) 
                MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp) 
                else MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )

        if (node.nodeType == NodeType.CALENDAR) {
            val events = remember(node.content) { node.toCalendarEvents() }
            val currentTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val activeEvents = events.filter { !it.isDone }.sortedBy { it.time }
            // 优先显示即将到来的，如果没有则显示第一个未完成的
            val nextEvent = activeEvents.find { it.time >= currentTimeStr } ?: activeEvents.firstOrNull()
            
            if (nextEvent != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${nextEvent.time} ${nextEvent.title}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "今日任务已全部完成 ✓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        } else if (node.nodeType == NodeType.NOTE && node.content.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = node.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = if (weight > 0.3f) 3 else 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WeightedTileLayout(
    nodes: List<KnowledgeNode>, 
    isRootLevel: Boolean = false,
    onNodeClick: (KnowledgeNode) -> Unit
) {
    if (nodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "已到达知识终点",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.surfaceVariant
    )

    val hasHeavyNode = nodes.any { it.weight > 1f }
    
    // 只有在非根层级、存在高权重节点、且总项数 >= 4 时，才执行“其他”合并逻辑
    val shouldMerge = !isRootLevel && hasHeavyNode && nodes.size >= 4

    val displayNodes = if (shouldMerge) {
        // 按权重降序排序
        val sortedNodes = nodes.sortedByDescending { it.weight }
        
        // 前两个最高权重的节点（必须大于 1f 才算“高权重项”）
        val topNodes = sortedNodes.filter { it.weight > 1f }.take(2)
        
        // 剩余所有节点
        val remainingNodes = sortedNodes.filter { it !in topNodes }
        
        if (remainingNodes.isNotEmpty()) {
            topNodes + KnowledgeNode(
                id = "others", 
                title = "其他", 
                weight = 1f, 
                children = remainingNodes
            )
        } else {
            topNodes
        }
    } else {
        nodes
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val totalWeight = displayNodes.sumOf { it.weight.toDouble() }.toFloat()
        displayNodes.forEachIndexed { index, node ->
            val weight = node.weight / totalWeight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight)
                    .clip(RoundedCornerShape(40.dp))
                    .background(colors[index % colors.size])
                    .clickable { onNodeClick(node) },
                contentAlignment = Alignment.Center
            ) {
                NodeTileContent(node, weight)
            }
        }
    }
}

@Composable
fun KnowledgeAppTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFD0E8D1),
            onPrimary = Color(0xFF1B3721),
            primaryContainer = Color(0xFF324E37),
            onPrimaryContainer = Color(0xFFD0E8D1),
            secondary = Color(0xFFE2E3D2),
            background = Color(0xFF141412),
            surface = Color(0xFF1C1C17),
            onBackground = Color(0xFFE6E2D9),
            onSurfaceVariant = Color(0xFFEEEEEE)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF8DA47E),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD4E2D4),
            onPrimaryContainer = Color(0xFF1B3721),
            secondary = Color(0xFFC4C1A0),
            secondaryContainer = Color(0xFFF3E9DD),
            tertiaryContainer = Color(0xFFE1F2FB),
            surfaceVariant = Color(0xFFFAF3E0),
            background = Color(0xFFFDFDF9),
            onBackground = Color(0xFF1C1C17),
            onSurfaceVariant = Color(0xFF333333),
            outline = Color(0xFFAAAAAA)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineLarge = androidx.compose.ui.text.TextStyle(
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            ),
            headlineMedium = androidx.compose.ui.text.TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        ),
        content = content
    )
}
