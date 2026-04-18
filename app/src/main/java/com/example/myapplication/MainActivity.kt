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
            val savedTree = database.knowledgeDao().getTree("root_tree").firstOrNull()
            val initialRoot = if (savedTree != null) {
                savedTree.jsonContent.toNode()
            } else {
                val defaultRoot = KnowledgeNode(
                    id = "root",
                    title = "所有知识",
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
                database.knowledgeDao().insertTree(KnowledgeTreeEntity(jsonContent = defaultRoot.toJson()))
                defaultRoot
            }

            setContent {
                KnowledgeAppTheme {
                    KnowledgeApp(
                        initialRoot = initialRoot,
                        onTreeChanged = { newRoot ->
                            lifecycleScope.launch {
                                database.knowledgeDao().insertTree(KnowledgeTreeEntity(jsonContent = newRoot.toJson()))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KnowledgeApp(initialRoot: KnowledgeNode, onTreeChanged: (KnowledgeNode) -> Unit) {
    var rootNode by remember { mutableStateOf(initialRoot) }
    var isEditMode by remember { mutableStateOf(false) }
    val navigationStack = remember { mutableStateListOf<KnowledgeNode>() }
    
    LaunchedEffect(Unit) {
        if (navigationStack.isEmpty()) {
            val defaultNode = findDefaultNode(rootNode)
            if (defaultNode != null && defaultNode != rootNode) {
                navigationStack.add(rootNode)
                navigationStack.add(defaultNode)
            } else {
                navigationStack.add(rootNode)
            }
        }
    }

    LaunchedEffect(rootNode) {
        onTreeChanged(rootNode)
        if (navigationStack.isNotEmpty()) {
            val newStack = mutableListOf<KnowledgeNode>()
            var current = rootNode
            newStack.add(current)
            for (i in 1 until navigationStack.size) {
                val targetId = navigationStack[i].id
                val found = current.children.find { it.id == targetId }
                if (found != null) {
                    newStack.add(found)
                    current = found
                } else break
            }
            navigationStack.clear()
            navigationStack.addAll(newStack)
        }
    }

    if (isEditMode) {
        TreeEditor(
            rootNode = rootNode,
            onClose = { isEditMode = false },
            onSave = { 
                rootNode = it
                isEditMode = false
            }
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
    rootNode: KnowledgeNode,
    onClose: () -> Unit,
    onSave: (KnowledgeNode) -> Unit
) {
    var editingTree by remember { mutableStateOf(rootNode) }
    var showExitConfirm by remember { mutableStateOf(false) }

    val hasChanges = editingTree != rootNode

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
                title = { Text("编辑层级结构", fontSize = 18.sp) },
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
                        Text("保存", fontWeight = FontWeight.Bold, color = if(hasChanges) MaterialTheme.colorScheme.primary else Color.Gray)
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
                Text("根节点：${editingTree.title}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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
