package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.*
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
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
    var localTitle by remember(node.id) { mutableStateOf(node.title) }
    var localContent by remember(node.id) { mutableStateOf(node.content) }

    // 防抖同步
    LaunchedEffect(localTitle, localContent) {
        if (localTitle != node.title || localContent != node.content) {
            delay(500)
            onChanged(node.copy(title = localTitle, content = localContent))
        }
    }

    val levelColor = when (level % 5) {
        1 -> Color(0xFF64B5F6)
        2 -> Color(0xFF81C784)
        3 -> Color(0xFFFFB74D)
        4 -> Color(0xFFE57373)
        else -> Color(0xFF9575CD)
    }

    val backgroundColor = if (node.nodeType == NodeType.CATEGORY) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 8).dp) // 层级缩进
            .drawBehind {
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
        // 彻底去除阴影高度，消除阴影引起的“暗边”
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
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
                val isCompact = level >= 4

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
                        var showDetailEditor by remember { mutableStateOf(false) }
                        
                        Card(
                            onClick = { showDetailEditor = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val previewText = remember(node.content) { node.getNotePreview(maxChars = 100) }
                                Text(
                                    text = previewText.ifEmpty { "点击编辑便签内容..." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (previewText.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("点击进行详细编辑", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (showDetailEditor) {
                            NoteDetailEditor(
                                title = node.title,
                                initialContent = node.toNoteContent(),
                                onSave = { updatedContent ->
                                    onChanged(node.copy(content = updatedContent.toJsonString()))
                                    showDetailEditor = false
                                },
                                onDismiss = { showDetailEditor = false }
                            )
                        }
                    }
                    NodeType.CALENDAR -> {
                        var showCalendarEditor by remember { mutableStateOf(false) }
                        val events = remember(node.content) { node.toCalendarEvents() }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = node.sharedCalendarEnabled,
                                        onCheckedChange = { onChanged(node.copy(sharedCalendarEnabled = it)) },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Text("关联全局日历", fontSize = 12.sp, color = if(node.sharedCalendarEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                                }
                                
                                if (!node.sharedCalendarEnabled) {
                                    TextButton(
                                        onClick = { showCalendarEditor = true },
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("编辑私有日程", fontSize = 11.sp)
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (node.sharedCalendarEnabled) Icons.Default.Public else Icons.Default.Event, 
                                            null, 
                                            tint = MaterialTheme.colorScheme.primary, 
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (node.sharedCalendarEnabled) "正在使用全局共享日历" else "正在使用私有日程表", 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!node.sharedCalendarEnabled) {
                                        Text("包含 ${events.size} 条私有日程。", fontSize = 11.sp, color = Color.Gray)
                                    } else {
                                        Text("所有关联节点将同步显示同一份日程清单。", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        if (showCalendarEditor) {
                            CalendarEditorDialog(
                                title = "编辑私有日程",
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
