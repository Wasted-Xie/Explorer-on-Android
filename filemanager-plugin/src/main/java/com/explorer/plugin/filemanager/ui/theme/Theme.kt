// filemanager-plugin/src/main/java/com/explorer/plugin/filemanager/ui/theme/Theme.kt
package com.explorer.plugin.filemanager.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    primaryVariant = Color(0xFF3700B3),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF03DAC5),
    secondaryVariant = Color(0xFF018786),
    onSecondary = Color(0xFF000000),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    primaryVariant = Color(0xFF3700B3),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF03DAC5),
    secondaryVariant = Color(0xFF018786),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0)
)

@Composable
fun FileManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

private val Typography = Typography(
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal)
)