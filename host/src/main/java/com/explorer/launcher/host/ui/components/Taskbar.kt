// host/src/main/java/com/explorer/launcher/host/ui/components/Taskbar.kt
package com.explorer.launcher.host.ui.components

import androidx.compose.animation.animateDpAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ViewModule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.explorer.launcher.host.model.DesktopItem
import com.explorer.launcher.host.model.TaskbarConfig
import com.explorer.launcher.host.ui.theme.TaskbarBackgroundColor
import com.explorer.launcher.host.ui.theme.Windows10Colors
import com.explorer.launcher.host.ui.theme.Windows10Shadows
import com.explorer.launcher.host.ui.theme.Windows10Shapes
import com.explorer.launcher.host.ui.theme.Windows10Typography

/**
 * Windows 10 风格任务栏组件
 */
@Composable
fun Taskbar(
    config: TaskbarConfig = TaskbarConfig(),
    pinnedApps: List<DesktopItem> = emptyList(),
    runningApps: List<DesktopItem> = emptyList(),
    onStartClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTaskViewClick: () -> Unit,
    onWidgetsClick: () -> Unit,
    onAppClick: (DesktopItem) -> Unit,
    onAppLongClick: (DesktopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val taskbarHeight = config.height.dp
    val iconSize = config.iconSize.dp

    // 任务栏背景
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(taskbarHeight)
            .background(TaskbarBackgroundColor())
            .clip(Windows10Shapes.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 开始按钮
            if (config.showStartButton) {
                StartButton(
                    onClick = onStartClick,
                    size = taskbarHeight,
                    iconSize = iconSize
                )
            }

            // 搜索框
            if (config.showSearchBox) {
                SearchBox(
                    onClick = onSearchClick,
                    height = taskbarHeight * 0.6f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
            }

            // 任务视图按钮
            if (config.showTaskView) {
                TaskbarIconButton(
                    icon = Icons.Default.ViewModule,
                    contentDescription = "任务视图",
                    onClick = onTaskViewClick,
                    size = taskbarHeight,
                    iconSize = iconSize
                )
            }

            // 固定应用 + 运行中应用
            TaskbarAppsSection(
                pinnedApps = pinnedApps,
                runningApps = runningApps,
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
                taskbarHeight = taskbarHeight,
                iconSize = iconSize
            )

            Spacer(Modifier.weight(1f))

            // 小工具按钮
            if (config.showWidgets) {
                TaskbarIconButton(
                    icon = Icons.Default.Widgets,
                    contentDescription = "小工具",
                    onClick = onWidgetsClick,
                    size = taskbarHeight,
                    iconSize = iconSize
                )
            }

            // 系统托盘区
            SystemTray(
                taskbarHeight = taskbarHeight,
                iconSize = iconSize
            )

            // 日期时间
            DateTimeWidget(
                taskbarHeight = taskbarHeight
            )

            // 通知中心/操作中心按钮
            ActionCenterButton(
                taskbarHeight = taskbarHeight,
                iconSize = iconSize
            )
        }
    }
}

/**
 * 开始按钮
 */
@Composable
fun StartButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    var isHovered by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor by remember {
        derivedStateOf {
            when {
                isPressed -> Windows10Colors.TaskbarPressed
                isHovered -> Windows10Colors.TaskbarHover
                else -> Color.Transparent
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(backgroundColor, Windows10Shapes.Small)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; onClick(); isPressed = false },
                    onTap = { /* 已在 onPress 中处理 */ }
                )
            }
            .onGloballyPositioned { /* 可用于获取位置 */ }
    ) {
        // Windows 徽标图标 - 使用文本替代
        Text(
            text = "⊞",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Windows10Colors.TextPrimary,
            modifier = Modifier
                .size(iconSize)
                .padding(start = 4.dp, end = 4.dp)
                .align(Alignment.Center)
        )
    }
}

/**
 * 搜索框
 */
@Composable
fun SearchBox(
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val searchBackground = com.explorer.launcher.host.ui.theme.SearchBackgroundColor()

    Box(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .background(searchBackground, Windows10Shapes.Pill)
            .clip(Windows10Shapes.Pill)
            .pointerInput(Unit) {
                detectTapGestures(onTap = onClick)
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = Windows10Colors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "搜索应用和文件...",
                style = Windows10Typography.BodyMedium,
                color = Windows10Colors.TextSecondary
            )
        }
    }
}

/**
 * 任务栏应用图标按钮
 */
@Composable
fun TaskbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    isActive: Boolean = false,
    isPinned: Boolean = false
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by remember {
        derivedStateOf {
            when {
                isActive -> Windows10Colors.TaskbarPressed
                isHovered -> Windows10Colors.TaskbarHover
                else -> Color.Transparent
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(backgroundColor, Windows10Shapes.Small)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* 悬停反馈 */ },
                    onTap = onClick
                )
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Windows10Colors.WindowsBlue else Windows10Colors.TextPrimary,
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center)
        )
        // 运行指示器（底部小横线）
        if (isActive || isPinned) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(Windows10Colors.WindowsBlue, Windows10Shapes.Small)
            )
        }
    }
}

/**
 * 任务栏应用区域
 */
@Composable
fun TaskbarAppsSection(
    pinnedApps: List<DesktopItem>,
    runningApps: List<DesktopItem>,
    onAppClick: (DesktopItem) -> Unit,
    onAppLongClick: (DesktopItem) -> Unit,
    taskbarHeight: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val allApps = remember(pinnedApps, runningApps) {
        // 合并固定和运行中，去重
        val seen = mutableSetOf<String>()
        (pinnedApps + runningApps).filter { it.packageName.isNotBlank() && seen.add(it.packageName) }
    }

    Row(
        modifier = Modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        allApps.forEach { app ->
            val isPinned = pinnedApps.any { it.packageName == app.packageName }
            val isRunning = runningApps.any { it.packageName == app.packageName }

            TaskbarAppIcon(
                app = app,
                isRunning = isRunning,
                isPinned = isPinned,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) },
                taskbarHeight = taskbarHeight,
                iconSize = iconSize
            )
        }
    }
}

/**
 * 任务栏单个应用图标
 */
@Composable
fun TaskbarAppIcon(
    app: DesktopItem,
    isRunning: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    taskbarHeight: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by remember {
        derivedStateOf {
            when {
                isRunning -> Windows10Colors.TaskbarPressed
                isHovered -> Windows10Colors.TaskbarHover
                else -> Color.Transparent
            }
        }
    }

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(taskbarHeight)
            .background(backgroundColor, Windows10Shapes.Small)
            .clip(Windows10Shapes.Small)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* 悬停 */ },
                    onTap = onClick,
                    onLongPress = onLongClick
                )
            }
    ) {
        // 应用图标
        Box(
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center)
                .background(
                    Color.Transparent,
                    Windows10Shapes.Medium
                )
        ) {
            // 暂显示标签首字母
            Text(
                text = app.label.take(1).uppercase(),
                style = Windows10Typography.LabelMedium,
                color = Windows10Colors.TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 运行状态指示器
        if (isRunning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(Windows10Colors.WindowsBlue, Windows10Shapes.Small)
            )
        }
        // 固定状态指示器（左侧小竖线）
        if (isPinned && !isRunning) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(Windows10Colors.WindowsBlue.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * 系统托盘区
 */
@Composable
fun SystemTray(
    taskbarHeight: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .height(taskbarHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TrayIcon(Icons.Default.Wifi, "网络")
        TrayIcon(Icons.Default.VolumeUp, "音量")
        TrayIcon(Icons.Default.BatteryFull, "电池")
    }
}

@Composable
fun TrayIcon(icon: ImageVector, contentDescription: String) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(
                if (isHovered) Windows10Colors.TrayItemHover else Color.Transparent,
                Windows10Shapes.Small
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* 悬停 */ },
                    onTap = { /* 点击显示详细面板 */ }
                )
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Windows10Colors.TextPrimary,
            modifier = Modifier.size(16.dp).align(Alignment.Center)
        )
    }
}

/**
 * 日期时间显示
 */
@Composable
fun DateTimeWidget(
    taskbarHeight: androidx.compose.ui.unit.Dp
) {
    val timeText by remember {
        mutableStateOf(java.text.SimpleDateFormat("HH:mm\nyyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date()))
    }

    Box(
        modifier = Modifier
            .height(taskbarHeight)
            .width(80.dp)
            .padding(horizontal = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { /* 打开日历 */ })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeText.lines().first(),
                style = Windows10Typography.LabelMedium.copy(fontWeight = FontWeight.Medium),
                color = Windows10Colors.TextPrimary
            )
            Text(
                text = timeText.lines().getOrElse(1) { "" },
                style = Windows10Typography.Caption,
                color = Windows10Colors.TextSecondary
            )
        }
    }
}

/**
 * 操作中心/通知中心按钮
 */
@Composable
fun ActionCenterButton(
    taskbarHeight: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp
) {
    var isHovered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(taskbarHeight)
            .background(
                if (isHovered) Windows10Colors.TaskbarHover else Color.Transparent,
                Windows10Shapes.Small
            )
            .clip(Windows10Shapes.Small)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { /* 打开通知中心 */ })
            }
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = "通知中心",
            tint = Windows10Colors.TextPrimary,
            modifier = Modifier.size(iconSize).align(Alignment.Center)
        )
    }
}