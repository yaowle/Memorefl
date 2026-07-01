package com.example.myapplication.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.KnowledgeNode
import com.example.myapplication.KnowledgeScheme
import com.example.myapplication.LanguageManager
import com.example.myapplication.toJson
import com.example.myapplication.toNode
import com.example.myapplication.R

/**
 * 设置页面：负责方案管理（切换、新建、重命名、删除）以及数据的 JSON 导入导出。
 */
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
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    if (showImportDialog) {
        var importText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.import_scheme_title)) },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text(stringResource(R.string.paste_json_hint)) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        if (importText.isNotBlank()) {
                            onImport(importText.toNode())
                            showImportDialog = false
                            Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.confirm)) }
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(currentSchemeName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_scheme_title)) },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank() && newName != currentSchemeName) {
                        onRenameScheme(currentSchemeName, newName)
                    }
                    showRenameDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
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
            Text(stringResource(R.string.scheme_management), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            // 方案选择
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.current_scheme, currentSchemeName))
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
                    val timestamp = (System.currentTimeMillis() % 10000).toInt()
                    onNewScheme(context.getString(R.string.new_scheme_prefix, timestamp)) 
                }) { Text(stringResource(R.string.create_new)) }
                OutlinedButton(onClick = { showRenameDialog = true }) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.rename)) 
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
                        Text(stringResource(R.string.delete)) 
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.data_import_export), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clip = ClipData.newPlainText("JSON", editingTree.toJson(pretty = false))
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.copied_compact_json), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.copy_compact))
                }
                
                OutlinedButton(
                    onClick = {
                        val clip = ClipData.newPlainText("JSON", editingTree.toJson(pretty = true))
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.copied_pretty_json), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, null, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.copy_pretty))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Input, null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.import_json_data))
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(32.dp))

            // 语言切换
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            var selectedLang by remember { mutableStateOf(LanguageManager.getSavedLanguage(context)) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = selectedLang == LanguageManager.LANG_ZH,
                            onClick = {
                                if (selectedLang != LanguageManager.LANG_ZH) {
                                    selectedLang = LanguageManager.LANG_ZH
                                    LanguageManager.setLanguage(context, LanguageManager.LANG_ZH)
                                    (context as? Activity)?.recreate()
                                }
                            },
                            label = { Text(stringResource(R.string.language_chinese)) },
                            leadingIcon = if (selectedLang == LanguageManager.LANG_ZH) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedLang == LanguageManager.LANG_EN,
                            onClick = {
                                if (selectedLang != LanguageManager.LANG_EN) {
                                    selectedLang = LanguageManager.LANG_EN
                                    LanguageManager.setLanguage(context, LanguageManager.LANG_EN)
                                    (context as? Activity)?.recreate()
                                }
                            },
                            label = { Text(stringResource(R.string.language_english)) },
                            leadingIcon = if (selectedLang == LanguageManager.LANG_EN) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(32.dp))

            Text(stringResource(R.string.about_app), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MemoRefl", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.app_slogan), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.version_text, "1.0.0"), fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.app_desc), fontSize = 14.sp)
                }
            }
        }
    }
}
