package com.example.myapplication.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.myapplication.NoteBlock
import com.example.myapplication.NoteContent
import com.example.myapplication.R

/**
 * 全屏便签详情编辑器 - 优化版
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
            } ?: context.getString(R.string.unknown_file)
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
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                                }
                            },
                            actions = {
                                TextButton(onClick = { onSave(content) }) {
                                    Text(stringResource(R.string.done), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        )
                        // 顶部操作工具栏
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ToolItem(Icons.Default.TextFields, stringResource(R.string.body_text)) {
                                    content = content.copy(blocks = content.blocks + NoteBlock.Text("", false))
                                }
                                ToolItem(Icons.Default.Title, stringResource(R.string.heading)) {
                                    content = content.copy(blocks = content.blocks + NoteBlock.Text("", true))
                                }
                                ToolItem(Icons.Default.AddPhotoAlternate, stringResource(R.string.image)) {
                                    imagePickerLauncher.launch("image/*")
                                }
                                ToolItem(Icons.Default.AttachFile, stringResource(R.string.file)) {
                                    filePickerLauncher.launch("*/*")
                                }
                                ToolItem(Icons.Default.CheckBox, stringResource(R.string.todo)) {
                                    content = content.copy(blocks = content.blocks + NoteBlock.Todo("", false))
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (content.blocks.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 180.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = Color.LightGray.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.click_icons_to_create),
                                color = Color.Gray.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(16.dp)) }

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
    }
}

@Composable
private fun ToolItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
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
                            placeholder = { Text(if (block.isHeading) stringResource(R.string.input_heading) else stringResource(R.string.input_body)) },
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
                                placeholder = { Text(stringResource(R.string.add_caption), fontSize = 12.sp) },
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
                                placeholder = { Text(stringResource(R.string.todo_placeholder)) },
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
                        val context = LocalContext.current
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            val uri = Uri.parse(block.uri)
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, block.mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_file_chooser)))
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, context.getString(R.string.cannot_open_file_error, e.localizedMessage), android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        block.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        block.mimeType,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.delete_node), modifier = Modifier.size(18.dp), tint = Color.Gray)
            }
        }
    }
}
