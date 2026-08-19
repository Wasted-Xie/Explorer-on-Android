// host/src/main/java/com/explorer/launcher/host/ui/theme/Windows10Theme.kt
package com.explorer.launcher.host.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp

/**
 * Windows 10 风格配色方案
 */
object Windows10Colors {
    // Windows 10 蓝
    val WindowsBlue = Color(0xFF0078D7)
    val WindowsBlueLight = Color(0xFF0099FF)
    val WindowsBlueDark = Color(0xFF005A9E)

    // 任务栏颜色
    val TaskbarBackground = Color(0xFF101010)          // 深色模式任务栏
    val TaskbarBackgroundLight = Color(0xFFF2F2F2)     // 浅色模式任务栏
    val TaskbarHover = Color(0xFF3E3E42)               // 悬停
    val TaskbarPressed = Color(0xFF0078D7)             // 按下/激活
    val TaskbarDivider = Color(0xFF3F3F46)             // 分割线

    // 开始菜单
    val StartMenuBackground = Color(0xFF1F1F1F)
    val StartMenuBackgroundLight = Color(0xFFFFFFFF)
    val StartMenuTileBackground = Color(0xFF2D2D2D)
    val StartMenuTileBackgroundLight = Color(0xFFF3F3F3)

    // 桌面
    val DesktopBackground = Color(0xFF004F8C)          // 默认蓝色壁纸色
    val DesktopIconBackground = Color(0x00000000)      // 透明
    val DesktopIconHover = Color(0x33FFFFFF)           // 20% 白色覆盖
    val DesktopIconSelected = Color(0x4C0078D7)        // 30% 蓝色覆盖

    // 文本
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFCCCCCC)
    val TextDisabled = Color(0xFF888888)
    val TextOnAccent = Color(0xFFFFFFFF)

    // 搜索框
    val SearchBackground = Color(0xFF2D2D2D)
    val SearchBackgroundLight = Color(0xFFF3F3F3)
    val SearchBorder = Color(0xFF3F3F46)
    val SearchFocusBorder = Color(0xFF0078D7)

    // 系统托盘
    val TrayBackground = Color(0xFF101010)
    val TrayBackgroundLight = Color(0xFFF2F2F2)
    val TrayItemHover = Color(0xFF3E3E42)

    // 通用
    val CardBackground = Color(0xFF1F1F1F)
    val CardBackgroundLight = Color(0xFFFFFFFF)
    val Divider = Color(0xFF3F3F46)
    val FocusRing = Color(0xFF0078D7)
    val Error = Color(0xFFE81123)
    val Success = Color(0xFF107C10)
    val Warning = Color(0xFFCC6A00)
}

/**
 * Windows 10 风格阴影
 */
object Windows10Shadows {
    val CardElevation = Shadow(
        ambient = Color(0x33000000),
        spot = Color(0x1A000000),
        blurRadius = 8.dp
    )
    val MenuElevation = Shadow(
        ambient = Color(0x4D000000),
        spot = Color(0x33000000),
        blurRadius = 16.dp
    )
    val FlyoutElevation = Shadow(
        ambient = Color(0x40000000),
        spot = Color(0x26000000),
        blurRadius = 24.dp
    )
}

/**
 * Windows 10 风格圆角
 */
object Windows10Shapes {
    val Small = androidx.compose.ui.graphics.RectangleShape
    val Medium = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    val Large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    val Pill = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
}

/**
 * Windows 10 风格排版
 */
object Windows10Typography {
    val TitleLarge = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 32.sp,
        letterSpacing = 0.sp,
        lineHeight = 40.sp
    )
    val TitleMedium = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 28.sp
    )
    val BodyLarge = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 20.sp
    )
    val BodyMedium = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 18.sp
    )
    val LabelLarge = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    )
    val LabelMedium = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp
    )
    val Caption = TextStyle(
        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 14.sp
    )
}

/**
 * Windows 10 深色主题配色
 */
val DarkWindows10ColorScheme = darkColorScheme(
    primary = Windows10Colors.WindowsBlue,
    primaryContainer = Windows10Colors.WindowsBlueDark,
    onPrimary = Windows10Colors.TextOnAccent,
    onPrimaryContainer = Windows10Colors.TextPrimary,
    secondary = Windows10Colors.WindowsBlueLight,
    secondaryContainer = Windows10Colors.WindowsBlue,
    onSecondary = Windows10Colors.TextOnAccent,
    onSecondaryContainer = Windows10Colors.TextPrimary,
    tertiary = Windows10Colors.Success,
    tertiaryContainer = Windows10Colors.Success,
    onTertiary = Windows10Colors.TextOnAccent,
    onTertiaryContainer = Windows10Colors.TextOnAccent,
    error = Windows10Colors.Error,
    errorContainer = Color(0xFF5C1A1D),
    onError = Windows10Colors.TextOnAccent,
    onErrorContainer = Windows10Colors.TextPrimary,
    background = Windows10Colors.DesktopBackground,
    onBackground = Windows10Colors.TextPrimary,
    surface = Windows10Colors.CardBackground,
    onSurface = Windows10Colors.TextPrimary,
    surfaceVariant = Windows10Colors.TaskbarBackground,
    onSurfaceVariant = Windows10Colors.TextSecondary,
    outline = Windows10Colors.Divider,
    outlineVariant = Windows10Colors.Divider.copy(alpha = 0.5f),
    shadow = Windows10Colors.TaskbarBackground.copy(alpha = 0.8f),
    scrim = Windows10Colors.TaskbarBackground.copy(alpha = 0.8f),
    inverseSurface = Windows10Colors.CardBackgroundLight,
    inverseOnSurface = Color.Black,
    inversePrimary = Windows10Colors.WindowsBlue
)

/**
 * Windows 10 浅色主题配色
 */
val LightWindows10ColorScheme = lightColorScheme(
    primary = Windows10Colors.WindowsBlue,
    primaryContainer = Color(0xFFD6E4F0),
    onPrimary = Windows10Colors.TextOnAccent,
    onPrimaryContainer = Color.Black,
    secondary = Windows10Colors.WindowsBlueDark,
    secondaryContainer = Color(0xFFD6E4F0),
    onSecondary = Windows10Colors.TextOnAccent,
    onSecondaryContainer = Color.Black,
    tertiary = Windows10Colors.Success,
    tertiaryContainer = Color(0xFFD8F5DD),
    onTertiary = Windows10Colors.TextOnAccent,
    onTertiaryContainer = Color.Black,
    error = Windows10Colors.Error,
    errorContainer = Color(0xFFFDEDEE),
    onError = Windows10Colors.TextOnAccent,
    onErrorContainer = Color.Black,
    background = Color(0xFFF2F2F2),
    onBackground = Color.Black,
    surface = Windows10Colors.CardBackgroundLight,
    onSurface = Color.Black,
    surfaceVariant = Windows10Colors.TaskbarBackgroundLight,
    onSurfaceVariant = Color(0xFF333333),
    outline = Windows10Colors.Divider,
    outlineVariant = Windows10Colors.Divider.copy(alpha = 0.5f),
    shadow = Color.Black.copy(alpha = 0.1f),
    scrim = Color.Black.copy(alpha = 0.3f),
    inverseSurface = Windows10Colors.CardBackground,
    inverseOnSurface = Windows10Colors.TextPrimary,
    inversePrimary = Windows10Colors.WindowsBlueLight
)

/**
 * Windows 10 主题入口
 */
@Composable
fun Windows10Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkWindows10ColorScheme else LightWindows10ColorScheme
    val shapes = Windows10Shapes
    val typography = Windows10Typography

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = typography,
        content = content
    )
}

/**
 * 获取任务栏背景色
 */
@Composable
fun TaskbarBackgroundColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.isDark) Windows10Colors.TaskbarBackground else Windows10Colors.TaskbarBackgroundLight
}

/**
 * 获取搜索框背景色
 */
@Composable
fun SearchBackgroundColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.isDark) Windows10Colors.SearchBackground else Windows10Colors.SearchBackgroundLight
}