// host/src/main/java/com/explorer/launcher/host/model/DesktopItem.kt
package com.explorer.launcher.host.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * 桌面项类型
 */
enum class DesktopItemType {
    APP,            // 应用快捷方式
    FOLDER,         // 文件夹
    WIDGET,         // 小工具
    SHORTCUT,       // 文件/网址快捷方式
    PLUGIN_PANEL    // 插件面板入口
}

/**
 * 桌面项基础模型
 */
@Serializable
data class DesktopItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: DesktopItemType,
    val label: String,
    val iconRes: Int = 0,                    // 资源图标 ID
    val iconUri: String = "",                // 图标 URI (content://, file://, android.resource://)
    val packageName: String = "",            // 应用包名
    val className: String = "",              // Activity 类名
    val intentAction: String = "",           // Intent Action
    val intentData: String = "",             // Intent Data URI
    val extras: Map<String, String> = emptyMap(), // 透传参数
    val position: Int = -1,                  // 网格位置 (-1 表示自动)
    val pageIndex: Int = 0,                  // 所在页索引
    val spanX: Int = 1,                      // 横向跨度
    val spanY: Int = 1,                      // 纵向跨度
    val isLocked: Boolean = false,           // 是否锁定位置
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        type = DesktopItemType.values()[parcel.readInt()],
        label = parcel.readString()!!,
        iconRes = parcel.readInt(),
        iconUri = parcel.readString()!!,
        packageName = parcel.readString()!!,
        className = parcel.readString()!!,
        intentAction = parcel.readString()!!,
        intentData = parcel.readString()!!,
        extras = parcel.readHashMap(String::class.javaPrimitiveType, String::class.javaPrimitiveType) ?: emptyMap(),
        position = parcel.readInt(),
        pageIndex = parcel.readInt(),
        spanX = parcel.readInt(),
        spanY = parcel.readInt(),
        isLocked = parcel.readByte() != 0.toByte(),
        createdAt = parcel.readLong(),
        updatedAt = parcel.readLong()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeInt(type.ordinal)
        dest.writeString(label)
        dest.writeInt(iconRes)
        dest.writeString(iconUri)
        dest.writeString(packageName)
        dest.writeString(className)
        dest.writeString(intentAction)
        dest.writeString(intentData)
        dest.writeMap(extras)
        dest.writeInt(position)
        dest.writeInt(pageIndex)
        dest.writeInt(spanX)
        dest.writeInt(spanY)
        dest.writeByte(if (isLocked) 1 else 0)
        dest.writeLong(createdAt)
        dest.writeLong(updatedAt)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DesktopItem> = object : Parcelable.Creator<DesktopItem> {
            override fun createFromParcel(parcel: Parcel): DesktopItem = DesktopItem(parcel)
            override fun newArray(size: Int): Array<DesktopItem?> = arrayOfNulls(size)
        }
    }

    /** 创建应用快捷方式 */
    companion object {
        fun createAppShortcut(
            packageName: String,
            className: String,
            label: String,
            iconRes: Int = 0,
            iconUri: String = ""
        ): DesktopItem {
            return DesktopItem(
                type = DesktopItemType.APP,
                label = label,
                iconRes = iconRes,
                iconUri = iconUri,
                packageName = packageName,
                className = className
            )
        }

        /** 创建插件面板入口 */
        fun createPluginPanel(
            pluginId: String,
            label: String,
            iconUri: String = ""
        ): DesktopItem {
            return DesktopItem(
                type = DesktopItemType.PLUGIN_PANEL,
                label = label,
                iconUri = iconUri,
                intentAction = "com.explorer.launcher.ACTION_OPEN_PLUGIN_PANEL",
                extras = mapOf("plugin_id" to pluginId)
            )
        }

        /** 创建文件夹 */
        fun createFolder(label: String = "文件夹"): DesktopItem {
            return DesktopItem(
                type = DesktopItemType.FOLDER,
                label = label,
                iconUri = "android.resource://com.explorer.launcher.host/drawable/ic_folder"
            )
        }
    }

    /** 复制并更新位置 */
    fun copyWithPosition(newPosition: Int, newPageIndex: Int = pageIndex): DesktopItem {
        return copy(position = newPosition, pageIndex = newPageIndex, updatedAt = System.currentTimeMillis())
    }

    /** 复制并更新标签 */
    fun copyWithLabel(newLabel: String): DesktopItem {
        return copy(label = newLabel, updatedAt = System.currentTimeMillis())
    }
}

/**
 * 文件夹模型（包含多个桌面项）
 */
@Serializable
data class Folder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val items: List<DesktopItem> = emptyList(),
    val backgroundColor: Int = 0xFF333333,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        name = parcel.readString()!!,
        items = parcel.createTypedArrayList(DesktopItem.CREATOR) ?: emptyList(),
        backgroundColor = parcel.readInt(),
        createdAt = parcel.readLong(),
        updatedAt = parcel.readLong()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(name)
        dest.writeTypedList(items)
        dest.writeInt(backgroundColor)
        dest.writeLong(createdAt)
        dest.writeLong(updatedAt)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Folder> = object : Parcelable.Creator<Folder> {
            override fun createFromParcel(parcel: Parcel): Folder = Folder(parcel)
            override fun newArray(size: Int): Array<Folder?> = arrayOfNulls(size)
        }
    }

    fun addItem(item: DesktopItem): Folder {
        return copy(items = items + item, updatedAt = System.currentTimeMillis())
    }

    fun removeItem(itemId: String): Folder {
        return copy(items = items.filter { it.id != itemId }, updatedAt = System.currentTimeMillis())
    }
}

/**
 * 壁纸模型
 */
@Serializable
data class Wallpaper(
    val id: String,
    val name: String,
    val type: WallpaperType,
    val uri: String,                    // 图片 URI 或颜色值
    val isSystem: Boolean = false,      // 是否系统内置
    val previewUri: String = ""         // 预览图 URI
) : Parcelable {
    enum class WallpaperType {
        STATIC,         // 静态图片
        LIVE,           // 动态壁纸
        SOLID_COLOR,    // 纯色
        DAILY_BING      // 每日必应
    }

    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        name = parcel.readString()!!,
        type = WallpaperType.values()[parcel.readInt()],
        uri = parcel.readString()!!,
        isSystem = parcel.readByte() != 0.toByte(),
        previewUri = parcel.readString()!!
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(name)
        dest.writeInt(type.ordinal)
        dest.writeString(uri)
        dest.writeByte(if (isSystem) 1 else 0)
        dest.writeString(previewUri)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Wallpaper> = object : Parcelable.Creator<Wallpaper> {
            override fun createFromParcel(parcel: Parcel): Wallpaper = Wallpaper(parcel)
            override fun newArray(size: Int): Array<Wallpaper?> = arrayOfNulls(size)
        }
    }
}

/**
 * 桌面布局配置
 */
@Serializable
data class DesktopLayout(
    val gridColumns: Int = 5,
    val gridRows: Int = 6,
    val iconSize: Int = 96,             // dp
    val iconSpacing: Int = 16,          // dp
    val labelTextSize: Int = 12,        // sp
    val showLabels: Boolean = true,
    val allowRotation: Boolean = false,
    val pageCount: Int = 3,
    val defaultPage: Int = 1
) : Parcelable {
    constructor(parcel: Parcel) : this(
        gridColumns = parcel.readInt(),
        gridRows = parcel.readInt(),
        iconSize = parcel.readInt(),
        iconSpacing = parcel.readInt(),
        labelTextSize = parcel.readInt(),
        showLabels = parcel.readByte() != 0.toByte(),
        allowRotation = parcel.readByte() != 0.toByte(),
        pageCount = parcel.readInt(),
        defaultPage = parcel.readInt()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(gridColumns)
        dest.writeInt(gridRows)
        dest.writeInt(iconSize)
        dest.writeInt(iconSpacing)
        dest.writeInt(labelTextSize)
        dest.writeByte(if (showLabels) 1 else 0)
        dest.writeByte(if (allowRotation) 1 else 0)
        dest.writeInt(pageCount)
        dest.writeInt(defaultPage)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DesktopLayout> = object : Parcelable.Creator<DesktopLayout> {
            override fun createFromParcel(parcel: Parcel): DesktopLayout = DesktopLayout(parcel)
            override fun newArray(size: Int): Array<DesktopLayout?> = arrayOfNulls(size)
        }
    }
}

/**
 * 任务栏配置
 */
@Serializable
data class TaskbarConfig(
    val position: TaskbarPosition = TaskbarPosition.BOTTOM,
    val height: Int = 48,               // dp
    val iconSize: Int = 24,             // dp
    val showStartButton: Boolean = true,
    val showSearchBox: Boolean = true,
    val showTaskView: Boolean = true,
    val showWidgets: Boolean = true,
    val pinnedApps: List<String> = emptyList(),  // 固定应用包名列表
    val autoHide: Boolean = false,
    val useSmallIcons: Boolean = false,
    val combineButtons: Boolean = true
) : Parcelable {
    enum class TaskbarPosition {
        BOTTOM, TOP, LEFT, RIGHT
    }

    constructor(parcel: Parcel) : this(
        position = TaskbarPosition.values()[parcel.readInt()],
        height = parcel.readInt(),
        iconSize = parcel.readInt(),
        showStartButton = parcel.readByte() != 0.toByte(),
        showSearchBox = parcel.readByte() != 0.toByte(),
        showTaskView = parcel.readByte() != 0.toByte(),
        showWidgets = parcel.readByte() != 0.toByte(),
        pinnedApps = parcel.createStringArrayList() ?: emptyList(),
        autoHide = parcel.readByte() != 0.toByte(),
        useSmallIcons = parcel.readByte() != 0.toByte(),
        combineButtons = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(position.ordinal)
        dest.writeInt(height)
        dest.writeInt(iconSize)
        dest.writeByte(if (showStartButton) 1 else 0)
        dest.writeByte(if (showSearchBox) 1 else 0)
        dest.writeByte(if (showTaskView) 1 else 0)
        dest.writeByte(if (showWidgets) 1 else 0)
        dest.writeStringList(pinnedApps)
        dest.writeByte(if (autoHide) 1 else 0)
        dest.writeByte(if (useSmallIcons) 1 else 0)
        dest.writeByte(if (combineButtons) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TaskbarConfig> = object : Parcelable.Creator<TaskbarConfig> {
            override fun createFromParcel(parcel: Parcel): TaskbarConfig = TaskbarConfig(parcel)
            override fun newArray(size: Int): Array<TaskbarConfig?> = arrayOfNulls(size)
        }
    }
}