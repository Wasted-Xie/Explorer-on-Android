// host/src/main/java/com/explorer/launcher/host/ui/theme/Theme.kt
package com.explorer.launcher.host.ui.theme

import androidx.compose.runtime.Composable

/**
 * Windows 10 风格主题入口
 * 实际实现见 Windows10Theme.kt
 */
@Composable
fun ExplorerLauncherTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    Windows10Theme(darkTheme = darkTheme, content = content)
}