// host/src/main/java/com/explorer/launcher/host/plugin/PluginInfo.kt
package com.explorer.launcher.host.plugin

import android.content.pm.ServiceInfo
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import com.explorercore.plugin.PluginContract
import kotlinx.serialization.Serializable

/**
 * 插件元数据模型，对应 plugin_descriptor.xml 中的声明
 */
@Serializable
data class PluginInfo(
    val id: String,                    // 唯一标识：com.explorer.plugin.filemanager
    val name: String,                  // 显示名称：文件管理器
    val version: String,               // 版本号：1.0.0
    val entrypoint: String,            // 服务类全名：com.explorer.plugin.filemanager.FileManagerPluginService
    val minHostVersion: String = "1.0", // 最低宿主版本
    val packageName: String,           // 插件应用包名
    val serviceName: String,           // 服务类名（用于 Intent）
    val requestedPermissions: List<String> = emptyList(), // 请求的权限
    val extensions: List<ExtensionPoint> = emptyList(),   // 实现的扩展点
    val labelRes: Int = 0,             // 标签资源 ID（可选）
    val iconRes: Int = 0,              // 图标资源 ID（可选）
    var isEnabled: Boolean = true,     // 是否启用
    var isLoaded: Boolean = false      // 服务是否已绑定
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() !!,
        name = parcel.readString() !!,
        version = parcel.readString() !!,
        entrypoint = parcel.readString() !!,
        minHostVersion = parcel.readString() !!,
        packageName = parcel.readString() !!,
        serviceName = parcel.readString() !!,
        requestedPermissions = parcel.createStringArrayList() ?: emptyList(),
        extensions = parcel.createTypedArrayList(ExtensionPoint.CREATOR) ?: emptyList(),
        labelRes = parcel.readInt(),
        iconRes = parcel.readInt(),
        isEnabled = parcel.readByte() != 0.toByte(),
        isLoaded = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(name)
        dest.writeString(version)
        dest.writeString(entrypoint)
        dest.writeString(minHostVersion)
        dest.writeString(packageName)
        dest.writeString(serviceName)
        dest.writeStringList(requestedPermissions)
        dest.writeTypedList(extensions)
        dest.writeInt(labelRes)
        dest.writeInt(iconRes)
        dest.writeByte(if (isEnabled) 1 else 0)
        dest.writeByte(if (isLoaded) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PluginInfo> = object : Parcelable.Creator<PluginInfo> {
            override fun createFromParcel(parcel: Parcel): PluginInfo = PluginInfo(parcel)
            override fun newArray(size: Int): Array<PluginInfo?> = arrayOfNulls(size)
        }
    }

    /**
     * 扩展点类型
     */
    enum class ExtensionPoint(val key: String) : Parcelable {
        PANEL("panel"),
        SHORTCUT("shortcut"),
        SEARCH("search"),
        TRAY("tray"),
        SETTINGS("settings"),
        WALLPAPER("wallpaper"),
        GESTURE("gesture");

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(key)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<ExtensionPoint> = object : Parcelable.Creator<ExtensionPoint> {
                override fun createFromParcel(parcel: Parcel): ExtensionPoint {
                    return ExtensionPoint.values().firstOrNull { it.key == parcel.readString() }
                        ?: PANEL
                }
                override fun newArray(size: Int): Array<ExtensionPoint?> = arrayOfNulls(size)
            }
        }
    }

    /**
     * 检查是否实现了某扩展点
     */
    fun hasExtension(ext: ExtensionPoint): Boolean = extensions.contains(ext)

    /**
     * 版本比较：返回 true 如果当前版本 >= requiredVersion
     */
    fun satisfiesVersion(requiredVersion: String): Boolean {
        return compareVersions(this.version, requiredVersion) >= 0
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val n1 = parts1.getOrElse(i) { 0 }
            val n2 = parts2.getOrElse(i) { 0 }
            if (n1 != n2) return n1.compareTo(n2)
        }
        return 0
    }
}

/**
 * 插件运行时状态
 */
data class PluginRuntimeState(
    val pluginId: String,
    var isBound: Boolean = false,
    var service: com.explorercore.plugin.IPluginService? = null,
    var hostCallbacks: com.explorercore.plugin.IHostCallbacks? = null,
    var lastError: String? = null,
    var boundTime: Long = 0
)