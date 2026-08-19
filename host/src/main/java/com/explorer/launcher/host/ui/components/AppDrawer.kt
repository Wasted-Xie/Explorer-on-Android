// host/src/main/java/com/explorer/launcher/host/ui/components/AppDrawer.kt
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
 * Windows 10 风格应用抽屉
 * 从底部滑出，支持搜索、字母分组、最近使用
 */
@Composable
fun AppDrawer(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    allApps: List<DesktopItem> = emptyList(),
    recentApps: List<DesktopItem> = emptyList(),
    onAppClick: (DesktopItem) -> Unit,
    onAppLongClick: (DesktopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchText by remember { mutableStateOf("") }
    val showSearchResults by remember { derivedStateOf { searchText.isNotBlank() } }

    val opacity by animateBoolAsState(isVisible, animationSpec = tween(200))
    val height by animateDpAsState(
        targetValue = if (isVisible) 600.dp else 0.dp,
        animationSpec = tween(300, delayMillis = 50)
    )
    val yOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 100.dp,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f)
    )

    if (!isVisible && opacity.value == 0f && height.value == 0.dp) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = (0.4f * opacity.value)))
            .pointerInput(Unit) {
                detectTapGestures(onTap = onDismiss)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer {
                    translationY = yOffset.value
                    alpha = opacity.value
                }
                .background(Windows10Colors.StartMenuBackground, Windows10Shapes.Large)
                .clip(Windows10Shapes.Large)
                .align(Alignment.BottomCenter)
        ) {
            // 顶部栏
            DrawerHeader(
                onCloseClick = onDismiss,
                searchText = searchText,
                onSearchChange = { searchText = it },
                onSearchClick = { /* 聚焦搜索 */ }
            )

            // 内容区
            if (showSearchResults) {
                SearchResultsList(
                    apps = allApps.filter { it.label.lowercase().contains(searchText.lowercase()) },
                    onAppClick = { onAppClick(it); onDismiss() },
                    onAppLongClick = onAppLongClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                AppListContent(
                    allApps = allApps,
                    recentApps = recentApps,
                    onAppClick = { onAppClick(it); onDismiss() },
                    onAppLongClick = onAppLongClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * 抽屉头部：搜索栏 + 关闭按钮
 */
@Composable
fun DrawerHeader(
    onCloseClick: () -> Unit,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 搜索框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(Windows10Colors.SearchBackground, Windows10Shapes.Pill)
                    .clip(Windows10Shapes.Pill)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = onSearchClick)
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
                        text = if (searchText.isBlank()) "搜索应用..." else searchText,
                        style = Windows10Typography.BodyMedium,
                        color = if (searchText.isBlank()) Windows10Colors.TextSecondary else Windows10Colors.TextPrimary
                    )
                }
            }

            // 关闭按钮
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = onCloseClick)
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Windows10Colors.TextSecondary,
                    modifier = Modifier.size(24.dp).align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * 应用列表内容（分组显示）
 */
@Composable
fun AppListContent(
    allApps: List<DesktopItem>,
    recentApps: List<DesktopItem>,
    onAppClick: (DesktopItem) -> Unit,
    onAppLongClick: (DesktopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 最近使用
        if (recentApps.isNotEmpty()) {
            SectionHeader("最近使用")
            LazyVerticalGrid(
                cells = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentApps.take(8)) { app ->
                    AppDrawerItem(
                        app = app,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 所有应用（按字母分组）
        SectionHeader("所有应用")
        val groupedApps = allApps
            .filter { it.type == com.explorer.launcher.host.model.DesktopItemType.APP }
            .groupBy { it.label.first().uppercase() }
            .toList()
            .sortedBy { it.first }

        groupedApps.forEach { (letter, apps) ->
            AlphabetHeader(letter)
            LazyVerticalGrid(
                cells = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(apps) { app ->
                    AppDrawerItem(
                        app = app,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 字母分组标题
 */
@Composable
fun AlphabetHeader(letter: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = letter,
            style = Windows10Typography.LabelMedium.copy(
                color = Windows10Colors.TextSecondary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        )
    }
}

/**
 * 应用抽屉单项
 */
@Composable
fun AppDrawerItem(
    app: DesktopItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    val backgroundColor by remember {
        derivedStateOf {
            if (isHovered) Windows10Colors.TaskbarHover else Color.Transparent
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .background(backgroundColor, Windows10Shapes.Medium)
            .clip(Windows10Shapes.Medium)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = onClick,
                    onLongPress = onLongClick
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 图标
            Box(
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = Windows10Typography.TitleMedium,
                    color = Windows10Colors.TextPrimary,
                    modifier = Modifier.fillMaxSize().align(Alignment.Center)
                )
            }
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
 * 搜索结果列表
 */
@Composable
fun SearchResultsList(
    apps: List<DesktopItem>,
    onAppClick: (DesktopItem) -> Unit,
    onAppLongClick: (DesktopItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = "无结果",
                    tint = Windows10Colors.TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "未找到匹配的应用",
                    style = Windows10Typography.BodyMedium,
                    color = Windows10Colors.TextSecondary
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps) { app ->
                SearchResultItem(
                    app = app,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    app: DesktopItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
            .height(56.dp)
            .background(backgroundColor, Windows10Shapes.Small)
            .clip(Windows10Shapes.Small)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = onClick,
                    onLongPress = onLongClick
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(modifier = Modifier.size(32.dp)) {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = Windows10Typography.LabelMedium,
                    color = Windows10Colors.TextPrimary,
                    modifier = Modifier.fillMaxSize().align(Alignment.Center)
                )
            }
            Spacer(Modifier.width(12.dp))
            // 信息
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = app.label,
                    style = Windows10Typography.BodyMedium,
                    color = Windows10Colors.TextPrimary
                )
                if (app.packageName.isNotBlank()) {
                    Text(
                        text = app.packageName,
                        style = Windows10Typography.Caption,
                        color = Windows10Colors.TextSecondary
                    )
                }
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
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, start = 4.dp)
    )
}