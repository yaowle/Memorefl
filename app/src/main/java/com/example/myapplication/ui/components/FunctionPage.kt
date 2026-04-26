package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.CalendarEvent
import com.example.myapplication.KnowledgeNode
import com.example.myapplication.NodeType
import com.example.myapplication.R
import com.example.myapplication.toCalendarEvents
import com.example.myapplication.toJsonString
import java.text.SimpleDateFormat
import java.util.*

/**
 * 功能页内容渲染器
 */
@Composable
fun FunctionPageContent(
    node: KnowledgeNode, 
    sharedEvents: List<CalendarEvent>,
    onNodeUpdated: (KnowledgeNode) -> Unit,
    onUpdateSharedCalendar: (List<CalendarEvent>) -> Unit
) {
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
                    text = node.content.ifEmpty { stringResource(R.string.no_note_content) },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 28.sp,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                )
            }
            NodeType.CALENDAR -> {
                CalendarPageView(
                    node = node, 
                    events = if (node.sharedCalendarEnabled) sharedEvents else node.toCalendarEvents(),
                    onEventsChanged = { updatedEvents ->
                        if (node.sharedCalendarEnabled) {
                            onUpdateSharedCalendar(updatedEvents)
                        } else {
                            onNodeUpdated(node.copy(content = updatedEvents.toJsonString()))
                        }
                    },
                    isShared = node.sharedCalendarEnabled
                )
            }
            else -> {}
        }
    }
}

/**
 * 日历视图容器
 */
@Composable
fun CalendarPageView(
    node: KnowledgeNode, 
    events: List<CalendarEvent>,
    onEventsChanged: (List<CalendarEvent>) -> Unit,
    isShared: Boolean
) {
    // 维护一个每分钟更新的时间状态，确保“进行中”判断是实时的
    var currentDateTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            val nextMinute = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            kotlinx.coroutines.delay(maxOf(1, nextMinute - System.currentTimeMillis()))
            currentDateTime = Date()
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault()) }
    
    val currentTimeStr = timeFormatter.format(currentDateTime)
    
    val currentOrNext = remember(events, currentTimeStr) {
        val sorted = events.sortedBy { it.time }
        val next = sorted.find { it.time >= currentTimeStr && !it.isDone }
        val current = sorted.lastOrNull { it.time <= currentTimeStr && !it.isDone }
        Pair(current, next)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val currentTime = timeFormatter.format(currentDateTime)
        val currentDate = dateFormatter.format(currentDateTime)
        
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.calendar_summary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (isShared) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        stringResource(R.string.global_shared), 
                        fontSize = 10.sp, 
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        TodoItemView(
            label = stringResource(R.string.status_ongoing), 
            content = currentOrNext.first?.title ?: stringResource(R.string.no_ongoing_events), 
            time = currentOrNext.first?.time,
            isCurrent = true
        )
        TodoItemView(
            label = stringResource(R.string.status_next), 
            content = currentOrNext.second?.title ?: stringResource(R.string.no_more_events), 
            time = currentOrNext.second?.time,
            isCurrent = false
        )
        
        var showFullCalendar by remember { mutableStateOf(false) }
        if (showFullCalendar) {
            FullCalendarDialog(
                events = events, 
                onChanged = onEventsChanged,
                onClose = { showFullCalendar = false }
            )
        }

        OutlinedButton(
            onClick = { showFullCalendar = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CalendarToday, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.view_full_calendar, events.size))
        }
    }
}

/**
 * 完整日程列表弹窗
 */
@Composable
fun FullCalendarDialog(
    events: List<CalendarEvent>, 
    onChanged: (List<CalendarEvent>) -> Unit,
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
                    Text(stringResource(R.string.calendar_list), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                if (events.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_calendar_events), color = Color.Gray)
                    }
                } else {
                    val sortedEvents = remember(events) { events.sortedBy { it.time } }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(sortedEvents, key = { it.id }) { event ->
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
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = if (event.isDone) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (event.isDone) Color.Gray else Color.Unspecified,
                                    modifier = Modifier.weight(1f)
                                )
                                if (event.isDone) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                } else {
                                    IconButton(
                                        onClick = {
                                            val updatedEvents = events.map { e -> if (e.id == event.id) e.copy(isDone = true) else e }
                                            onChanged(updatedEvents)
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

/**
 * 单个日程项展示
 */
@Composable
fun TodoItemView(label: String, content: String, time: String? = null, isCurrent: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = alpha)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
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
