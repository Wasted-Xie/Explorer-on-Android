// host/src/main/java/com/explorer/launcher/host/ui/components/StartMenu.kt
package com.explorer.launcher.host.ui.components

import androidx.compose.animation.animateBoolAsState
import androidx.compose.animation.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.explorer.launcher.host.model.DesktopItem
import com.explorer.launcher.host.ui.theme.Windows10Colors
import com.explorer.launcher.host.ui.theme.Windows10Shapes
import com.explorer.launcher.host.ui.theme.Windows10Typography

/**
 * Windows 10 风格开始菜单
 * 左侧：固定应用、最近使用、电源按钮
 * 右侧：磁贴式应用网格
 */
@Composable
fun StartMenu(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    pinnedApps: List<DesktopItem> = emptyList(),
    recentApps: List<DesktopItem> = emptyList(),
    tileApps: List<DesktopItem> = emptyList(),
    onAppClick: (DesktopItem) -> Unit,
    onPowerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAllAppsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpanded by remember { mutableStateOf(false) }

    // 动画
    val width by animateDpAsState(
        targetValue = if (isExpanded) 600.dp else 480.dp,
        animationSpec = tween(200)
    )
    val opacity by animateBoolAsState(isVisible, animationSpec = tween(150))
    val scale by animateDpAsState(
        targetValue = if (isVisible) 1.dp else 0.95.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
    )

    if (!isVisible && opacity.value == 0f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = onDismiss)
            }
    ) {
        // 开始菜单主体 - 靠左下角
        Box(
            modifier = Modifier
                .width(width)
                .height(520.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = opacity.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.BottomStart
                }
                .align(Alignment.BottomStart)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Windows10Colors.StartMenuBackground, Windows10Shapes.Medium)
                    .clip(Windows10Shapes.Medium)
            ) {
                // 顶部：用户头像/账户
                UserHeader(onClick = { /* 打开账户设置 */ })

                // 左侧：固定应用 + 最近使用
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 左侧列表
                    Column(
                        modifier = Modifier
                            .width(if (isExpanded) 200.dp else 160.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 固定应用
                        if (pinnedApps.isNotEmpty()) {
                            SectionHeader("固定")
                            LazyColumn(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(pinnedApps.take(8)) { app ->
                                    StartMenuAppItem(
                                        app = app,
                                        isExpanded = isExpanded,
                                        onClick = { onAppClick(app); onDismiss() }
                                    )
                                }
                            }
                        }

                        // 分割线
                        Spacer(Modifier.height(8.dp).fillMaxWidth()
                            .background(Windows10Colors.Divider.copy(alpha = 0.3f), Windows10Shapes.Small)
                        )

                        // 最近使用
                        if (recentApps.isNotEmpty()) {
                            SectionHeader("最近使用")
                            LazyColumn(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(recentApps.take(6)) { app ->
                                    StartMenuAppItem(
                                        app = app,
                                        isExpanded = isExpanded,
                                        onClick = { onAppClick(app); onDismiss() }
                                    )
                                }
                            }
                        }
                    }

                    // 右侧：磁贴网格
                    if (isExpanded && tileApps.isNotEmpty()) {
                        Spacer(Modifier.width(12.dp).height(1.dp)
                            .background(Windows10Colors.Divider.copy(alpha = 0.3f))
                        )
                        TileGrid(
                            apps = tileApps,
                            onAppClick = { onAppClick(it); onDismiss() },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp, end = 8.dp)
                        )
                    }
                }

                // 底部：电源、设置、全屏展开按钮
                BottomBar(
                    isExpanded = isExpanded,
                    onPowerClick = { onPowerClick(); onDismiss() },
                    onSettingsClick = { onSettingsClick(); onDismiss() },
                    onAllAppsClick = { onAllAppsClick(); isExpanded = !isExpanded }
                )
            }
        }
    }
}

/**
 * 用户头部区域
 */
@Composable
fun UserHeader(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = onClick)
            }
            .background(Color.Transparent, Windows10Shapes.Medium)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像占位
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Windows10Colors.WindowsBlue, Windows10Shapes.Large)
            ) {
                Text(
                    text = "用户",
                    style = Windows10Typography.LabelMedium.copy(color = Windows10Colors.TextPrimary),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "用户名",
                    style = Windows10Typography.BodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Windows10Colors.TextPrimary
                )
                Text(
                    text = "user@example.com",
                    style = Windows10Typography.Caption,
                    color = Windows10Colors.TextSecondary
                )
            }
        }
    }
}

/**
 * 区域标题
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = Windows10Typography.LabelMedium.copy(
            color = Windows10Colors.TextSecondary,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

/**
 * 开始菜单应用项
 */
@Composable
fun StartMenuAppItem(
    app: DesktopItem,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by remember {
        derivedStateOf {
            if (isHovered) Windows10Colors.TaskbarHover else Color.Transparent
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(backgroundColor, Windows10Shapes.Small)
            .clip(Windows10Shapes.Small)
            .pointerInput(Unit) {
                detectTapGestures(onTap = onClick)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier.size(24.dp)
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = Windows10Typography.LabelMedium,
                    color = Windows10Colors.TextPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.width(12.dp))
            // 标签
            if (isExpanded) {
                Text(
                    text = app.label,
                    style = Windows10Typography.BodyMedium,
                    color = Windows10Colors.TextPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 磁贴网格
 */
@Composable
fun TileGrid(
    apps: List<DesktopItem>,
    onAppClick: (DesktopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = 4
    val rows = 3

    androidx.compose.foundation.layout.Grid(
        cells = androidx.compose.foundation.layout.GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        apps.forEach { app ->
            TileItem(
                app = app,
                size = TileSize.MEDIUM,
                onClick = { onAppClick(app) }
            )
        }
    }
}

/**
 * 磁贴尺寸
 */
enum class TileSize {
    SMALL,      // 1x1
    MEDIUM,     // 2x2
    WIDE,       // 4x2
    LARGE       // 4x4
}

/**
 * 磁贴项
 */
@Composable
fun TileItem(
    app: DesktopItem,
    size: TileSize = TileSize.MEDIUM,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val (width, height) = when (size) {
        TileSize.SMALL -> 60.dp to 60.dp
        TileSize.MEDIUM -> 130.dp to 130.dp
        TileSize.WIDE -> 270.dp to 130.dp
        TileSize.LARGE -> 270.dp to 270.dp
    }

    val backgroundColor by remember {
        derivedStateOf {
            if (isHovered) Windows10Colors.StartMenuTileBackgroundLight.copy(alpha = 0.8f)
            else Windows10Colors.StartMenuTileBackground
        }
    }

    Box(
        modifier = Modifier
            .size(width, height)
            .background(backgroundColor, Windows10Shapes.Medium)
            .clip(Windows10Shapes.Medium)
            .pointerInput(Unit) {
                detectTapGestures(onTap = onClick)
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标
            Box(
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = Windows10Typography.TitleMedium.copy(color = Windows10Colors.TextPrimary),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(8.dp))
            // 标签
            Text(
                text = app.label,
                style = Windows10Typography.BodyMedium,
                color = Windows10Colors.TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.TextAlign.Center
            )
        }
    }
}

/**
 * 底部栏
 */
@Composable
fun BottomBar(
    isExpanded: Boolean,
    onPowerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAllAppsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Windows10Colors.StartMenuTileBackground, Windows10Shapes.Medium)
            .clip(Windows10Shapes.Medium)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 电源按钮
            BottomBarButton(
                icon = Icons.Default.PowerSettingsNew,
                label = "电源",
                onClick = onPowerClick
            )

            // 设置按钮
            BottomBarButton(
                icon = Icons.Default.Settings,
                label = "设置",
                onClick = onSettingsClick
            )

            Spacer(Modifier.weight(1f))

            // 全屏/展开按钮
            BottomBarButton(
                icon = if (isExpanded) Icons.Default.ArrowBack else Icons.Default.Apps,
                label = if (isExpanded) "收起" else "所有应用",
                onClick = onAllAppsClick
            )
        }
    }
}

@Composable
fun BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by remember {
        derivedStateOf {
            if (isHovered) Windows10Colors.TaskbarHover else Color.Transparent
        }
    }

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(48.dp)
            .background(backgroundColor, Windows10Shapes.Small)
            .clip(Windows10Shapes.Small)
            .pointerInput(Unit) {
                detectTapGestures(onTap = onClick)
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Windows10Colors.TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            if (isHovered) { // 悬停时显示标签
                Text(
                    text = label,
                    style = Windows10Typography.Caption,
                    color = Windows10Colors.TextSecondary
                )
            }
        }
    }
}