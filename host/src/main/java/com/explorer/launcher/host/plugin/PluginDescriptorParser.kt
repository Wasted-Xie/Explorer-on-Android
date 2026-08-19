// host/src/main/java/com/explorer/launcher/host/plugin/PluginDescriptorParser.kt
package com.explorer.launcher.host.plugin

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Xml
import android.util.Log
import android.util.Xml as UtilXml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

/**
 * 解析插件的 plugin_descriptor.xml 元数据
 */
object PluginDescriptorParser {
    private const val TAG = "PluginDescriptorParser"
    private const val NS_ANDROID = "http://schemas.android.com/apk/res/android"

    /**
     * 从插件包中解析描述符
     */
    @Throws(PackageManager.NameNotFoundException::class, IOException::class, XmlPullParserException::class)
    fun parse(context: Context, packageName: String): PluginInfo {
        val pm = context.packageManager
        val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_META_DATA or PackageManager.GET_SERVICES)

        // 查找声明了 BIND_PLUGIN_SERVICE intent-filter 的服务
        val serviceInfo = findPluginService(pkgInfo)
            ?: throw IllegalArgumentException("Package $packageName does not declare a plugin service")

        // 读取 meta-data 引用的 XML 资源
        val metaData = serviceInfo.metaData
        val xmlResId = metaData?.getInt("com.explorercore.plugin") ?: 0
        if (xmlResId == 0) {
            throw IllegalArgumentException("Plugin service missing meta-data com.explorercore.plugin")
        }

        val parser = context.resources.getXml(xmlResId)
        return parseXml(parser, packageName, serviceInfo.name)
    }

    /**
     * 查找插件服务组件
     */
    private fun findPluginService(pkgInfo: android.content.pm.PackageInfo): android.content.pm.ServiceInfo? {
        pkgInfo.services?.let { services ->
            for (service in services) {
                val filters = service.intentFilters
                if (filters != null) {
                    for (filter in filters) {
                        if (filter.hasAction("com.explorercore.plugin.BIND_PLUGIN_SERVICE")) {
                            return service
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * 解析 XML 内容
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseXml(parser: XmlPullParser, packageName: String, serviceName: String): PluginInfo {
        var eventType = parser.eventType
        var pluginId = ""
        var pluginName = ""
        var version = "1.0.0"
        var entrypoint = ""
        var minHostVersion = "1.0"
        val requestedPermissions = mutableListOf<String>()
        val extensions = mutableListOf<PluginInfo.ExtensionPoint>()
        var labelRes = 0
        var iconRes = 0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "plugin" -> {
                            pluginId = parser.getAttributeValue(null, "id") ?: ""
                            pluginName = parser.getAttributeValue(null, "name") ?: ""
                            version = parser.getAttributeValue(null, "version") ?: "1.0.0"
                            entrypoint = parser.getAttributeValue(null, "entrypoint") ?: ""
                            minHostVersion = parser.getAttributeValue(null, "minHostVersion") ?: "1.0"
                            labelRes = parser.getAttributeResourceValue(NS_ANDROID, "label", 0)
                            iconRes = parser.getAttributeResourceValue(NS_ANDROID, "icon", 0)
                        }
                        "permission" -> {
                            val perm = parser.getAttributeValue(NS_ANDROID, "name")
                            if (perm != null) requestedPermissions.add(perm)
                        }
                        "extension" -> {
                            val point = parser.getAttributeValue(null, "point")
                            if (point != null) {
                                val ext = PluginInfo.ExtensionPoint.values()
                                    .firstOrNull { it.key == point }
                                if (ext != null) extensions.add(ext)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        // 验证必填字段
        if (pluginId.isBlank()) throw IllegalArgumentException("Plugin id is empty")
        if (entrypoint.isBlank()) throw IllegalArgumentException("Plugin entrypoint is empty")

        return PluginInfo(
            id = pluginId,
            name = pluginName,
            version = version,
            entrypoint = entrypoint,
            minHostVersion = minHostVersion,
            packageName = packageName,
            serviceName = serviceName,
            requestedPermissions = requestedPermissions,
            extensions = extensions,
            labelRes = labelRes,
            iconRes = iconRes
        )
    }

    /**
     * 从 InputStream 解析（用于测试或外部 XML）
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseFromStream(inputStream: InputStream): PluginInfo {
        val parser = UtilXml.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        // 需要包名和服务名，这里暂时用占位符
        return parseXml(parser, "unknown", "unknown")
    }
}