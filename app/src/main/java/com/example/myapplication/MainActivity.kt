package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.KnowledgeAppTheme
import com.example.myapplication.ui.components.*

/**
 * 主入口 Activity
 */
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            newBase?.let { LanguageManager.apply(LanguageManager.getSavedLanguage(it), it) }
        )
    }

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
                            onNewSchemeFromNode = { name, node -> 
                                viewModel.createNewSchemeFromNode(name, node) 
                            },
                            onRenameScheme = { old, new -> 
                                viewModel.renameScheme(old, new, state.rootNode.toJson()) 
                            },
                            onUndo = { viewModel.undo() },
                            onRedo = { viewModel.redo() },
                            sharedCalendarEvents = state.sharedCalendarEvents,
                            onUpdateSharedCalendar = { events ->
                                viewModel.updateSharedCalendar(state.currentSchemeName, events)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 主应用容器，管理导航状态与页面切换
 */
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
    onNewSchemeFromNode: (String, KnowledgeNode) -> Unit,
    onRenameScheme: (String, String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    sharedCalendarEvents: List<CalendarEvent>,
    onUpdateSharedCalendar: (List<CalendarEvent>) -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var isSettingsMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
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
            onImport = { importedNode ->
                // 修复：导入时创建一个新方案，而不是覆盖当前方案
                val timestamp = (System.currentTimeMillis() % 1000).toInt()
                                    val newSchemeName = context.getString(R.string.import_scheme_prefix, timestamp)
                onNewSchemeFromNode(newSchemeName, importedNode)
                isSettingsMode = false
            },
            onClose = { isSettingsMode = false }
        )
    } else if (isEditMode) {
        TreeEditor(
            currentSchemeName = currentSchemeName,
            editingTree = rootNode,
            sharedCalendarEvents = sharedCalendarEvents,
            onSave = { 
                onNodeUpdated(it)
                isEditMode = false
            },
            onUndo = onUndo,
            onRedo = onRedo,
            onUpdateSharedCalendar = onUpdateSharedCalendar,
            onOpenSettings = { isSettingsMode = true },
            onClose = { isEditMode = false }
        )
    } else if (navigationStack.isNotEmpty()) {
        val currentNode = navigationStack.last()
        Scaffold { innerPadding ->
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
                    // 顶部状态栏与导航
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
                                        contentDescription = stringResource(R.string.back),
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
                                            contentDescription = stringResource(R.string.home),
                                            modifier = Modifier.size(26.dp),
                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isEditMode = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { isSettingsMode = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // 页面内容区域
                    Box(modifier = Modifier.weight(1f)) {
                        if (currentNode.nodeType != NodeType.CATEGORY) {
                            FunctionPageContent(
                                node = currentNode, 
                                sharedEvents = sharedCalendarEvents,
                                onNodeUpdated = onNodeUpdated,
                                onUpdateSharedCalendar = onUpdateSharedCalendar
                            )
                        } else {
                            WeightedTileLayout(
                                nodes = currentNode.children,
                                isRootLevel = navigationStack.size <= 1,
                                sharedEvents = sharedCalendarEvents,
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
