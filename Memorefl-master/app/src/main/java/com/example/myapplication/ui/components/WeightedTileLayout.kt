package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.CalendarEvent
import com.example.myapplication.KnowledgeNode
import com.example.myapplication.NodeType
import com.example.myapplication.getNotePreview
import com.example.myapplication.toCalendarEvents
import java.text.SimpleDateFormat
import java.util.*

/**
 * 权重瓦片布局：根据节点的权重（Weight）动态分配屏幕比例，展示磁贴风格的导航界面。
 * 支持根据节点类型（如日历、便签）预览内容快照。
 */
@Composable
fun WeightedTileLayout(
    nodes: List<KnowledgeNode>, 
    isRootLevel: Boolean = false,
    sharedEvents: List<CalendarEvent> = emptyList(),
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
    val shouldMerge = !isRootLevel && hasHeavyNode && nodes.size >= 4

    val displayNodes = if (shouldMerge) {
        val sortedNodes = nodes.sortedByDescending { it.weight }
        val topNodes = sortedNodes.filter { it.weight > 1f }.take(2)
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
                NodeTileContent(node, weight, sharedEvents)
            }
        }
    }
}

@Composable
fun NodeTileContent(node: KnowledgeNode, weight: Float, sharedEvents: List<CalendarEvent>) {
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
            val events = if (node.sharedCalendarEnabled) sharedEvents else remember(node.content) { node.toCalendarEvents() }
            val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val activeEvents = events.filter { !it.isDone }.sortedBy { it.time }
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
                            imageVector = if (node.sharedCalendarEnabled) Icons.Default.Public else Icons.Default.Event,
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
        } else if (node.nodeType == NodeType.NOTE) {
            val previewText = remember(node.content) { node.getNotePreview() }
            if (previewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = if (weight > 0.3f) 4 else 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
