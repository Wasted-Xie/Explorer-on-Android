// host/src/main/java/com/explorer/launcher/host/ui/components/ContextMenu.kt
package com.explorer.launcher.host.ui.components

import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paste
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.explorer.launcher.host.model.DesktopItem
import com.explorer.launcher.host.model.Folder
import com.explorer.launcher.host.ui.theme.Windows10Colors
import com.explorer.launcher.host.ui.theme.Windows10Shapes
import com.explorer.launcher.host.ui.theme.Windows10Typography

/**
 * 右键/长按上下文菜单
 */
@Composable
fun ContextMenu(
    isVisible: Boolean,
    position: androidx.compose.ui.geometry.Offset,
    menuType: ContextMenuType,
    onDismiss: () -> Unit,
    onAction: (ContextMenuAction) -> Unit,
    targetItem: DesktopItem? = null,
    targetFolder: Folder? = null,
    screenWidth: androidx.compose.ui.unit.Dp,
    screenHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val opacity by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(100)
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)
    )
    val offset by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = tween(150)
    )

    val menuWidth = 220.dp
    val menuHeight = when (menuType) {
        ContextMenuType.DESKTOP_EMPTY -> 200.dp
        ContextMenuType.DESKTOP_ITEM -> 280.dp
        ContextMenuType.FOLDER -> 240.dp
        ContextMenuType.TASKBAR_APP -> 200.dp
        ContextMenuType.TASKBAR_EMPTY -> 160.dp
    }

    // 计算菜单位置（防止超出屏幕）
    val adjustedX = (position.x - menuWidth / 2).coerceIn(8.dp, screenWidth - menuWidth - 8.dp)
    val adjustedY = (position.y - menuHeight / 2).coerceIn(8.dp, screenHeight - menuHeight - 8.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.0)) // 透明背景用于点击消失
            .pointerInput(Unit) {
                detectTapGestures(onTap = onDismiss)
            }
    ) {
        Box(
            modifier = Modifier
                .size(menuWidth, menuHeight)
                .graphicsLayer {
                    translationX = adjustedX
                    translationY = adjustedY + offset.value
                    alpha = opacity
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.TopStart
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Windows10Colors.StartMenuBackground, Windows10Shapes.Medium)
                    .clip(Windows10Shapes.Medium)
                    .shadow(Windows10Shadows.MenuElevation)
            ) {
                when (menuType) {
                    ContextMenuType.DESKTOP_EMPTY -> DesktopEmptyMenu(onAction, onDismiss)
                    ContextMenuType.DESKTOP_ITEM -> DesktopItemMenu(targetItem!!, onAction, onDismiss)
                    ContextMenuType.FOLDER -> FolderMenu(targetFolder!!, onAction, onDismiss)
                    ContextMenuType.TASKBAR_APP -> TaskbarAppMenu(targetItem!!, onAction, onDismiss)
                    ContextMenuType.TASKBAR_EMPTY -> TaskbarEmptyMenu(onAction, onDismiss)
                }
            }
        }
    }
}

enum class ContextMenuType {
    DESKTOP_EMPTY,    // 桌面空白处
    DESKTOP_ITEM,     // 桌面图标
    FOLDER,           // 文件夹
    TASKBAR_APP,      // 任务栏应用
    TASKBAR_EMPTY     // 任务栏空白处
}

enum class ContextMenuAction(
    val label: String,
    val icon: ImageVector,
    val isDestructive: Boolean = false
) {
    // 通用
    OPEN("打开", Icons.Default.Launch),
    RENAME("重命名", Icons.Default.Edit),
    DELETE("删除", Icons.Default.Delete, true),
    PROPERTIES("属性", Icons.Default.Info),

    // 桌面空白处
    NEW_FOLDER("新建文件夹", Icons.Default.Folder),
    NEW_SHORTCUT("新建快捷方式", Icons.Default.Link),
    PASTE("粘贴", Icons.Default.ContentPaste),
    VIEW_SETTINGS("查看设置", Icons.Default.Settings),
    SORT_BY("排序方式", Icons.Default.Sort),

    // 桌面图标
    RUN_AS_ADMIN("以管理员身份运行", Icons.Default.Security),
    PIN_TO_TASKBAR("固定到任务栏", Icons.Default.PushPin),
    UNPIN_FROM_TASKBAR("从任务栏取消固定", Icons.Default.PushPin),
    CREATE_SHORTCUT("创建快捷方式", Icons.Default.Link),

    // 文件夹
    RENAME_FOLDER("重命名文件夹", Icons.Default.Edit),
    CHANGE_COLOR("更改颜色", Icons.Default.ColorLens),

    // 任务栏应用
    UNPIN("取消固定", Icons.Default.PushPin),
    CLOSE_WINDOW("关闭窗口", Icons.Default.Close),
    CLOSE_ALL_WINDOWS("关闭所有窗口", Icons.Default.FullscreenExit),

    // 任务栏空白处
    TASKBAR_SETTINGS("任务栏设置", Icons.Default.Settings),
    SHOW_DESKTOP("显示桌面", Icons.Default.DesktopMac)
}

@Composable
fun ContextMenuItem(
    action: ContextMenuAction,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by remember {
        derivedStateOf {
            if (!enabled) Color.Transparent
            else if (isHovered) Windows10Colors.TaskbarHover
            else Color.Transparent
        }
    }

    val textColor by remember {
        derivedStateOf {
            if (!enabled) Windows10Colors.TextDisabled
            else if (action.isDestructive) Windows10Colors.Error
            else Windows10Colors.TextPrimary
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(backgroundColor, Windows10Shapes.Small)
            .clip(Windows10Shapes.Small)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (enabled) onClick() }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = if (!enabled) Windows10Colors.TextDisabled else textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = action.label,
                style = Windows10Typography.BodyMedium.copy(color = textColor),
                modifier = Modifier.weight(1f)
            )
            // 快捷键提示（可选）
            // Text(text = "Ctrl+V", style = Windows10Typography.Caption, color = Windows10Colors.TextSecondary)
        }
    }
}

@Composable
fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 8.dp)
            .background(Windows10Colors.Divider.copy(alpha = 0.3f))
    )
}

/**
 * 桌面空白处菜单
 */
@Composable
fun DesktopEmptyMenu(
    onAction: (ContextMenuAction) -> Unit,
    onDismiss: () -> Unit
) {
    Column {
        // 新建
        ContextMenuItem(ContextMenuAction.NEW_FOLDER) { onAction(ContextMenuAction.NEW_FOLDER); onDismiss() }
        ContextMenuItem(ContextMenuAction.NEW_SHORTCUT) { onAction(ContextMenuAction.NEW_SHORTCUT); onDismiss() }
        MenuDivider()

        // 粘贴
        ContextMenuItem(ContextMenuAction.PASTE, enabled = false) { } // TODO: 检查剪贴板
        MenuDivider()

        // 视图
        ContextMenuItem(ContextMenuAction.SORT_BY) { onAction(ContextMenuAction.SORT_BY); onDismiss() }
        ContextMenuItem(ContextMenuAction.VIEW_SETTINGS) { onAction(ContextMenuAction.VIEW_SETTINGS); onDismiss() }
    }
}

/**
 * 桌面图标菜单
 */
@Composable
fun DesktopItemMenu(
    item: DesktopItem,
    onAction: (ContextMenuAction) -> Unit,
    onDismiss: () -> Unit
) {
    Column {
        ContextMenuItem(ContextMenuAction.OPEN) { onAction(ContextMenuAction.OPEN); onDismiss() }
        MenuDivider()

        ContextMenuItem(ContextMenuAction.PIN_TO_TASKBAR) { onAction(ContextMenuAction.PIN_TO_TASKBAR); onDismiss() }
        ContextMenuItem(ContextMenuAction.CREATE_SHORTCUT) { onAction(ContextMenuAction.CREATE_SHORTCUT); onDismiss() }
        MenuDivider()

        ContextMenuItem(ContextMenuAction.RENAME) { onAction(ContextMenuAction.RENAME); onDismiss() }
        ContextMenuItem(ContextMenuAction.PROPERTIES) { onAction(ContextMenuAction.PROPERTIES); onDismiss() }
        MenuDivider()

        ContextMenuItem(ContextMenuAction.DELETE) { onAction(ContextMenuAction.DELETE); onDismiss() }
    }
}

/**
 * 文件夹菜单
 */
@Composable
fun FolderMenu(
    folder: Folder,
    onAction: (ContextMenuAction) -> Unit,
    onDismiss: () -> Unit
) {
    Column {
        ContextMenuItem(ContextMenuAction.RENAME_FOLDER) { onAction(ContextMenuAction.RENAME_FOLDER); onDismiss() }
        ContextMenuItem(ContextMenuAction.CHANGE_COLOR) { onAction(ContextMenuAction.CHANGE_COLOR); onDismiss() }
        MenuDivider()
        ContextMenuItem(ContextMenuAction.DELETE) { onAction(ContextMenuAction.DELETE); onDismiss() }
    }
}

/**
 * 任务栏应用菜单
 */
@Composable
fun TaskbarAppMenu(
    item: DesktopItem,
    onAction: (ContextMenuAction) -> Unit,
    onDismiss: () -> Unit
) {
    Column {
        ContextMenuItem(ContextMenuAction.UNPIN) { onAction(ContextMenuAction.UNPIN); onDismiss() }
        MenuDivider()
        ContextMenuItem(ContextMenuAction.CLOSE_WINDOW) { onAction(ContextMenuAction.CLOSE_WINDOW); onDismiss() }
        ContextMenuItem(ContextMenuAction.CLOSE_ALL_WINDOWS) { onAction(ContextMenuAction.CLOSE_ALL_WINDOWS); onDismiss() }
        MenuDivider()
        ContextMenuItem(ContextMenuAction.PROPERTIES) { onAction(ContextMenuAction.PROPERTIES); onDismiss() }
    }
}

/**
 * 任务栏空白处菜单
 */
@Composable
fun TaskbarEmptyMenu(
    onAction: (ContextMenuAction) -> Unit,
    onDismiss: () -> Unit
) {
    Column {
        ContextMenuItem(ContextMenuAction.TASKBAR_SETTINGS) { onAction(ContextMenuAction.TASKBAR_SETTINGS); onDismiss() }
        MenuDivider()
        ContextMenuItem(ContextMenuAction.SHOW_DESKTOP) { onAction(ContextMenuAction.SHOW_DESKTOP); onDismiss() }
    }
}