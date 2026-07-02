package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.myapplication.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
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
    var showColorPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 卡片可选颜色（工程冷峻风格），引用共享调色板
    val cardBackgroundColors = cardColorPalette.map { it.background }

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
                            contentDescription = stringResource(R.string.toggle_collapse),
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // 排序按钮
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowUpward, stringResource(R.string.move_up), modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ArrowDownward, stringResource(R.string.move_down), modifier = Modifier.size(16.dp), tint = Color.Gray)
                    }
                }

                // 核心改动：将添加按钮整合进卡片内
                val canAdd = node.nodeType == NodeType.CATEGORY && (node.limitDisabled || (node.children.size < 3))
                if (canAdd) {
                    TextButton(
                        onClick = {
                            val newNode = KnowledgeNode(id = UUID.randomUUID().toString(), title = context.getString(R.string.new_category))
                            onChanged(node.copy(children = listOf(newNode) + node.children))
                        },
                        modifier = Modifier.height(28.dp).padding(end = 12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.add_child), fontSize = 10.sp)
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete_node), modifier = Modifier.size(18.dp), tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
                
                // 卡片颜色选择色块
                Box(modifier = Modifier.size(24.dp).clickable { showColorPicker = true }) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.Center)
                            .background(
                                color = if (node.color != null) Color(node.color!!) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(3.dp)
                            )
                            .then(
                                if (node.color == null) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(3.dp)
                                ) else Modifier
                            )
                    )
                }
                
                // 颜色选择弹窗
                if (showColorPicker) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        onDismissRequest = { showColorPicker = false }
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(R.string.card_color),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                // 颜色网格：每行 4 个
                                cardBackgroundColors.chunked(4).forEach { row ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        row.forEach { colorArg ->
                                            val isSelected = node.color == colorArg
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clickable {
                                                        onChanged(node.copy(color = colorArg))
                                                        showColorPicker = false
                                                    }
                                                    .then(
                                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                                                    )
                                                    .padding(3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (colorArg == null) {
                                                    // 默认色：显示斜线纹理
                                                    Icon(
                                                        Icons.Default.Block,
                                                        contentDescription = stringResource(R.string.default_color),
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .background(Color(colorArg), CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                    label = { Text(stringResource(R.string.node_name), fontSize = 10.sp) },
                    singleLine = true
                )

                IconButton(onClick = { onChanged(node.copy(isDefault = !node.isDefault)) }) {
                    Icon(
                        if (node.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(R.string.default_entry),
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
                                    1 -> stringResource(R.string.weight_very_low)
                                    2 -> stringResource(R.string.weight_low)
                                    3 -> stringResource(R.string.weight_medium)
                                    4 -> stringResource(R.string.weight_high)
                                    5 -> stringResource(R.string.weight_very_high)
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
                            text = if (node.nodeType == NodeType.CATEGORY) stringResource(R.string.set_function_page) else stringResource(R.string.function_page_enabled),
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
                            Text(stringResource(R.string.remove_limit), fontSize = 10.sp, color = if(node.limitDisabled) MaterialTheme.colorScheme.primary else Color.Gray)
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
                        label = { Text(stringResource(R.string.type_note), fontSize = 11.sp) },
                        leadingIcon = if (node.nodeType == NodeType.NOTE) {
                            { Icon(Icons.Default.Note, null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                    // 日历类型入口已隐藏（功能暂关闭）
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
                                    text = previewText.ifEmpty { stringResource(R.string.click_to_edit_note) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (previewText.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EditNote, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.click_to_edit_detail), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
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
                                    Text(stringResource(R.string.link_global_calendar), fontSize = 12.sp, color = if(node.sharedCalendarEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                                }
                                
                                if (!node.sharedCalendarEnabled) {
                                    TextButton(
                                        onClick = { showCalendarEditor = true },
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.edit_private_events), fontSize = 11.sp)
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
                                            if (node.sharedCalendarEnabled) stringResource(R.string.using_global_calendar) else stringResource(R.string.using_private_calendar), 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!node.sharedCalendarEnabled) {
                                        Text(stringResource(R.string.private_events_count, events.size), fontSize = 11.sp, color = Color.Gray)
                                    } else {
                                        Text(stringResource(R.string.sync_same_calendar), fontSize = 11.sp, color = Color.Gray)
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
