// host/src/main/java/com/explorer/launcher/host/ui/MainActivity.kt
package com.explorer.launcher.host.ui

import android.os.Bundle
import android.view.DisplayCutout
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.explorer.launcher.host.HostApplication
import com.explorer.launcher.host.model.DesktopItem
import com.explorer.launcher.host.model.DesktopLayout
import com.explorer.launcher.host.model.Folder
import com.explorer.launcher.host.model.TaskbarConfig
import com.explorer.launcher.host.plugin.PluginInfo
import com.explorer.launcher.host.plugin.PluginManager
import com.explorer.launcher.host.ui.components.AppDrawer
import com.explorer.launcher.host.ui.components.ContextMenu
import com.explorer.launcher.host.ui.components.ContextMenuAction
import com.explorer.launcher.host.ui.components.ContextMenuState
import com.explorer.launcher.host.ui.components.ContextMenuType
import com.explorer.launcher.host.ui.components.DesktopGrid
import com.explorer.launcher.host.ui.components.StartMenu
import com.explorer.launcher.host.ui.components.Taskbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Map 转 Bundle 扩展函数
 */
fun Map<String, String>.toBundle(): Bundle {
    return Bundle().apply {
        this.forEach { (key, value) -> putString(key, value) }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沉浸式状态栏/导航栏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        setContent {
            ExplorerLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    LauncherScreen(
                        plugins = viewModel.plugins.observeAsState(initialValue = emptyList()).value,
                        desktopItems = viewModel.desktopItems.observeAsState(initialValue = emptyList()).value,
                        folders = viewModel.folders.observeAsState(initialValue = emptyList()).value,
                        runningApps = viewModel.runningApps.observeAsState(initialValue = emptyList()).value,
                        pinnedApps = viewModel.pinnedApps.observeAsState(initialValue = emptyList()).value,
                        onAppClick = viewModel::onAppClick,
                        onAppLongClick = viewModel::onAppLongClick,
                        onItemDrag = viewModel::onItemDrag,
                        onDragEnd = viewModel::onDragEnd,
                        onEmptyAreaLongClick = viewModel::onEmptyAreaLongClick,
                        onStartClick = viewModel::toggleStartMenu,
                        onSearchClick = viewModel::toggleAppDrawer,
                        onTaskViewClick = viewModel::onTaskViewClick,
                        onWidgetsClick = viewModel::onWidgetsClick,
                        onPowerClick = viewModel::onPowerClick,
                        onSettingsClick = viewModel::onSettingsClick,
                        onAllAppsClick = viewModel::onAllAppsClick
                    )
                }
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pluginManager: PluginManager
) : ViewModel() {
    // 插件列表
    private val _plugins = MutableLiveData<List<PluginInfo>>()
    val plugins: LiveData<List<PluginInfo>> = _plugins

    // 桌面图标
    private val _desktopItems = MutableLiveData<List<DesktopItem>>()
    val desktopItems: LiveData<List<DesktopItem>> = _desktopItems

    // 文件夹
    private val _folders = MutableLiveData<List<Folder>>()
    val folders: LiveData<List<Folder>> = _folders

    // 运行中的应用（任务栏显示）
    private val _runningApps = MutableLiveData<List<DesktopItem>>()
    val runningApps: LiveData<List<DesktopItem>> = _runningApps

    // 固定到任务栏的应用
    private val _pinnedApps = MutableLiveData<List<DesktopItem>>()
    val pinnedApps: LiveData<List<DesktopItem>> = _pinnedApps

    // UI 状态
    private val _isStartMenuOpen = MutableLiveData(false)
    val isStartMenuOpen: LiveData<Boolean> = _isStartMenuOpen

    private val _isAppDrawerOpen = MutableLiveData(false)
    val isAppDrawerOpen: LiveData<Boolean> = _isAppDrawerOpen

    private val _contextMenu = MutableLiveData<ContextMenuState?>(null)
    val contextMenu: LiveData<ContextMenuState?> = _contextMenu

    private val _desktopLayout = MutableLiveData(DesktopLayout())
    val desktopLayout: LiveData<DesktopLayout> = _desktopLayout

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        scope.launch {
            // 加载插件
            _plugins.value = pluginManager.getAllPlugins()
            pluginManager.addListener(object : PluginManager.PluginEventListener {
                override fun onPluginsChanged() {
                    _plugins.postValue(pluginManager.getAllPlugins())
                }
                override fun onPluginStateChanged(pluginId: String) {
                    _plugins.postValue(pluginManager.getAllPlugins())
                }
            })

            // 初始化桌面图标（从存储加载，这里用示例数据）
            _desktopItems.value = generateSampleDesktopItems()
            _pinnedApps.value = generateSampleDesktopItems().take(3).toList()
            _runningApps.value = emptyList()
            _folders.value = emptyList()
        }
    }

    private fun generateSampleDesktopItems(): List<DesktopItem> {
        return listOf(
            DesktopItem.createPluginPanel("com.explorer.plugin.filemanager", "文件管理器"),
            DesktopItem.createAppShortcut("com.android.settings", "com.android.settings.Settings", "设置"),
            DesktopItem.createAppShortcut("com.android.chrome", "com.google.android.apps.chrome.Main", "Chrome"),
            DesktopItem.createAppShortcut("com.google.android.gm", "com.google.android.gm.ConversationListActivityGmail", "Gmail"),
            DesktopItem.createAppShortcut("com.google.android.youtube", "com.google.android.youtube.HomeActivity", "YouTube"),
            DesktopItem.createAppShortcut("com.whatsapp", "com.whatsapp.Main", "WhatsApp"),
            DesktopItem.createAppShortcut("com.instagram.android", "com.instagram.mainactivity.MainActivity", "Instagram"),
            DesktopItem.createAppShortcut("com.spotify.music", "com.spotify.music.MainActivity", "Spotify"),
            DesktopItem.createAppShortcut("com.netflix.mediaclient", "com.netflix.mediaclient.ui.launch.UILaunchActivity", "Netflix"),
            DesktopItem.createAppShortcut("com.microsoft.teams", "com.microsoft.teams.MainActivity", "Teams"),
            DesktopItem.createFolder("工作文件夹"),
            DesktopItem.createFolder("游戏"),
        )
    }

    // ========== 事件处理 ==========

    fun toggleStartMenu() {
        _isStartMenuOpen.value = !_isStartMenuOpen.value
    }

    fun toggleAppDrawer() {
        _isAppDrawerOpen.value = !_isAppDrawerOpen.value
    }

    fun onAppClick(item: DesktopItem) {
        // 启动应用/插件
        when (item.type) {
            DesktopItemType.PLUGIN_PANEL -> {
                val pluginId = item.extras["plugin_id"] ?: ""
                if (pluginId.isNotBlank()) {
                    // 请求插件面板
                    pluginManager.requestPanel(pluginId, item.extras.toBundle())
                }
            }
            DesktopItemType.APP -> {
                // 启动应用
                launchApp(item)
                // 添加到运行中
                addToRunningApps(item)
            }
            DesktopItemType.FOLDER -> {
                // 打开文件夹（显示文件夹视图）
                // TODO: 触发文件夹打开状态
            }
            else -> {}
        }
    }

    fun onAppLongClick(item: DesktopItem) {
        // 显示上下文菜单
        showContextMenu(ContextMenuType.DESKTOP_ITEM, item)
    }

    fun onEmptyAreaLongClick(offset: Offset) {
        showContextMenu(ContextMenuType.DESKTOP_EMPTY, null, offset)
    }

    fun onItemDrag(item: DesktopItem, dx: androidx.compose.ui.unit.Dp, dy: androidx.compose.ui.unit.Dp) {
        // 拖拽中，更新视觉反馈
    }

    fun onDragEnd(item: DesktopItem, newPosition: Int) {
        // 更新位置
        val updated = _desktopItems.value?.map { it.copyWithPosition(
            if (it.id == item.id) newPosition else it.position
        ) } ?: emptyList()
        _desktopItems.value = updated
    }

    fun addToRunningApps(item: DesktopItem) {
        val current = _runningApps.value ?: emptyList()
        if (current.none { it.packageName == item.packageName }) {
            _runningApps.value = current + item
        }
    }

    fun removeFromRunningApps(packageName: String) {
        _runningApps.value = (_runningApps.value ?: emptyList()).filter { it.packageName != packageName }
    }

    fun onTaskViewClick() {
        // TODO: 任务视图
    }

    fun onWidgetsClick() {
        // TODO: 小工具面板
    }

    fun onPowerClick() {
        // TODO: 电源菜单
    }

    fun onSettingsClick() {
        // TODO: 打开设置
    }

    fun onAllAppsClick() {
        _isAppDrawerOpen.value = true
    }

    private fun launchApp(item: DesktopItem) {
        // 实际启动 Intent
    }

    private fun showContextMenu(
        type: ContextMenuType,
        item: DesktopItem? = null,
        offset: Offset = Offset(0f, 0f)
    ) {
        _contextMenu.value = ContextMenuState(
            type = type,
            position = offset,
            targetItem = item,
            targetFolder = null
        )
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}

/**
 * 上下文菜单状态
 */
data class ContextMenuState(
    val type: ContextMenuType,
    val position: Offset,
    val targetItem: DesktopItem?,
    val targetFolder: Folder?
)

/**
 * 主启动器屏幕合成
 */
@Composable
fun LauncherScreen(
    plugins: List<PluginInfo>,
    desktopItems: List<DesktopItem>,
    folders: List<Folder>,
    runningApps: List<DesktopItem>,
    pinnedApps: List<DesktopItem>,
    onAppClick: (DesktopItem) -> Unit,
    onAppLongClick: (DesktopItem) -> Unit,
    onItemDrag: (DesktopItem, androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp) -> Unit,
    onDragEnd: (DesktopItem, Int) -> Unit,
    onEmptyAreaLongClick: (Offset) -> Unit,
    onStartClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTaskViewClick: () -> Unit,
    onWidgetsClick: () -> Unit,
    onPowerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAllAppsClick: () -> Unit
) {
    val isStartMenuOpen by remember { mutableStateOf(false) }
    val isAppDrawerOpen by remember { mutableStateOf(false) }
    val contextMenuState by remember { mutableStateOf<ContextMenuState?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 桌面网格
        DesktopGrid(
            layout = DesktopLayout(),
            items = desktopItems,
            folders = folders,
            onItemClick = onAppClick,
            onItemLongClick = { item, offset ->
                contextMenuState = ContextMenuState(
                    type = ContextMenuType.DESKTOP_ITEM,
                    position = offset,
                    targetItem = item,
                    targetFolder = null
                )
            },
            onEmptyAreaLongClick = { offset ->
                contextMenuState = ContextMenuState(
                    type = ContextMenuType.DESKTOP_EMPTY,
                    position = offset,
                    targetItem = null,
                    targetFolder = null
                )
            },
            onItemDrag = onItemDrag,
            onDragEnd = onDragEnd,
            modifier = Modifier.fillMaxSize()
        )

        // 任务栏（底部）
        Taskbar(
            config = TaskbarConfig(),
            pinnedApps = pinnedApps,
            runningApps = runningApps,
            onStartClick = { isStartMenuOpen = true },
            onSearchClick = { isAppDrawerOpen = true },
            onTaskViewClick = onTaskViewClick,
            onWidgetsClick = onWidgetsClick,
            onAppClick = onAppClick,
            onAppLongClick = { item ->
                contextMenuState = ContextMenuState(
                    type = ContextMenuType.TASKBAR_APP,
                    position = Offset(0f, 0f), // 任务栏菜单位置特殊处理
                    targetItem = item,
                    targetFolder = null
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 开始菜单
        StartMenu(
            isVisible = isStartMenuOpen,
            onDismiss = { isStartMenuOpen = false },
            pinnedApps = pinnedApps,
            recentApps = runningApps,
            tileApps = desktopItems.filter { it.type == DesktopItemType.APP }.take(12).toList(),
            onAppClick = onAppClick,
            onPowerClick = onPowerClick,
            onSettingsClick = onSettingsClick,
            onAllAppsClick = { isAppDrawerOpen = true; isStartMenuOpen = false }
        )

        // 应用抽屉
        AppDrawer(
            isVisible = isAppDrawerOpen,
            onDismiss = { isAppDrawerOpen = false },
            allApps = desktopItems.filter { it.type == DesktopItemType.APP },
            recentApps = runningApps,
            onAppClick = onAppClick,
            onAppLongClick = onAppLongClick
        )

        // 上下文菜单
        contextMenuState?.let { state ->
            ContextMenu(
                isVisible = true,
                position = state.position,
                menuType = state.type,
                onDismiss = { contextMenuState = null },
                onAction = { action ->
                    handleContextMenuAction(action, state.targetItem)
                    contextMenuState = null
                },
                targetItem = state.targetItem,
                targetFolder = state.targetFolder,
                screenWidth = 1080.dp, // TODO: 从 WindowMetrics 获取
                screenHeight = 1920.dp
            )
        }
    }
}

private fun handleContextMenuAction(action: ContextMenuAction, item: DesktopItem?) {
    // TODO: 处理各种上下文菜单动作
    when (action) {
        ContextMenuAction.OPEN -> item?.let { onAppClick(it) }
        ContextMenuAction.DELETE -> item?.let { /* 删除 */ }
        ContextMenuAction.RENAME -> item?.let { /* 重命名 */ }
        ContextMenuAction.PIN_TO_TASKBAR -> item?.let { /* 固定到任务栏 */ }
        ContextMenuAction.UNPIN -> item?.let { /* 取消固定 */ }
        ContextMenuAction.PROPERTIES -> item?.let { /* 属性 */ }
        else -> {}
    }
}