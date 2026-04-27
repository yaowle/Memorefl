package com.example.myapplication.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.myapplication.NoteBlock
import com.example.myapplication.NoteContent

/**
 * 全屏便签详情编辑器 - 优化版（工具栏移至顶部）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailEditor(
    title: String,
    initialContent: NoteContent,
    onSave: (NoteContent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf(initialContent) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val newBlocks = content.blocks + NoteBlock.Image(it.toString())
            content = content.copy(blocks = newBlocks)
        }
    }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "未知文件"
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
            val newBlocks = content.blocks + NoteBlock.File(it.toString(), fileName, mimeType)
            content = content.copy(blocks = newBlocks)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "取消")
                                }
                            },
                            actions = {
                                TextButton(onClick = { onSave(content) }) {
                                    Text("完成", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        )
                        // 顶部操作工具栏
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 2.dp,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    content = content.copy(blocks = content.blocks + NoteBlock.Text("", false))
                                }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.TextFields, contentDescription = "正文", tint = MaterialTheme.colorScheme.primary)
                                        Text("正文", fontSize = 9.sp)
                                    }
                                }
                                IconButton(onClick = {
                                    content = content.copy(blocks = content.blocks + NoteBlock.Text("", true))
                                }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Title, contentDescription = "标题", tint = MaterialTheme.colorScheme.primary)
                                        Text("标题", fontSize = 9.sp)
                                    }
                                }
                                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "图片", tint = MaterialTheme.colorScheme.primary)
                                        Text("图片", fontSize = 9.sp)
                                    }
                                }
                                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AttachFile, contentDescription = "文件", tint = MaterialTheme.colorScheme.primary)
                                        Text("文件", fontSize = 9.sp)
                                    }
                                }
                                IconButton(onClick = {
                                    content = content.copy(blocks = content.blocks + NoteBlock.Todo("", false))
                                }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CheckBox, contentDescription = "待办", tint = MaterialTheme.colorScheme.primary)
                                        Text("待办", fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    
                    if (content.blocks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(0.7f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("点击上方图标开始创作", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    itemsIndexed(content.blocks) { index, block ->
                        NoteBlockItem(
                            block = block,
                            onUpdate = { updatedBlock ->
                                val newList = content.blocks.toMutableList()
                                newList[index] = updatedBlock
                                content = content.copy(blocks = newList)
                            },
                            onDelete = {
                                val newList = content.blocks.toMutableList()
                                newList.removeAt(index)
                                content = content.copy(blocks = newList)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun NoteBlockItem(
    block: NoteBlock,
    onUpdate: (NoteBlock) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(1f).padding(8.dp)) {
                when (block) {
                    is NoteBlock.Text -> {
                        TextField(
                            value = block.text,
                            onValueChange = { onUpdate(block.copy(text = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(if (block.isHeading) "输入标题..." else "输入正文...") },
                            textStyle = if (block.isHeading)
                                MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            else MaterialTheme.typography.bodyLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                    is NoteBlock.Image -> {
                        Column {
                            AsyncImage(
                                model = block.uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                            TextField(
                                value = block.caption ?: "",
                                onValueChange = { onUpdate(block.copy(caption = it)) },
                                placeholder = { Text("添加图片说明...", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.labelSmall,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                    is NoteBlock.Todo -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = block.checked,
                                onCheckedChange = { onUpdate(block.copy(checked = it)) }
                            )
                            TextField(
                                value = block.text,
                                onValueChange = { onUpdate(block.copy(text = it)) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("待办事项...") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                    is NoteBlock.File -> {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(block.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(block.mimeType, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Clear, contentDescription = "删除", modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
    }
}
