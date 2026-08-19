# Explorer Launcher 插件开发标准接入文档

> **版本**: 1.0  
> **适用宿主版本**: >= 1.0  
> **最后更新**: 2024

---

## 1. 概述

Explorer Launcher 采用**插件化架构**，核心宿主仅提供桌面基础能力（图标网格、应用抽屉、搜索栏、任务栏、开始菜单），所有垂直领域功能（文件管理、天气、日历、系统监控等）均以**独立插件**形式接入。

插件本质是一个**独立的 Android 应用**（拥有自己的 `applicationId`、进程、权限），通过 **AIDL 定义的标准契约** 与宿主通信。宿主负责发现、加载、生命周期管理、UI 容器提供、权限代理、事件分发。

### 核心优势
- **隔离性**：插件崩溃不影响宿主，权限最小化
- **可替换性**：用户可自由安装/卸载/禁用插件
- **生态开放**：第三方开发者无需宿主源码即可开发插件
- **热更新**：插件可独立发布更新，无需宿主重新编译

---

## 2. 插件基础结构

### 2.1 模块组织
```
MyPlugin/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── aidl/com/explorercore/plugin/     # 复制宿主 AIDL 文件（或依赖 plugin-api 模块）
│   ├── java/com/example/myplugin/
│   │   ├── MyPluginApplication.kt
│   │   ├── MyPluginService.kt            # 实现 IPluginService
│   │   └── ui/                           # 可选：Compose/View UI
│   └── res/
│       ├── values/strings.xml
│       └── xml/plugin_descriptor.xml     # 必须：插件元数据
```

### 2.2 Gradle 依赖
```kotlin
// build.gradle.kts (插件模块)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.myplugin"  // 唯一标识
        minSdk = 21
        targetSdk = 34
    }
    buildFeatures { aidl = true }  // 启用 AIDL
}

dependencies {
    // 方式 A：依赖发布到 Maven 的 plugin-api 库
    implementation("com.explorer.launcher:plugin-api:1.0.0")
    
    // 方式 B：本地复制 AIDL 文件到 src/main/aidl/com/explorercore/plugin/
    // 则无需额外依赖
    
    // UI 依赖（按需）
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.material3:material3:1.2.1")
}
```

---

## 3. 插件清单：plugin_descriptor.xml

**位置**：`src/main/res/xml/plugin_descriptor.xml`  
**引用方式**：在 `AndroidManifest.xml` 的 `<service>` 中声明 `<meta-data>` 指向此资源。

```xml
<?xml version="1.0" encoding="utf-8"?>
<plugin xmlns:android="http://schemas.android.com/apk/res/android"
    id="com.example.myplugin"                 // 唯一 ID，建议与 applicationId 一致
    name="我的插件"                             // 显示名称
    version="1.0.0"                           // 语义化版本
    entrypoint="com.example.myplugin.MyPluginService" // 服务类全名
    minHostVersion="1.0">                     // 最低兼容宿主版本
    
    <!-- 宿主应代理授予的权限（安装时向用户申请） -->
    <permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <permission android:name="android.permission.CAMERA"/>
    
    <!-- 实现的扩展点（至少一个） -->
    <extension point="panel"/>        // 面板：可停靠在侧边栏/底部/悬浮窗
    <extension point="shortcut"/>     // 快捷方式：桌面图标/应用抽屉条目
    <extension point="search"/>       // 搜索提供者：全局搜索聚合结果
    <extension point="tray"/>         // 系统托盘：任务栏常驻图标
    <extension point="settings"/>     // 设置页面：宿主设置中嵌入
    <extension point="wallpaper"/>    // 动态壁纸：实现 WallpaperService
    <extension point="gesture"/>      // 手势处理：全局手势拦截
</plugin>
```

### 字段说明
| 字段 | 必填 | 说明 |
|------|------|------|
| `id` | ✅ | 全局唯一，建议用反向域名 |
| `name` | ✅ | 用户可见名称，支持字符串资源引用 `@string/xxx` |
| `version` | ✅ | 语义化版本，用于兼容性判断 |
| `entrypoint` | ✅ | 实现 `IPluginService` 的 Service 类全名 |
| `minHostVersion` | ❌ | 默认 `1.0`，宿主版本低于此值将不加载 |
| `permission` | ❌ | 宿主会在插件首次启动时统一申请这些权限 |
| `extension` | ✅ | 至少声明一个扩展点 |

---

## 4. 核心接口：IPluginService

插件**必须**实现此 AIDL 接口的 Service，并在 Manifest 中声明：

```xml
<service
    android:name=".MyPluginService"
    android:exported="true"
    android:permission="android.permission.BIND_JOB_SERVICE">
    <intent-filter>
        <action android:name="com.explorercore.plugin.BIND_PLUGIN_SERVICE"/>
    </intent-filter>
    <meta-data
        android:name="com.explorercore.plugin"
        android:resource="@xml/plugin_descriptor"/>
</service>
```

### 实现模板
```kotlin
// MyPluginService.kt
class MyPluginService : Service() {

    private var hostCallbacks: IHostCallbacks? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MyPluginService = this@MyPluginService
    }

    override fun onBind(intent: Intent): IBinder = binder

    // ========== IPluginService 契约 ==========

    override fun initialize(callbacks: IHostCallbacks) {
        hostCallbacks = callbacks
        // 插件就绪，通知宿主
        callbacks.onPluginEvent(PLUGIN_EVENT_PANEL_READY, Bundle().apply {
            putString("plugin_id", "com.example.myplugin")
        })
    }

    override fun onHostEvent(eventCode: Int, data: Bundle?) {
        when (eventCode) {
            HOST_EVENT_REQUEST_PERMISSION -> {
                // 宿主请求我们申请权限（通常在首次启动时）
                val perms = data?.getStringArrayList(EXTRA_PERMISSIONS) ?: emptyList()
                requestPermissions(perms)
            }
            HOST_EVENT_UI_UPDATE -> {
                // 宿主通知 UI 需要刷新（如主题切换、语言变更）
                refreshUI()
            }
            HOST_EVENT_THEME_CHANGED -> applyTheme(data?.getInt("theme_mode") ?: 0)
            HOST_EVENT_LOW_MEMORY -> clearCaches()
        }
    }

    // ========== 辅助方法 ==========

    private fun requestPermissions(permissions: List<String>) {
        // 插件无 Activity 上下文，需通过宿主代理申请
        // 宿主会在 onPermissionRequested 回调中处理用户授权
        hostCallbacks?.onPluginEvent(PLUGIN_EVENT_REQUEST_PERMISSION, Bundle().apply {
            putString("plugin_id", "com.example.myplugin")
            putStringArrayList(EXTRA_PERMISSIONS, permissions as ArrayList<String>)
        })
    }

    private fun refreshUI() { /* 更新面板/快捷方式 UI */ }
    private fun applyTheme(mode: Int) { /* 适配深色/浅色 */ }
    private fun clearCaches() { /* 释放内存 */ }

    companion object {
        // 事件码常量（与 PluginContract 保持一致）
        const val PLUGIN_EVENT_PANEL_READY = 2000
        const val PLUGIN_EVENT_SHOW_TOAST = 2001
        const val PLUGIN_EVENT_REQUEST_PERMISSION = 2002
        const val PLUGIN_EVENT_PANEL_RESIZE_REQUEST = 2003
        const val PLUGIN_EVENT_SEARCH_QUERY = 2004
        const val HOST_EVENT_REQUEST_PERMISSION = 1000
        const val HOST_EVENT_UI_UPDATE = 1001
        const val HOST_EVENT_THEME_CHANGED = 1002
        const val HOST_EVENT_LOW_MEMORY = 1003
        const val EXTRA_PERMISSIONS = "extra_permissions"
    }
}
```

---

## 5. 扩展点详解

### 5.1 Panel Extension (`point="panel"`)
**用途**：提供可嵌入宿主 UI 的功能面板（如文件管理器主界面、天气详情、系统监控仪表盘）。

**交互流程**：
1. 插件调用 `hostCallbacks.requestPanel(panelId, args)` 请求宿主创建面板容器
2. 宿主返回 `IPanelHost` Binder，插件通过它接收 `onUpdate`/`showToast`/`requestResize`
3. 宿主在指定位置（侧边栏/底部/悬浮窗）渲染面板 UI

**面板 ID 规范**：
| ID | 场景 | 宿主渲染位置 |
|----|------|-------------|
| `main` | 插件主界面 | 侧边栏全高 / 底部半屏 / 悬浮窗 |
| `compact` | 紧凑视图 | 任务栏弹出 / 悬浮球展开 |
| `settings` | 设置子页面 | 宿主设置页面内嵌 |

**插件主动推送更新**：
```kotlin
// 插件内部数据变化时，通知宿主刷新面板
val panelHost: IPanelHost = // 保存 requestPanel 返回的 binder
panelHost.onUpdate(Bundle().apply {
    putString("action", "refresh_file_list")
    putString("path", "/sdcard/Download")
})
```

### 5.2 Shortcut Extension (`point="shortcut"`)
**用途**：在桌面/应用抽屉创建可点击的图标。

**实现**：插件在 `initialize` 中通过宿主回调注册快捷方式信息：
```kotlin
// 宿主提供注册快捷方式的扩展事件（需宿主实现）
val data = Bundle().apply {
    putString("shortcut_id", "open_file_manager")
    putString("label", "文件管理器")
    putString("icon_uri", "android.resource://com.example.myplugin/drawable/ic_folder")
    putString("intent_action", "com.example.myplugin.OPEN_MAIN")
}
hostCallbacks?.onPluginEvent(PLUGIN_EVENT_REGISTER_SHORTCUT, data)
```
> 注：宿主需实现 `PLUGIN_EVENT_REGISTER_SHORTCUT` 事件处理，将快捷方式添加到桌面数据库。

### 5.3 Search Extension (`point="search"`)
**用途**：参与宿主全局搜索，返回自定义结果。

**流程**：
1. 用户在搜索栏输入 → 宿主广播查询给所有 `search` 插件
2. 插件在后台检索 → 调用 `hostCallbacks.notifySearchResults(query, results)`
3. 宿主合并所有插件结果展示

**SearchResultItem 结构**：
```kotlin
data class SearchResultItem(
    val id: String,           // 唯一标识
    val title: String,        // 主标题
    val subtitle: String?,    // 副标题
    val iconUri: String?,     // 图标 URI (content:// / file:// / android.resource://)
    val intentAction: String?,// 点击触发的 Intent Action
    val intentData: String?,  // Intent Data URI
    val extras: Bundle?       // 透传数据
)
```

**插件实现示例**：
```kotlin
// 接收宿主搜索请求（需宿主发送 HOST_EVENT_SEARCH_QUERY 事件）
override fun onHostEvent(eventCode: Int, data: Bundle?) {
    if (eventCode == HOST_EVENT_SEARCH_QUERY) {
        val query = data?.getString(EXTRA_SEARCH_QUERY) ?: ""
        scope.launch(Dispatchers.IO) {
            val results = searchLocalDatabase(query)
            hostCallbacks?.notifySearchResults(query, results)
        }
    }
}
```

### 5.4 Tray Extension (`point="tray"`)
**用途**：任务栏系统托盘区常驻图标（如网络状态、音量、同步状态）。

**能力**：
- 显示图标 + 可选徽标（数字/小红点）
- 点击展开面板 / 单击切换状态
- 长按显示上下文菜单

### 5.5 Settings Extension (`point="settings"`)
**用途**：在宿主设置页面嵌入插件专属设置项。

**实现**：插件提供一个 Compose `@Composable` 函数或 Fragment，宿主在设置列表中嵌入渲染。

### 5.6 Wallpaper Extension (`point="wallpaper"`)
**用途**：提供动态壁纸/锁屏壁纸。

**实现**：标准 Android `WallpaperService`，在插件 Manifest 中声明：
```xml
<service
    android:name=".MyWallpaperService"
    android:permission="android.permission.BIND_WALLPAPER">
    <intent-filter>
        <action android:name="android.service.wallpaper.WallpaperService"/>
    </intent-filter>
    <meta-data
        android:name="android.service.wallpaper"
        android:resource="@xml/wallpaper_definition"/>
</service>
```

### 5.7 Gesture Extension (`point="gesture"`)
**用途**：注册全局手势（如双击桌面锁屏、三指上滑打开任务管理器）。

**事件**：宿主检测到手势 → 发送 `HOST_EVENT_GESTURE_DETECTED` 给相关插件。

---

## 6. 通信协议与事件码规范

### 6.1 事件码分配
| 范围 | 方向 | 用途 |
|------|------|------|
| 1000-1999 | 宿主 → 插件 | 宿主主动通知/请求 |
| 2000-2999 | 插件 → 宿主 | 插件主动上报/请求 |
| 3000-3999 | 双向 | 扩展点专用 |
| 10000+ | 自定义 | 插件私有协议 |

### 6.2 标准事件码（PluginContract）
```kotlin
// 宿主 → 插件 (1000-1999)
const val HOST_EVENT_REQUEST_PERMISSION = 1000  // 请求插件申请权限
const val HOST_EVENT_UI_UPDATE = 1001           // UI 刷新（主题/语言/方向）
const val HOST_EVENT_THEME_CHANGED = 1002       // 主题模式变更 (0=跟随系统/1=浅色/2=深色)
const val HOST_EVENT_LOW_MEMORY = 1003          // 低内存警告
const val HOST_EVENT_SEARCH_QUERY = 1004        // 搜索查询分发
const val HOST_EVENT_GESTURE_DETECTED = 1005    // 手势触发

// 插件 → 宿主 (2000-2999)
const val PLUGIN_EVENT_PANEL_READY = 2000       // 面板就绪
const val PLUGIN_EVENT_SHOW_TOAST = 2001        // 请求显示 Toast
const val PLUGIN_EVENT_REQUEST_PERMISSION = 2002 // 请求宿主代理申请权限
const val PLUGIN_EVENT_PANEL_RESIZE_REQUEST = 2003 // 请求面板尺寸变更
const val PLUGIN_EVENT_SEARCH_QUERY = 2004      // 插件主动发起搜索
const val PLUGIN_EVENT_REGISTER_SHORTCUT = 2005 // 注册快捷方式
const val PLUGIN_EVENT_UNREGISTER_SHORTCUT = 2006 // 注销快捷方式
```

### 6.3 Bundle 标准键名
```kotlin
const val EXTRA_PERMISSIONS = "extra_permissions"       // StringArrayList
const val EXTRA_GRANT_RESULTS = "extra_grant_results"   // IntegerArrayList (0=granted)
const val EXTRA_SEARCH_QUERY = "extra_search_query"     // String
const val EXTRA_PANEL_ID = "extra_panel_id"             // String
const val EXTRA_SHORTCUT_ID = "extra_shortcut_id"       // String
const val EXTRA_GESTURE_TYPE = "extra_gesture_type"     // String
```

---

## 7. 权限模型

### 7.1 权限代理机制
1. 插件在 `plugin_descriptor.xml` 声明所需权限
2. 宿主在插件首次绑定时，统一弹窗请求用户授权
3. 授权后，宿主通过以下方式将权限“传递”给插件：
   - **存储权限**：通过 `ContentProvider` 临时授权 `content://` URI
   - **位置/相机等**：插件自行在自身进程中申请（已获得用户授权）
   - **特殊权限** (`MANAGE_EXTERNAL_STORAGE`)：宿主引导用户进入系统设置页

### 7.2 权限最小化原则
- 仅声明**运行时必需**的权限
- 运行时按需申请（如首次访问相册时再申请 `READ_MEDIA_IMAGES`）
- 敏感权限在插件设置页提供开关说明

---

## 8. 生命周期管理

```
宿主启动
   │
   ├─► 扫描已安装应用 (queryIntentServices BIND_PLUGIN_SERVICE)
   │
   ├─► 解析 plugin_descriptor.xml → PluginInfo 缓存
   │
   ├─► 版本兼容检查 (minHostVersion <= 宿主版本)
   │
   ├─► bindService() 连接插件 IPluginService
   │       │
   │       ├─► onServiceConnected
   │       │       │
   │       │       └─► plugin.initialize(hostCallbacks)  ← 关键：传递宿主回调
   │       │
   │       └─► 插件内部初始化（数据库、监听器、UI 状态恢复）
   │
   ├─► 插件回调 PLUGIN_EVENT_PANEL_READY → 宿主更新 UI 列表
   │
   ├─► 用户交互触发面板/快捷方式/搜索
   │
   └─► 卸载/禁用/更新 → 宿主收到广播 → unbindService/refreshPlugin
```

### 关键回调时机
| 时机 | 插件应做的事 |
|------|-------------|
| `initialize()` | 保存 `hostCallbacks`、注册观察者、恢复状态、上报 `PANEL_READY` |
| `onHostEvent(HOST_EVENT_UI_UPDATE)` | 刷新 UI、应用新主题 |
| `onHostEvent(HOST_EVENT_LOW_MEMORY)` | 清理图片缓存、释放大对象 |
| 服务解绑 (`onServiceDisconnected`) | 保存状态、停止后台任务、释放资源 |

---

## 9. 完整示例：最简插件

### 9.1 目录结构
```
SimplePlugin/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   ├── aidl/com/explorercore/plugin/
│   │   ├── IPluginService.aidl
│   │   ├── IHostCallbacks.aidl
│   │   ├── IPanelHost.aidl
│   │   └── SearchResultItem.aidl
│   ├── java/com/example/simpleplugin/
│   │   ├── SimplePluginApplication.kt
│   │   ├── SimplePluginService.kt
│   │   └── ui/SimplePanel.kt
│   └── res/
│       ├── values/strings.xml
│       └── xml/plugin_descriptor.xml
```

### 9.2 关键代码

**AndroidManifest.xml**
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.simpleplugin">

    <application
        android:name=".SimplePluginApplication"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher">

        <service
            android:name=".SimplePluginService"
            android:exported="true"
            android:permission="android.permission.BIND_JOB_SERVICE">
            <intent-filter>
                <action android:name="com.explorercore.plugin.BIND_PLUGIN_SERVICE"/>
            </intent-filter>
            <meta-data
                android:name="com.explorercore.plugin"
                android:resource="@xml/plugin_descriptor"/>
        </service>

        <!-- 可选：直接启动的 Activity（桌面图标点击） -->
        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SimplePlugin">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
```

**plugin_descriptor.xml**
```xml
<plugin xmlns:android="http://schemas.android.com/apk/res/android"
    id="com.example.simpleplugin"
    name="简易示例"
    version="1.0.0"
    entrypoint="com.example.simpleplugin.SimplePluginService"
    minHostVersion="1.0">
    <extension point="panel"/>
    <extension point="shortcut"/>
</plugin>
```

**SimplePluginService.kt**
```kotlin
class SimplePluginService : Service() {
    private var hostCallbacks: IHostCallbacks? = null
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() { fun getService() = this@SimplePluginService }
    override fun onBind(intent: Intent) = binder

    override fun initialize(callbacks: IHostCallbacks) {
        hostCallbacks = callbacks
        // 注册快捷方式
        val shortcutData = Bundle().apply {
            putString("shortcut_id", "simple_open")
            putString("label", "简易示例")
            putString("icon_uri", "android.resource://com.example.simpleplugin/drawable/ic_launcher")
            putString("intent_action", "com.example.simpleplugin.OPEN_PANEL")
        }
        callbacks.onPluginEvent(2005, shortcutData) // PLUGIN_EVENT_REGISTER_SHORTCUT
        // 面板就绪
        callbacks.onPluginEvent(2000, Bundle().apply { putString("plugin_id", "com.example.simpleplugin") })
    }

    override fun onHostEvent(eventCode: Int, data: Bundle?) {
        when (eventCode) {
            1000 -> { /* 权限请求 */ }
            1001 -> { /* UI 刷新 */ }
        }
    }
}
```

**SimplePanel.kt (Compose 面板 UI)**
```kotlin
@Composable
fun SimplePanel(hostCallbacks: IHostCallbacks) {
    var count by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("简易插件面板", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("点击次数: $count", fontSize = 18.sp)
        Button(onClick = { 
            count++
            // 通知宿主面板内容更新
            hostCallbacks.onPluginEvent(2000, Bundle().apply {
                putString("action", "count_changed")
                putInt("count", count)
            })
        }) {
            Text("点击我", fontSize = 18.sp)
        }
    }
}
```

---

## 10. 调试与测试

### 10.1 本地调试步骤
1. **安装宿主**：编译运行 `host` 模块到设备
2. **安装插件**：编译运行插件应用到同一设备
3. **验证发现**：宿主桌面长按 → “插件管理” → 应显示插件条目
4. **查看日志**：
   ```bash
   adb logcat -s PluginManager PluginDescriptorParser MyPluginService
   ```

### 10.2 常见问题排查
| 现象 | 原因 | 解决 |
|------|------|------|
| 插件不出现在列表 | Manifest 缺少 intent-filter 或 meta-data | 检查 `<action android:name="com.explorercore.plugin.BIND_PLUGIN_SERVICE"/>` 和 `<meta-data android:name="com.explorercore.plugin">` |
| 绑定失败 `SecurityException` | 宿主未声明 `android.permission.BIND_JOB_SERVICE` | 宿主 Manifest 添加 `<uses-permission android:name="android.permission.BIND_JOB_SERVICE"/>` |
| AIDL 找不到类 | 插件未复制 AIDL 文件或包名不匹配 | 确保 `aidl/com/explorercore/plugin/*.aidl` 路径与包名一致 |
| 面板不显示 | 插件未调用 `requestPanel` 或宿主未实现容器 | 检查插件 `initialize` 中是否请求面板，宿主 `PluginManager.requestPanel` 是否返回有效 Binder |

### 10.3 单元测试建议
- **PluginDescriptorParser**：测试各种 XML 变体（缺失字段、额外扩展点、命名空间）
- **PluginManager**：Mock `PackageManager`、`ServiceConnection` 验证发现/绑定/解绑流程
- **IPluginService 实现**：Mock `IHostCallbacks` 验证事件收发

---

## 11. 发布规范

### 11.1 版本发布清单
- [ ] `versionCode` 递增，`versionName` 语义化
- [ ] `minHostVersion` 正确反映最低兼容宿主版本
- [ ] 所有声明权限在隐私政策中解释
- [ ] 通过宿主兼容性测试矩阵（宿主 v1.0/v1.1/v2.0）
- [ ] 无崩溃、ANR、内存泄漏（LeakCanary 检测）

### 11.2 应用商店发布
- 插件作为**独立应用**上架（Google Play / 国内应用市场）
- 应用描述明确标注：“Explorer Launcher 插件，需安装宿主 vX.Y+”
- 提供截图展示面板/快捷方式/搜索效果

### 11.3 企业/内部分发
- 通过 MDM 预装宿主 + 插件套件
- 支持静默安装/更新（`PackageInstaller.Session`）

---

## 12. API 兼容性承诺

| 宿主版本 | 插件 API 版本 | 兼容性策略 |
|----------|--------------|------------|
| 1.x | 1.x | 完全向后兼容，新增扩展点不破坏旧插件 |
| 2.0 | 2.x | 可能有破坏性变更，提供迁移指南及兼容层 6 个月 |
| 3.0+ | 3.x | 遵循语义化版本，Major 版本允许 Breaking Change |

**插件开发者建议**：
- 在 `plugin_descriptor.xml` 设置合理的 `minHostVersion`
- 使用 `hostCallbacks.onPluginEvent` 时做版本检查或 try-catch
- 监听宿主发布的 `HOST_EVENT_API_DEPRECATED` 事件（未来版本提供）

---

## 13. 安全与隐私要求

1. **不收集用户隐私数据** 未经明确同意
2. **网络请求** 仅用于功能必需（同步、更新检查），并在设置页提供开关
3. **文件访问** 仅通过 SAF / MediaStore / 宿主授权 URI，不直接使用文件路径
4. **进程隔离** 不尝试绕过沙箱（反射、隐藏 API、共享内存等）
5. **签名验证** 宿主可配置仅加载特定签名的插件（企业版功能）

---

## 14. 常见问题 FAQ

**Q: 插件能否没有 UI，只做后台服务？**  
A: 可以。只实现 `IPluginService`，不声明 `panel`/`shortcut` 扩展点即可。

**Q: 插件之间能否通信？**  
A: 不直接通信。通过宿主中转：插件 A 发事件 → 宿主分发给插件 B。

**Q: 如何处理插件更新后的数据迁移？**  
A: 插件自行在 `initialize` 中检查版本号执行迁移逻辑，宿主不介入。

**Q: 插件能否使用宿主的 ViewModel / Repository？**  
A: 不能跨进程共享对象。如需共享数据，定义 AIDL 接口或通过 `ContentProvider` 暴露。

**Q: 支持 Kotlin Multiplatform / Flutter / React Native 开发插件吗？**  
A: 只要能编译出实现 `IPluginService` 的 Android Service 即可。UI 层可用任何框架，但面板嵌入宿主时需输出 Compose/View 兼容的 UI。

---

## 15. 附录：AIDL 文件清单（复制到插件项目）

插件项目需包含以下 4 个 AIDL 文件（包名 `com.explorercore.plugin`）：

```
src/main/aidl/com/explorercore/plugin/
├── IPluginService.aidl      # 插件实现
├── IHostCallbacks.aidl      # 宿主实现，插件调用
├── IPanelHost.aidl          # 宿主实现，面板通信
└── SearchResultItem.aidl    // 搜索结果数据类
```

> **提示**：建议将 `plugin-api` 发布为 Maven 库，插件直接 `implementation("com.explorer.launcher:plugin-api:1.0.0")` 依赖，避免手动复制 AIDL 导致版本不一致。

---

## 16. 更新日志

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0 | 2024-01-15 | 初版发布：定义核心契约、扩展点、事件码、权限模型、示例代码 |

---

**文档维护者**：Explorer Launcher 核心团队  
**反馈渠道**：GitHub Issues / 内部 Wiki / 邮件 plugins@explorer.example.com