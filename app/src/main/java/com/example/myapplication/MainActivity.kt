package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

// 数据结构：节点
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

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            // 获取所有方案
            val allSchemes = database.knowledgeDao().getAllSchemes().firstOrNull() ?: emptyList()
            
            // 如果没有任何方案，创建一个默认方案
            if (allSchemes.isEmpty()) {
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
                database.knowledgeDao().insertScheme(KnowledgeScheme("默认方案", defaultRoot.toJson()))
            }

            setContent {
                KnowledgeAppTheme {
                    val schemes by database.knowledgeDao().getAllSchemes().collectAsState(initial = emptyList())
                    
                    if (schemes.isNotEmpty()) {
                        KnowledgeApp(
                            allSchemes = schemes,
                            onSchemeChanged = { name, newRoot ->
                                lifecycleScope.launch {
                                    database.knowledgeDao().insertScheme(KnowledgeScheme(name, newRoot.toJson()))
                                }
                            },
                            onDeleteScheme = { scheme ->
                                lifecycleScope.launch {
                                    database.knowledgeDao().deleteScheme(scheme)
                                }
                            },
                            onNewScheme = { name ->
                                lifecycleScope.launch {
                                    val newNode = KnowledgeNode(id = UUID.randomUUID().toString(), title = name)
                                    database.knowledgeDao().insertScheme(KnowledgeScheme(name, newNode.toJson()))
                                }
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
    allSchemes: List<KnowledgeScheme>,
    onSchemeChanged: (String, KnowledgeNode) -> Unit,
    onDeleteScheme: (KnowledgeScheme) -> Unit,
    onNewScheme: (String) -> Unit
) {
    var currentSchemeName by remember { mutableStateOf(allSchemes.first().name) }
    
    // 确保当前方案存在，如果不存在（被删了）则切回第一个
    val currentScheme = allSchemes.find { it.name == currentSchemeName } ?: allSchemes.first().also { currentSchemeName = it.name }
    
    var rootNode by remember(currentScheme.name) { mutableStateOf(currentScheme.jsonContent.toNode()) }
    var isEditMode by remember { mutableStateOf(false) }
    val navigationStack = remember { mutableStateListOf<KnowledgeNode>() }
    
    // 自动保存修改
    LaunchedEffect(rootNode) {
        if (rootNode.toJson() != currentScheme.jsonContent) {
            onSchemeChanged(currentSchemeName, rootNode)
        }
    }

    // 处理导航栈重置
    LaunchedEffect(currentSchemeName) {
        navigationStack.clear()
        val node = currentScheme.jsonContent.toNode()
        rootNode = node
        val defaultNode = findDefaultNode(node)
        if (defaultNode != null && defaultNode != node) {
            navigationStack.add(node)
            navigationStack.add(defaultNode)
        } else {
            navigationStack.add(node)
        }
    }

    if (isEditMode) {
        TreeEditor(
            currentSchemeName = currentSchemeName,
            allSchemes = allSchemes,
            onSchemeSwitch = { currentSchemeName = it },
            onNewScheme = onNewScheme,
            onDeleteScheme = onDeleteScheme,
            onSave = { 
                onSchemeChanged(currentSchemeName, it)
                isEditMode = false
            },
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
                                    onClick = { navigationStack.removeAt(navigationStack.size - 1) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBackIosNew,
                                        contentDescription = "返回",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                
                                if (navigationStack.size > 2) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = {
                                            val first = navigationStack.first()
                                            navigationStack.clear()
                                            navigationStack.add(first)
                                        },
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
                        if (currentNode.isEndPage) {
                            EndPageContent(currentNode)
                        } else {
                            WeightedTileLayout(
                                nodes = currentNode.children,
                                onNodeClick = { node ->
                                    if (node.children.isNotEmpty() || node.isEndPage) {
                                        navigationStack.add(node)
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
fun EndPageContent(node: KnowledgeNode) {
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
        Text(
            text = node.content.ifEmpty { "暂无内容" },
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 28.sp,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeEditor(
    currentSchemeName: String,
    allSchemes: List<KnowledgeScheme>,
    onSchemeSwitch: (String) -> Unit,
    onNewScheme: (String) -> Unit,
    onDeleteScheme: (KnowledgeScheme) -> Unit,
    onSave: (KnowledgeNode) -> Unit,
    onClose: () -> Unit
) {
    val currentScheme = allSchemes.find { it.name == currentSchemeName } ?: allSchemes.first()
    var editingTree by remember(currentSchemeName) { mutableStateOf(currentScheme.jsonContent.toNode()) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }

    val hasChanges = editingTree.toJson() != currentScheme.jsonContent

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

    if (showImportDialog) {
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
                        editingTree = importText.toNode()
                        showImportDialog = false
                        importText = ""
                    } catch (e: Exception) {
                        // 简单提示
                    }
                }) { Text("导入") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑方案", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showExitConfirm = true else onClose()
                    }) { Icon(Icons.Default.Close, "取消") }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(editingTree) },
                        enabled = hasChanges
                    ) {
                        Text("保存当前", fontWeight = FontWeight.Bold, color = if(hasChanges) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("方案管理", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                
                // 方案选择器
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("当前方案:", fontSize = 12.sp, modifier = Modifier.width(60.dp))
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(currentSchemeName, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    allSchemes.forEach { scheme ->
                                        DropdownMenuItem(
                                            text = { Text(scheme.name) },
                                            onClick = {
                                                onSchemeSwitch(scheme.name)
                                                expanded = false
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("+ 新建方案", color = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            onNewScheme("新方案 ${allSchemes.size + 1}")
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Row(modifier = Modifier.padding(top = 8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val jsonStr = editingTree.toJson()
                                    val clip = android.content.ClipData.newPlainText("Knowledge Scheme", jsonStr)
                                    clipboardManager.setPrimaryClip(clip)
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("导出 JSON", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("导入 JSON", fontSize = 11.sp)
                            }
                            if (allSchemes.size > 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onDeleteScheme(currentScheme) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                Text("结构编辑: ${editingTree.title}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            
            renderEditNodes(
                node = editingTree,
                level = 0,
                onUpdate = { editingTree = it }
            )
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

fun LazyListScope.renderEditNodes(
    node: KnowledgeNode,
    level: Int,
    onUpdate: (KnowledgeNode) -> Unit
) {
    if (level > 0) {
        item(key = node.id) {
            NodeEditItem(
                node = node,
                level = level,
                onChanged = { onUpdate(it) },
                onDelete = { /* 在上级处理 */ }
            )
        }
    }

    // 渲染子节点，并提供删除子节点的能力
    node.children.forEachIndexed { index, child ->
        renderEditNodes(
            node = child,
            level = level + 1,
            onUpdate = { updatedChild ->
                val newChildren = node.children.toMutableList()
                newChildren[index] = updatedChild
                onUpdate(node.copy(children = newChildren))
            }
        )
        
        // 在每个节点下方提供“删除此项”按钮
        item(key = child.id + "_del") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = (level * 24 + 16).dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        val newChildren = node.children.toMutableList()
                        newChildren.removeAt(index)
                        onUpdate(node.copy(children = newChildren))
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("删除「${child.title}」", fontSize = 10.sp)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(start = (level * 24).dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
        }
    }

    // 添加按钮
    item(key = node.id + "_add") {
        val canAdd = !node.isEndPage && ((level == 0) || node.limitDisabled || (node.children.size < 3))
        if (canAdd) {
            OutlinedButton(
                onClick = {
                    val newNode = KnowledgeNode(id = UUID.randomUUID().toString(), title = "新分类")
                    onUpdate(node.copy(children = node.children + newNode))
                },
                modifier = Modifier.padding(start = (level * 24).dp).height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("在「${node.title}」下添加", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun NodeEditItem(
    node: KnowledgeNode,
    level: Int,
    onChanged: (KnowledgeNode) -> Unit,
    onDelete: () -> Unit
) {
    // 根据层级改变卡片色调，易于辨别，并增加侧边线条标识层级
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = node.title,
                    onValueChange = { onChanged(node.copy(title = it)) },
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("权重: ${node.weight.toInt()}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(42.dp))
                    Slider(
                        value = node.weight,
                        onValueChange = { onChanged(node.copy(weight = it)) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("尾页", fontSize = 10.sp, color = if(node.isEndPage) MaterialTheme.colorScheme.primary else Color.Gray)
                    Switch(
                        checked = node.isEndPage,
                        onCheckedChange = { 
                            // 设置为尾页时清空子节点，取消尾页时清空内容
                            onChanged(node.copy(
                                isEndPage = it, 
                                children = if(it) emptyList() else node.children,
                                content = if(!it) "" else node.content
                            )) 
                        },
                        modifier = Modifier.scale(0.6f)
                    )
                }

                if (!node.isEndPage) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("解除限制", fontSize = 10.sp, color = if(node.limitDisabled) MaterialTheme.colorScheme.primary else Color.Gray)
                        Checkbox(
                            checked = node.limitDisabled,
                            onCheckedChange = { onChanged(node.copy(limitDisabled = it)) },
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }

            if (node.isEndPage) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = node.content,
                    onValueChange = { onChanged(node.copy(content = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
                    label = { Text("详细内容 (支持多行)", fontSize = 10.sp) },
                    minLines = 3
                )
            }
        }
    }
}


@Composable
fun WeightedTileLayout(nodes: List<KnowledgeNode>, onNodeClick: (KnowledgeNode) -> Unit) {
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
    val displayNodes = if (hasHeavyNode) {
        val heavyNodes = nodes.filter { it.weight > 1f }
        val lightNodes = nodes.filter { it.weight <= 1f }
        if (lightNodes.isNotEmpty()) {
            val minWeight = if (heavyNodes.isNotEmpty()) heavyNodes.minOf { it.weight } else 1f
            heavyNodes + KnowledgeNode("others", "其他", weight = minWeight, children = lightNodes)
        } else heavyNodes
    } else nodes

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
                Text(
                    text = node.title,
                    style = if (weight > 0.3f) 
                        MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp) 
                        else MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
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
