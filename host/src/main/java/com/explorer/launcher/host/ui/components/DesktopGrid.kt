// host/src/main/java/com/explorer/launcher/host/ui/components/DesktopGrid.kt
package com.explorer.launcher.host.ui.components

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.layout.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.graphics.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.explorer.launcher.host.model.DesktopItem
import com.explorer.launcher.host.model.DesktopLayout
import com.explorer.launcher.host.ui.theme.Windows10Colors
import com.explorer.launcher.host.ui.theme.Windows10Shapes
import com.explorer.launcher.host.ui.theme.Windows10Typography

/**
 * Windows 10 风格桌面网格
 * 支持图标拖拽排序、长按菜单、文件夹创建
 */
@Composable
fun DesktopGrid(
    layout: DesktopLayout = DesktopLayout(),
    items: List<DesktopItem> = emptyList(),
    folders: List<com.explorer.launcher.host.model.Folder> = emptyList(),
    wallpaper: Painter? = null,
    onItemClick: (DesktopItem) -> Unit,
    onItemLongClick: (DesktopItem, androidx.compose.ui.geometry.Offset) -> Unit,
    onEmptyAreaLongClick: (androidx.compose.ui.geometry.Offset) -> Unit,
    onItemDrag: (DesktopItem, androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp) -> Unit,
    onDragEnd: (DesktopItem, Int) -> Unit,  // 新位置
    modifier: Modifier = Modifier
) {
    val iconSize = layout.iconSize.dp
    val iconSpacing = layout.iconSpacing.dp
    val columns = layout.gridColumns
    val rows = layout.gridRows

    // 当前页的项目
    val pageItems = remember(items, layout.pageCount, layout.defaultPage) {
        val perPage = columns * rows
        val start = layout.defaultPage * perPage
        items.drop(start).take(perPage)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        // 壁纸背景
        if (wallpaper != null) {
            androidx.compose.ui.res.painterResource(
                id = com.explorer.launcher.host.R.drawable.default_wallpaper
            ).also { painter ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = "Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // 默认纯色背景
            Box(modifier = Modifier.fillMaxSize().background(Windows10Colors.DesktopBackground))
        }

        // 桌面图标网格
        LazyVerticalGrid(
            cells = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(iconSpacing),
            horizontalArrangement = Arrangement.spacedBy(iconSpacing),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            items(pageItems.size) { index ->
                val item = pageItems[index]
                DesktopIcon(
                    item = item,
                    layout = layout,
                    index = index,
                    onClick = { onItemClick(item) },
                    onLongClick = { offset -> onItemLongClick(item, offset) },
                    onDrag = { dx, dy -> onItemDrag(item, dx, dy) },
                    onDragEnd = { newPos -> onDragEnd(item, newPos) }
                )
            }
            // 填充空位以保持网格对齐
            items((columns * rows) - pageItems.size) {
                EmptyGridSlot(
                    iconSize = iconSize,
                    onLongClick = { offset -> onEmptyAreaLongClick(offset) }
                )
            }
        }

        // 页面指示器
        if (layout.pageCount > 1) {
            PageIndicator(
                currentPage = layout.defaultPage,
                totalPages = layout.pageCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp) // 任务栏上方
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 单个桌面图标
 */
@Composable
fun DesktopIcon(
    item: DesktopItem,
    layout: DesktopLayout,
    index: Int,
    onClick: () -> Unit,
    onLongClick: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDrag: (androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp) -> Unit,
    onDragEnd: (Int) -> Unit
) {
    var isSelected by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.unit.Offset(0.dp, 0.dp)) }
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else if (isSelected) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    )

    val iconSize = layout.iconSize.dp

    Box(
        modifier = Modifier
            .size(iconSize + 16.dp, iconSize + (if (layout.showLabels) 36.dp else 16.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = dragOffset.x
                translationY = dragOffset.y
                z = if (isDragging) 100f else if (isSelected) 10f else 0f
            }
            .pointerInput(Unit) {
                // 长按检测
                detectTapGestures(
                    onLongPress = { offset -> onLongClick(offset) },
                    onTap = { onClick() }
                )
                // 拖拽检测
                detectDragGestures(
                    onDragStart = { isDragging = true; isSelected = true },
                    onDrag = { change, dragAmount ->
                        dragOffset += dragAmount
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        isDragging = false
                        dragOffset = androidx.compose.ui.unit.Offset(0.dp, 0.dp)
                        // 计算新位置索引（简化版）
                        onDragEnd(index)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 图标容器
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(
                        if (isSelected) Windows10Colors.DesktopIconSelected
                        else Color.Transparent,
                        Windows10Shapes.Medium
                    )
                    .clip(Windows10Shapes.Medium)
            ) {
                // 图标内容
                if (item.iconRes != 0) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                } else if (item.iconUri.isNotBlank()) {
                    // 加载网络/文件图标 - 实际项目用 Coil
                    Text(
                        text = item.label.take(1).uppercase(),
                        style = Windows10Typography.TitleMedium,
                        color = Windows10Colors.TextPrimary,
                        modifier = Modifier.fillMaxSize().align(Alignment.Center)
                    )
                } else {
                    // 类型默认图标
                    when (item.type) {
                        com.explorer.launcher.host.model.DesktopItemType.FOLDER -> {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = item.label,
                                tint = Windows10Colors.TextPrimary,
                                modifier = Modifier.fillMaxSize().padding(8.dp).align(Alignment.Center)
                            )
                        }
                        else -> {
                            Text(
                                text = item.label.take(1).uppercase(),
                                style = Windows10Typography.TitleMedium,
                                color = Windows10Colors.TextPrimary,
                                modifier = Modifier.fillMaxSize().align(Alignment.Center)
                            )
                        }
                    }
                }

                // 选中框
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, Windows10Colors.WindowsBlue, Windows10Shapes.Medium)
                    )
                }
            }

            // 标签
            if (layout.showLabels) {
                Text(
                    text = item.label,
                    style = Windows10Typography.Caption.copy(
                        color = Windows10Colors.TextPrimary,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * 空网格槽位（用于长按创建快捷方式/文件夹）
 */
@Composable
fun EmptyGridSlot(
    iconSize: androidx.compose.ui.unit.Dp,
    onLongClick: (androidx.compose.ui.geometry.Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .size(iconSize + 16.dp, iconSize + 36.dp)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = onLongClick)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(
                        Windows10Colors.DesktopIconBackground,
                        Windows10Shapes.Medium
                    )
                    .clip(Windows10Shapes.Medium)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加",
                    tint = Windows10Colors.TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize().padding(16.dp).align(Alignment.Center)
                )
            }
            Text(
                text = "添加",
                style = Windows10Typography.Caption.copy(color = Windows10Colors.TextSecondary),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * 页面指示器
 */
@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentSize()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (0 until totalPages).forEach { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (isActive) 24.dp else 8.dp, 8.dp)
                    .background(
                        if (isActive) Windows10Colors.WindowsBlue else Windows10Colors.TextSecondary.copy(alpha = 0.4f),
                        Windows10Shapes.Pill
                    )
                    .animateContentSize()
            )
        }
    }
}

/**
 * 文件夹打开视图（模态底部弹出）
 */
@Composable
fun FolderView(
    folder: com.explorer.launcher.host.model.Folder,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onItemClick: (DesktopItem) -> Unit,
    onItemLongClick: (DesktopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val height by animateDpAsState(
        targetValue = if (isOpen) 300.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(250)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = onDismiss)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(Windows10Colors.StartMenuBackground, Windows10Shapes.Large)
                .clip(Windows10Shapes.Large)
                .align(Alignment.BottomCenter)
        ) {
            // 文件夹标题栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = folder.name,
                        style = Windows10Typography.TitleMedium,
                        color = Windows10Colors.TextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Windows10Colors.TextSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = onDismiss)
                            }
                    )
                }
            }

            // 文件夹内容网格
            androidx.compose.foundation.layout.Grid(
                cells = androidx.compose.foundation.layout.GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                folder.items.forEach { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onItemClick(item) },
                                    onLongPress = { onItemLongClick(item) }
                                )
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Windows10Colors.StartMenuTileBackground, Windows10Shapes.Medium)
                                    .clip(Windows10Shapes.Medium)
                            ) {
                                Text(
                                    text = item.label.take(1).uppercase(),
                                    style = Windows10Typography.TitleMedium,
                                    color = Windows10Colors.TextPrimary,
                                    modifier = Modifier.fillMaxSize().align(Alignment.Center)
                                )
                            }
                            Text(
                                text = item.label,
                                style = Windows10Typography.Caption,
                                color = Windows10Colors.TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}