# i18n 多语言改造记录

> 日期：2026-06-30
> 目的：消除硬编码中文字符串，建立多语言支持框架（中文/英文）

---

## 一、新增文件

### 1. `app/src/main/res/values-en/strings.xml`
- 新建英文字符串资源文件
- 包含所有新增 `strings.xml` 中定义的 key 的英文翻译
- 系统会根据设备语言自动切换 `values/` 与 `values-en/`

---

## 二、修改文件

### 2. `app/src/main/res/values/strings.xml`
新增以下字符串资源 key（原均为硬编码中文）：

| Key | 中文值 | 用途 |
|-----|--------|------|
| `default_scheme_name` | 默认方案 | ViewModel 默认方案名 |
| `import_scheme_prefix` | 导入方案_%s | 导入方案命名 |
| `new_scheme_prefix` | 新方案_%s | 新建方案命名 |
| `import_scheme_title` | 导入方案 | 导入对话框标题 |
| `paste_json_hint` | 请粘贴 JSON 数据 | 导入提示 |
| `import_success` | 导入成功 | 导入成功 Toast |
| `import_failed` | 导入失败 | 导入失败 Toast |
| `rename_scheme_title` | 重命名方案 | 重命名对话框标题 |
| `confirm` | 确认 | 确认按钮 |
| `settings` | 设置 | 设置标题 |
| `scheme_management` | 方案管理 | 方案管理区 |
| `current_scheme` | 当前方案 | 当前方案标签 |
| `create_new` | 新建 | 新建按钮 |
| `rename` | 重命名 | 重命名按钮 |
| `delete` | 删除 | 删除按钮 |
| `data_import_export` | 数据导入/导出 | 数据区标题 |
| `copy_compact` | 复制（紧凑） | 复制紧凑 JSON |
| `copy_pretty` | 复制（格式化） | 复制格式化 JSON |
| `copied_compact_json` | 已复制紧凑 JSON | 复制成功提示 |
| `copied_pretty_json` | 已复制格式化 JSON | 复制成功提示 |
| `import_json_data` | 导入 JSON 数据 | 导入按钮 |
| `about_app` | 关于应用 | 关于区标题 |
| `app_slogan` | 记忆反射 | 应用标语 |
| `version_text` | 版本：%s | 版本号显示 |
| `app_desc` | 一款基于知识树… | 应用描述 |
| `total_schemes` | 共 %d 个方案 | 方案计数 |
| `confirm_exit_title` | 确认退出 | 退出确认标题 |
| `unsaved_changes` | 有未保存的修改… | 未保存提示 |
| `confirm_exit` | 确认退出 | 确认退出按钮 |
| `continue_editing` | 继续编辑 | 继续编辑按钮 |
| `edit_global_calendar` | 编辑全局日历 | 全局日历按钮 |
| `add_root_category` | 添加根分类 | 添加根分类 |
| `new_category` | 新分类 | 默认分类名 |
| `undo` | 撤销 | 撤销按钮 |
| `redo` | 重做 | 重做按钮 |
| `save_changes` | 保存修改 | 保存按钮 |
| `cancel` | 取消 | 取消按钮 |
| `done` | 完成 | 完成按钮 |
| `tree_editor_title` | 知识树编辑器 | 编辑器标题 |
| `node_name` | 节点名称 | 节点名称标签 |
| `default_entry` | 默认入口 | 默认入口标签 |
| `set_function_page` | 设为功能页 | 功能页开关 |
| `function_page_enabled` | 功能页已启用 | 功能页提示 |
| `remove_limit` | 移除上限 | 移除权重上限 |
| `type_note` | 笔记 | 类型标签 |
| `type_calendar` | 日历 | 类型标签 |
| `click_to_edit_note` | 点击编辑笔记 | 占位提示 |
| `click_to_edit_detail` | 点击编辑详情 | 占位提示 |
| `link_global_calendar` | 关联全局日历 | 关联开关 |
| `edit_private_schedule` | 编辑私有日程 | 私有日程按钮 |
| `using_global_calendar` | 使用全局日历 | 状态提示 |
| `using_private_calendar` | 使用私有日历 | 状态提示 |
| `private_events_count` | 私有事件数：%d | 事件计数 |
| `sync_same_calendar` | 同步到同一日历 | 同步提示 |
| `edit_private_events` | 编辑私有事件 | 编辑按钮 |
| `weight_very_low` | 极低 | 权重标签 |
| `weight_low` | 低 | 权重标签 |
| `weight_medium` | 中 | 权重标签 |
| `weight_high` | 高 | 权重标签 |
| `weight_very_high` | 极高 | 权重标签 |
| `add_child` | 添加子节点 | 添加子节点 |
| `toggle_collapse` | 展开/折叠 | 折叠按钮 |
| `move_up` | 上移 | 上移按钮 |
| `move_down` | 下移 | 下移按钮 |
| `delete_node` | 删除节点 | 删除按钮 |
| `click_to_edit_detail_page` | 点击进入详情页编辑 | 占位提示 |
| `date_format_pattern` | MM月dd日 EEEE | 日期格式（中文） |
| `cannot_open_file` | 无法打开此文件 | 文件打开失败 |
| `cannot_open_file_error` | 无法打开文件：%s | 错误详情 |
| `open_file_chooser` | 打开文件选择器 | 文件选择 |
| `body_text` | 正文 | 正文按钮 |
| `heading` | 标题 | 标题按钮 |
| `image` | 图片 | 图片按钮 |
| `file` | 文件 | 文件按钮 |
| `todo` | 待办 | 待办按钮 |
| `click_icons_to_create` | 点击图标创建内容 | 提示文本 |
| `input_heading` | 输入标题… | 标题占位符 |
| `input_body` | 输入正文… | 正文占位符 |
| `add_caption` | 添加说明文字 | 图片说明 |
| `todo_placeholder` | 输入待办事项… | 待办占位符 |
| `calendar_editor` | 日历编辑器 | 日历编辑器标题 |
| `time` | 时间 | 时间标签 |
| `event` | 事件 | 事件标签 |
| `add_event` | 添加事件 | 添加事件 |
| `new_event_time` | 新事件时间 | 时间输入提示 |
| `new_event_title` | 新事件标题 | 标题输入提示 |
| `knowledge_end` | 知识End | 权重布局末尾文本 |
| `others` | 其他 | 其他分类 |
| `all_tasks_done` | 今日目标已全部完成！ | 全部完成提示 |
| `sample_work` | 工作 | 示例数据 |
| `sample_meeting` | 会议 | 示例数据 |
| `sample_doc` | 文档 | 示例数据 |
| `sample_life` | 生活 | 示例数据 |
| `sample_sport` | 运动 | 示例数据 |
| `sample_entertainment` | 娱乐 | 示例数据 |

---

### 3. `MainActivity.kt`
- 添加 `import androidx.compose.ui.res.stringResource`
- `"返回"` → `stringResource(R.string.back)`
- `"首页"` → `stringResource(R.string.home)`
- `"编辑"` → `stringResource(R.string.edit)`
- 导入方案命名改用 `stringResource(R.string.import_scheme_prefix, timestamp)`

### 4. `KnowledgeViewModel.kt`
- 添加 `import com.example.myapplication.R`
- `"默认方案"` → `appContext.getString(R.string.default_scheme_name)`
- 示例数据硬编码中文 → `appContext.getString(R.string.sample_work)` 等

### 5. `ui/components/SettingsPage.kt`
- 所有 Toast、对话框标题、按钮文本、分区标题 改用 `stringResource(...)` 或 `context.getString(...)`

### 6. `ui/components/TreeEditor.kt`
- 对话框标题、确认/取消按钮、图标描述、按钮文本 改用 `stringResource(...)`

### 7. `ui/components/NodeEditItem.kt`
- 权重标签（`when` 表达式）、类型标签、内容描述、文本元素 改用 `stringResource(...)`

### 8. `ui/components/FunctionPage.kt`
- `"点击进入详情页编辑"` → `stringResource(R.string.click_to_edit_detail_page)`
- `"无法打开此文件"` Toast → `context.getString(R.string.cannot_open_file)`
- 日期格式硬编码 `"MM月dd日 EEEE"` → `SimpleDateFormat(stringResource(R.string.date_format_pattern), ...)`

### 9. `ui/components/CalendarEditorDialog.kt`
- 完全改写为使用 `stringResource(...)` 的所有文本

### 10. `ui/components/WeightedTileLayout.kt`
- 完全改写为使用 `stringResource(...)` 的所有文本

### 11. `ui/components/NoteDetailEditor.kt`
- 完全改写为使用 `stringResource(...)` 的所有工具栏标签、占位符、错误消息

---

## 三、i18n 使用规范

### Composable 函数中
```kotlin
import androidx.compose.ui.res.stringResource

// 使用方式
Text(text = stringResource(R.string.settings))
```

### ViewModel / 非 Composable 上下文中
```kotlin
import com.example.myapplication.R

// 使用方式（需要 Context）
context.getString(R.string.default_scheme_name)
```

### 日期格式国际化
在 `values/strings.xml` 和 `values-en/strings.xml` 中定义同名 key：
```xml
<!-- values/strings.xml -->
<string name="date_format_pattern">MM月dd日 EEEE</string>

<!-- values-en/strings.xml -->
<string name="date_format_pattern">MMM dd, EEEE</string>
```
代码中：`SimpleDateFormat(stringResource(R.string.date_format_pattern), Locale.getDefault())`

---

## 四、如何添加新语言

1. 在 `app/src/main/res/` 下新建 `values-xx/` 目录（如 `values-ja/` 日语、`values-ko/` 韩语）
2. 复制 `values/strings.xml` 为 `values-xx/strings.xml`
3. 将所有中文值翻译为目标语言
4. 系统会根据设备语言自动切换

---

## 五、已知注意事项

- [ ] `date_format_pattern` 的英文格式当前为 `MMM dd, EEEE`，可根据需求调整
- [ ] 示例数据（sample_work 等）仅在首次创建默认方案时生效，已有数据不受影响
- [ ] 若后续新增硬编码字符串，需同步添加到 `values/strings.xml` 和 `values-en/strings.xml`

---

*本文档由 AI 辅助生成，记录本次 i18n 改造的全部操作。*

---
## 六、语言切换功能（2026-07-01）

### 新增：`LanguageManager.kt`
- SharedPreferences 持久化语言选择 zh/en
- `apply(context)` 在 attachBaseContext 中创建带目标 Locale 的 Context

### 修改：`MainActivity.kt`
- 新增 `attachBaseContext()` 应用已保存 Locale
- 主页头部右侧新增 Settings 图标（与 Edit 对称排列）

### 修改：`SettingsPage.kt`
- 新增 FilterChip 语言切换（中/英文），切换后 recreate()

### 新增 strings：language / language_chinese / language_english / language_switch_hint

---

## 七、卡片颜色自定义功能（2026-07-01）

### 数据模型
- `KnowledgeNode` 新增 `color: Long?` 字段（ARGB 颜色值），旧数据自动用 null（默认色）

### 新增 UI
- `NodeEditItem`: 卡片头部右侧（删除与星标之间）新增颜色色块，点击弹出 4×N 颜色网格
- 颜色选择器包含 13 种工程冷峻风格色 + 默认（无颜色）选项
- 选中颜色有蓝色圆形边框高亮

### 生效位置
- `WeightedTileLayout` 主页磁贴：节点有自定义颜色时使用，无则用系统默认色

### 新增 strings：card_color / default_color

---

## 八、卡片颜色 + 文字对比度配对（2026-07-01）

### 问题
自定义卡片颜色后，磁贴内的文字仍使用固定的 `onSurfaceVariant` 色值，暗色背景下对比度不足。

### 解决方案
- `Persistence.kt`: 新增 `CardColorScheme` 数据类和 `cardColorPalette`，为每种背景色定义配对的高可读文字色
- `getCardTextColor()` 函数：根据背景色自动返回对应的文字色
- 所有配对经过深色模式验证（暗底亮字对比度 ≥ 4.5:1）

### 修改文件
- `Persistence.kt`: 新增 `CardColorScheme` / `cardColorPalette` / `getCardTextColor()`
- `NodeEditItem.kt`: 颜色选择器改用共享调色板（`cardColorPalette.map { it.background }`）
- `WeightedTileLayout.kt`: 标题/副标题/日程标签的背景和文字色均使用 `getCardTextColor()` 动态计算

### 色彩对照表（深色模式）
| 背景色 | 名称 | 文字色 |
|--------|------|--------|
| 默认 | MaterialTheme | MaterialTheme.onSurfaceVariant |
| #1A1A2E | 深藏蓝 | #D8D8F0 淡蓝白 |
| #2D3047 | 暗灰蓝 | #E0E0F0 淡灰白 |
| #1B3A4B | 深海蓝 | #C0D8F0 淡天蓝 |
| #2C3E50 | 暗钢蓝 | #D0E0F0 淡钢白 |
| #3D5A80 | 钢蓝 | #C8D8F0 淡蓝 |
| #293241 | 暗蓝灰 | #DCE0E8 淡灰 |
| #3E4A56 | 冷灰 | #E4E8F0 极淡灰 |
| #1F3A3A | 暗青 | #C0E8D8 淡青 |
| #2F3E46 | 深灰绿 | #D8E4D8 淡绿白 |
| #4A4E69 | 灰紫 | #DCD4F0 淡紫 |
| #463F3A | 暖炭灰 | #ECDCC8 暖米白 |
| #3D405B | 暗紫蓝 | #D4D0F0 淡蓝紫 |
