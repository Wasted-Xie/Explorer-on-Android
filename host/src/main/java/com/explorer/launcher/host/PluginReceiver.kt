// host/src/main/java/com/explorer/launcher/host/PluginReceiver.kt
package com.explorer.launcher.host

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PluginReceiver : BroadcastReceiver() {
    private const val TAG = "PluginReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val packageName = intent.data?.schemeSpecificPart

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    Log.d(TAG, "Package added: $packageName, checking for plugins...")
                    // 触发插件重新扫描
                    HostApplication.instance.pluginManager.refreshPlugins()
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    Log.d(TAG, "Package removed: $packageName")
                    // 通知 PluginManager 移除该插件
                    HostApplication.instance.pluginManager.removePlugin(packageName)
                }
            }
            Intent.ACTION_PACKAGE_CHANGED -> {
                Log.d(TAG, "Package changed: $packageName")
                // 重新解析该插件
                HostApplication.instance.pluginManager.refreshPlugin(packageName)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Package replaced: $packageName")
                HostApplication.instance.pluginManager.refreshPlugin(packageName)
            }
        }
    }
}