# Explorer on Android - Windows 10 Style Launcher with Plugin Architecture
# 适用于Android设备的桌面应用——采用Windows 10风格的主界面设计，并具备插件架构

> A modern Android launcher inspired by Windows 10 Explorer, featuring a powerful plugin system for extensibility.
>一款现代风格的安卓启动器，其设计灵感来源于Windows 10的资源管理器界面。该启动器拥有强大的插件系统，可方便用户进行扩展。
[![Android](https://img.shields.io/badge/Android-API%2021%2B-green.svg)](https://developer.android.com/about/versions)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5-orange.svg)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Hilt-2.48-red.svg)](https://dagger.dev/hilt)
[![Gradle](https://img.shields.io/badge/Gradle-8.13-yellow.svg)](https://gradle.org)

---

## 目录

- [项目概览](#项目概览)
- [核心特性](#核心特性)
- [架构设计](#架构设计)
- [项目结构](#项目结构)
- [UI 组件](#ui-组件)
- [插件系统](#插件系统)
- [任务调度系统](#任务调度系统)
- [主题系统](#主题系统)
- [快速开始](#快速开始)
- [插件开发指南](#插件开发指南)
- [构建与运行](#构建与运行)
- [常见问题](#常见问题)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 项目概览

**Explorer on Android** 是一个基于 Android 的桌面启动器，旨在将 Windows 10 Explorer 的交互体验带到移动端。核心设计理念：

- **插件化架构**：核心宿主仅提供基础桌面能力，所有垂直功能（文件管理、天气、日历等）均以独立插件形式接入
- **Windows 10 风格**：任务栏、开始菜单、桌面图标网格、应用抽屉、右键菜单等完整复刻
- **现代技术栈**：Kotlin + Jetpack Compose + Hilt + Coroutines + Flow + AIDL
- **进程隔离**：插件运行在独立进程，通过 AIDL 与宿主通信，崩溃不影响主进程

---

## 核心特性

### 🖥️ 桌面环境
- **图标网格**：支持拖拽排序、长按上下文菜单、文件夹创建、多页面滑动
- **壁纸系统**：静态/动态壁纸、纯色、每日必应壁纸
- **应用抽屉**：字母分组、搜索过滤、最近使用、底部滑出动画

### 📋 任务栏与开始菜单
- **任务栏**：开始按钮 ⊞、搜索框、固定应用、运行中应用、系统托盘、日期时间、操作中心
- **开始菜单**：左侧固定/最近应用、右侧磁贴网格、底部电源/设置/所有应用
- **右键/长按菜单**：5 种菜单类型（桌面空白/图标/文件夹/任务栏应用/任务栏空白）

### 🔌 插件系统
- **标准化契约**：4 个 AIDL 接口定义宿主↔插件通信
- **扩展点**：Panel、Shortcut、Search、Tray、Settings、Wallpaper、Gesture
- **权限代理**：宿主统一申请权限，通过 ContentProvider 临时授权给插件
- **热插拔**：安装/卸载/更新插件自动检测，无需重启宿主

### ⚙️ 任务调度
- **优先级队列**：LOW/NORMAL/HIGH/CRITICAL 四级
- **并发控制**：信号量限流（全局 8 / 高优先级 4）
- **超时重试**：可配置超时与重试次数
- **WorkManager 集成**：持久化后台任务，支持充电/网络约束

---

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Explorer Launcher Host                    │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │  桌面 UI    │  │  搜索栏     │  │  任务栏/开始菜单│  ← Compose UI |
│  └─────────────┘  └─────────────┘  └─────────────┘          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              PluginManager (单例)                        │ │
│  │  • 插件发现/解析/版本检查                                 │ │
│  │  • AIDL 绑定/初始化/事件分发                             │ │
│  │  • 动态刷新（包变更广播）                                │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              TaskDispatcher (单例)                       │ │
│  │  • 优先级队列 (LOW/NORMAL/HIGH/CRITICAL)                │ │
│  │  • 并发控制 (信号量 8/4) + 超时重试                      │ │
│  │  • WorkManager 持久化任务                                │ │
│  │  • 插件暴露 submitTask/cancelTask/...                    │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  AIDL 契约层 (IPluginService / IHostCallbacks / IPanelHost)  │
├─────────────────────────────────────────────────────────────┤
│  插件进程隔离                                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ FileManager  │  │  Weather     │  │  ThirdParty  │  ...  │
│  │   Plugin     │  │   Plugin     │  │   Plugins    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

---

## 项目结构

```
Explorer on Android/
├── settings.gradle.kts          # Gradle settings with all modules
├── build.gradle.kts             # Root build configuration
├── gradle.properties            # Project-wide Gradle properties
├── local.properties             # Local SDK configuration (auto-generated)
├── gradlew / gradlew.bat        # Gradle wrapper scripts
├── gradle/wrapper/              # Gradle wrapper
├── PROJECT_PLAN.md              # 项目规划文档
├── PROJECT_PLAN_Launcher.md     # Launcher 版规划文档
├── PLUGIN_DEVELOPMENT_GUIDE.md  # 插件开发标准接入文档
├── plugin-api/                  # 插件 API 库
│   ├── src/main/
│   │   ├── aidl/com/explorercore/plugin/
│   │   │   ├── IPluginService.aidl       # 插件实现，宿主绑定
│   │   │   ├── IHostCallbacks.aidl       # 宿主实现，插件调用
│   │   │   ├── IPanelHost.aidl           # 面板通信接口
│   │   │   └── SearchResultItem.aidl     # 搜索结果数据类
│   │   ├── java/com/explorercore/plugin/
│   │   │   └── PluginContract.java       # 事件码/常量定义
│   │   └── res/xml/
│   │       └── sample_plugin_descriptor.xml
│   └── build.gradle.kts
├── host/                        # 主 Launcher 应用
│   ├── src/main/
│   │   ├── AndroidManifest.xml           # HOME Intent、权限、接收器
│   │   ├── java/com/explorer/launcher/host/
│   │   │   ├── HostApplication.kt        # Hilt 入口、PluginManager 初始化
│   │   │   ├── PluginReceiver.kt         # 包变更广播、插件热插拔
│   │   │   ├── model/                    # 数据模型
│   │   │   │   ├── DesktopItem.kt        # 图标/文件夹/快捷方式/插件面板
│   │   │   │   └── ...
│   │   │   ├── plugin/                   # 插件管理核心
│   │   │   │   ├── PluginManager.kt      # 发现/绑定/生命周期/事件分发
│   │   │   │   ├── PluginDescriptorParser.kt # XML 解析
│   │   │   │   ├── PluginInfo.kt         # 插件元数据模型
│   │   │   │   ├── PluginModule.kt       # Hilt 绑定
│   │   │   │   └── PluginTaskApi.kt      # 插件任务 API 接口
│   │   │   ├── task/                     # 任务调度系统
│   │   │   │   ├── TaskDispatcher.kt     # 核心调度器
│   │   │   │   ├── Task.kt               # 任务定义/结果/上下文
│   │   │   │   ├── TaskPriority.kt       # 优先级枚举
│   │   │   │   ├── BackgroundTaskWorker.kt # WorkManager Worker
│   │   │   │   └── TaskModule.kt         # Hilt 绑定
│   │   │   ├── ui/                       # UI 组件
│   │   │   │   ├── MainActivity.kt       # 统一状态管理、组件组装
│   │   │   │   ├── components/
│   │   │   │   │   ├── Taskbar.kt        # 任务栏
│   │   │   │   │   ├── StartMenu.kt      # 开始菜单
│   │   │   │   │   ├── DesktopGrid.kt    # 桌面图标网格
│   │   │   │   │   ├── AppDrawer.kt      # 应用抽屉
│   │   │   │   │   └── ContextMenu.kt    # 右键/长按菜单
│   │   │   │   └── theme/
│   │   │   │       ├── Theme.kt          # 主题入口
│   │   │   │       └── Windows10Theme.kt # Win10 配色/排版/阴影
│   │   │   └── res/                      # 资源文件
│   │   └── build.gradle.kts
├── filemanager-plugin/          # 官方文件管理器插件
│   ├── src/main/
│   │   ├── AndroidManifest.xml           # 插件服务声明、元数据
│   │   ├── aidl/com/explorercore/plugin/ # AIDL 编译用
│   │   ├── java/com/explorer/plugin/filemanager/
│   │   │   ├── FileManagerApplication.kt
│   │   │   ├── FileManagerPluginService.kt # IPluginService 实现
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt       # 独立启动 Activity
│   │   │       └── theme/Theme.kt
│   │   └── res/
│   │       ├── values/strings.xml
│   │       ├── values/colors.xml
│   │       ├── values/themes.xml
│   │       └── xml/plugin_descriptor.xml # 插件描述符
│   └── build.gradle.kts
└── shared/                      # 共享通用库
    ├── src/main/java/
    └── build.gradle.kts
```

---

## UI 组件

### Taskbar (`Taskbar.kt`)
| 组件 | 功能 |
|------|------|
| StartButton | Windows 徽标 ⊞，点击打开开始菜单 |
| SearchBox | 圆角搜索框，点击打开应用抽屉 |
| TaskbarAppsSection | 固定应用 + 运行中应用合并去重 |
| SystemTray | 网络/音量/电池托盘图标 |
| DateTimeWidget | 时间日期显示，点击打开日历 |
| ActionCenterButton | 通知中心入口 |

### StartMenu (`StartMenu.kt`)
- **左侧**：固定应用列表、最近使用、可展开/收起
- **右侧**：磁贴网格（4列×3行），支持中/大/宽/小四种尺寸
- **底部栏**：电源、设置、所有应用/收起切换
- **动画**：宽度/透明度/缩放联动，BottomStart 为变换原点

### DesktopGrid (`DesktopGrid.kt`)
- **LazyVerticalGrid**：固定列数，自动分页，页面指示器
- **DesktopIcon**：拖拽手势、长按菜单、选中/悬停状态、标签显示
- **EmptyGridSlot**：空位显示 "+"，长按创建快捷方式/文件夹
- **FolderView**：底部弹出式文件夹内容网格

### AppDrawer (`AppDrawer.kt`)
- **搜索栏**：实时过滤应用列表
- **最近使用**：顶部横向网格（最多 8 个）
- **所有应用**：按首字母分组（A-Z），字母侧边索引
- **空状态**：无结果时显示友好提示

### ContextMenu (`ContextMenu.kt`)
| 菜单类型 | 触发条件 | 主要操作 |
|----------|----------|----------|
| DESKTOP_EMPTY | 桌面空白处长按 | 新建文件夹/快捷方式、粘贴、排序、视图设置 |
| DESKTOP_ITEM | 图标长按 | 打开、固定到任务栏、创建快捷方式、重命名、属性、删除 |
| FOLDER | 文件夹长按 | 重命名、更改颜色、删除 |
| TASKBAR_APP | 任务栏图标长按 | 取消固定、关闭窗口、关闭所有、属性 |
| TASKBAR_EMPTY | 任务栏空白长按 | 任务栏设置、显示桌面 |

---

## 插件系统

### AIDL 契约 (4 个接口)

```aidl
// IPluginService.aidl - 插件实现，宿主绑定
interface IPluginService {
    void initialize(in IHostCallbacks callbacks);
    void onHostEvent(in int eventCode, in Bundle data);
}

// IHostCallbacks.aidl - 宿主实现，插件调用
interface IHostCallbacks {
    void onPluginEvent(in int eventCode, in Bundle data);
    IBinder requestPanel(in String panelId, in Bundle initialArgs);
    void notifySearchResults(in String query, in List<SearchResultItem> results);
    // 任务管理扩展
    String submitTask(in String taskName, in int priority, in Bundle payload, in String executorType);
    boolean cancelTask(in String taskId);
    int getTaskState(in String taskId);
    void scheduleBackgroundTask(in String taskType, in String payload, in long delayMillis, in boolean requiresCharging, in boolean requiresNetwork);
    void schedulePeriodicBackgroundTask(in String taskType, in long intervalMillis, in String payload, in boolean requiresCharging, in boolean requiresNetwork);
}

// IPanelHost.aidl - 面板通信（宿主实现，插件使用）
interface IPanelHost {
    void onUpdate(in Bundle data);
    void showToast(in String message);
    void requestResize(in int width, in int height);
}

// SearchResultItem.aidl - 搜索结果数据类
parcelable SearchResultItem {
    String id, title, subtitle, iconUri, intentAction, intentData;
    Bundle extras;
}
```

### 插件清单 (`plugin_descriptor.xml`)

```xml
<plugin xmlns:android="http://schemas.android.com/apk/res/android"
    id="com.example.myplugin"
    name="我的插件"
    version="1.0.0"
    entrypoint="com.example.myplugin.MyPluginService"
    minHostVersion="1.0">
    <permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <extension point="panel"/>
    <extension point="shortcut"/>
    <extension point="search"/>
</plugin>
```

### 服务实现模板

```kotlin
class MyPluginService : Service() {
    private var hostCallbacks: IHostCallbacks? = null

    override fun onBind(intent: Intent): IBinder = LocalBinder()

    override fun initialize(callbacks: IHostCallbacks) {
        hostCallbacks = callbacks
        // 注册快捷方式
        callbacks.onPluginEvent(PLUGIN_EVENT_REGISTER_SHORTCUT, bundleOf(
            "shortcut_id" to "my_shortcut",
            "label" to "我的插件",
            "icon_uri" to "android.resource://.../ic_launcher",
            "intent_action" to "com.example.OPEN"
        ))
        // 面板就绪
        callbacks.onPluginEvent(PLUGIN_EVENT_PANEL_READY, bundleOf("plugin_id" to "com.example.myplugin"))
    }

    override fun onHostEvent(eventCode: Int, data: Bundle?) {
        when (eventCode) {
            HOST_EVENT_REQUEST_PERMISSION -> requestPermissions(data)
            HOST_EVENT_UI_UPDATE -> refreshUI()
            HOST_EVENT_THEME_CHANGED -> applyTheme(data?.getInt("theme_mode") ?: 0)
        }
    }
}
```

### 权限模型
1. 插件在 `plugin_descriptor.xml` 声明所需权限
2. 宿主在插件首次绑定时统一弹窗请求用户授权
3. 授权后通过 ContentProvider 临时 URI 或直接授权传递给插件
4. 敏感权限（MANAGE_EXTERNAL_STORAGE）引导用户进入系统设置页

---

## 任务调度系统

### TaskDispatcher 核心能力
- **优先级调度**：PriorityQueue + 信号量分级（全局 8 并发，高优先级 4 并发）
- **任务生命周期**：PENDING → QUEUED → RUNNING → COMPLETED/FAILED/CANCELLED/TIMEOUT
- **超时重试**：`withTimeoutOrNull` + `retryCount` 自动重入队列
- **进度回调**：`TaskContext.updateProgress()` → `TaskListener.onTaskProgress()`
- **取消传播**：`CancellationException` 链式取消，支持按 ID/标签批量取消
- **WorkManager 集成**：持久化任务，支持充电/网络约束

### 使用示例

```kotlin
// 宿主侧提交任务
taskDispatcher.submitSimple(
    name = "PluginDiscovery",
    priority = TaskPriority.HIGH,
    executor = { discoverAndLoadPlugins() }
)

// 插件通过 IHostCallbacks 提交任务
val taskId = hostCallbacks.submitTask(
    "FileIndexRebuild",
    PluginContract.HOST_EVENT_TASK_SUBMITTED, // priority = 10 (HIGH)
    bundleOf("path" to "/sdcard"),
    "plugin_io"
)

// 插件调度持久化周期任务
hostCallbacks.schedulePeriodicBackgroundTask(
    "plugin_search_index_update",
    30 * 60 * 1000L,  // 30 分钟
    "{}",
    false,  // requiresCharging
    true    // requiresNetwork
)
```

---

## 主题系统

### Windows10Theme (`Windows10Theme.kt`)
| 颜色角色 | 深色模式 | 浅色模式 |
|----------|----------|----------|
| Primary (Windows Blue) | `#0078D7` | `#0078D7` |
| TaskbarBackground | `#101010` | `#F2F2F2` |
| StartMenuBackground | `#1F1F1F` | `#FFFFFF` |
| DesktopBackground | `#004F8C` | `#F2F2F2` |
| SearchBackground | `#2D2D2D` | `#F3F3F3` |

### 排版系统 (Windows10Typography)
- **TitleLarge**: 32sp / 40sp line-height
- **TitleMedium**: 20sp / 28sp
- **BodyLarge**: 14sp / 20sp
- **BodyMedium**: 13sp / 18sp
- **LabelLarge**: 14sp Medium / 20sp
- **LabelMedium**: 12sp Medium / 16sp
- **Caption**: 11sp / 14sp

### 圆角与阴影
- **Small**: RectangleShape (0dp)
- **Medium**: 4dp
- **Large**: 8dp
- **Pill**: 50dp (搜索框、按钮)
- **阴影分级**：CardElevation (8dp) < MenuElevation (16dp) < FlyoutElevation (24dp)

---

## 快速开始

### 环境要求
- Android Studio Ladybug (2024.2.1) 或更高版本
- JDK 17+ (推荐 Eclipse Temurin 21)
- Android SDK 34 (API 34)
- Gradle 8.13 (Android Studio 内置)

### 打开项目
```bash
# 1. 克隆/解压项目
cd "C:\Projects\Explorer on Android"

# 2. 在 Android Studio 中打开
# File → Open → 选择此目录

# 3. 等待 Gradle Sync 完成
# 首次同步会自动下载依赖、生成 gradlew wrapper
```

### 构建与运行
1. **Build → Make Project** (Ctrl+F9)
2. **Run → Run 'host'** (Shift+F10)
3. 选择模拟器 (API 21+) 或真机
4. 首次运行会请求设置为默认桌面 (HOME Intent)

### 安装文件管理器插件
- 文件管理器插件作为独立应用安装
- 或在宿主构建时包含 (`filemanager-plugin` 模块)
- 安装后自动被 `PluginManager` 发现并加载

---

## 插件开发指南

详见 [PLUGIN_DEVELOPMENT_GUIDE.md](PLUGIN_DEVELOPMENT_GUIDE.md) 完整文档。

### 快速创建插件

```kotlin
// 1. 依赖 plugin-api
implementation("com.explorer.launcher:plugin-api:1.0.0")
// 或本地依赖
implementation(project(":plugin-api"))

// 2. 实现 IPluginService
class MyPluginService : Service() {
    private var hostCallbacks: IHostCallbacks? = null
    override fun onBind(intent: Intent) = LocalBinder()
    override fun initialize(callbacks: IHostCallbacks) { ... }
    override fun onHostEvent(eventCode: Int, data: Bundle?) { ... }
}

// 3. AndroidManifest.xml 声明
<service android:name=".MyPluginService" android:exported="true">
    <intent-filter>
        <action android:name="com.explorercore.plugin.BIND_PLUGIN_SERVICE"/>
    </intent-filter>
    <meta-data android:name="com.explorercore.plugin" android:resource="@xml/plugin_descriptor"/>
</service>

// 4. res/xml/plugin_descriptor.xml
<plugin id="com.example.myplugin" name="My Plugin" version="1.0.0"
        entrypoint="com.example.MyPluginService" minHostVersion="1.0">
    <extension point="panel"/>
    <extension point="shortcut"/>
</plugin>
```

### 扩展点详解
| 扩展点 | 用途 | 宿主 UI 容器 |
|--------|------|--------------|
| `panel` | 功能面板 | 侧边栏/底部槽位/悬浮窗 |
| `shortcut` | 桌面/抽屉图标 | 图标网格 |
| `search` | 全局搜索结果 | 搜索结果聚合页 |
| `tray` | 任务栏托盘图标 | 任务栏右侧 |
| `settings` | 设置页面 | 宿主设置内嵌 |
| `wallpaper` | 动态壁纸 | 壁纸选择器 |
| `gesture` | 全局手势 | 手势检测层 |

---

## 构建与运行

### Gradle 命令
```bash
# 编译调试版
./gradlew :host:assembleDebug

# 运行单元测试
./gradlew :host:testDebugUnitTest

# 运行 instrumentation 测试
./gradlew :host:connectedAndroidTest

# 清理构建产物
./gradlew clean
```

### 代码生成
```bash
# 生成 AIDL 接口代码
./gradlew :plugin-api:compileDebugAidl

# 生成 Hilt 组件
./gradlew :host:kaptDebugKotlin
```

### 常用配置
- **修改包名**：更新各模块 `build.gradle.kts` 的 `namespace` / `applicationId`
- **更改最低 SDK**：修改各模块 `minSdk`
- **调整 Compose 版本**：更新 `kotlinCompilerExtensionVersion` 对应关系

---

## 常见问题

### Q: 编译报错 `Unresolved reference: OpenInNew`
**A**: Material Icons 中无 `OpenInNew`，已修复为 `Icons.Default.Launch`。请同步最新代码。

### Q: Gradle 同步失败 `Could not find com.android.tools.build:gradle:8.1.0`
**A**: 确保 `settings.gradle.kts` 中 `google()` 和 `mavenCentral()` 仓库可用。国内可配置阿里云镜像。

### Q: 运行时崩溃 `ClassNotFoundException: IPluginService`
**A**: 插件模块需在 `build.gradle.kts` 启用 `buildFeatures { aidl = true }` 并确保 AIDL 文件路径正确 (`src/main/aidl/com/explorercore/plugin/`)。

### Q: 插件不显示在桌面
**A**: 检查：
1. 插件 APK 已安装且版本匹配
2. `plugin_descriptor.xml` 中 `entrypoint` 指向正确的 Service 类
3. `AndroidManifest.xml` Service 声明包含 `BIND_PLUGIN_SERVICE` intent-filter 和 meta-data
4. `PluginManager` 日志中是否有 `Discovered plugin` 记录

### Q: 任务栏/开始菜单不显示
**A**: 确保 `MainActivity` 设置了 `android:launchMode="singleTask"` 和 `android:stateNotNeeded="true"`。检查 `WindowInsets` 沉浸式状态栏配置。

### Q: Windows 11 上 Gradle 守护进程启动失败
**A**: 这是 Gradle 8.13+ 已知 Bug。请使用 **Android Studio** 内置的 Gradle 进行构建，而非命令行。

---

## 贡献指南

### 代码规范
- 遵循 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- Compose UI 使用 `@Preview` 标注预览函数
- 公共 API 必须编写 KDoc 注释
- 提交前运行 `./gradlew :host:lintDebug`

### 分支策略
- `main`：稳定发布分支
- `develop`：开发集成分支
- `feature/*`：功能分支
- `fix/*`：Bug 修复分支
- `release/*`：发布准备分支

### 提交信息格式
```
<type>(<scope>): <subject>

<body>

<footer>
```
类型：`feat`、`fix`、`docs`、`style`、`refactor`、`test`、`chore`

### PR 流程
1. Fork 仓库并创建特性分支
2. 编写代码并添加测试
3. 确保所有检查通过 (Lint/测试/构建)
4. 提交 PR，填写模板
5. Code Review 通过后合并

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0 | 2024-01 | 初版：核心架构、插件系统、Windows 10 UI、文件管理器插件骨架 |

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [PROJECT_PLAN.md](PROJECT_PLAN.md) | 项目整体规划 |
| [PROJECT_PLAN_Launcher.md](PROJECT_PLAN_Launcher.md) | Launcher 详细规划与里程碑 |
| [PLUGIN_DEVELOPMENT_GUIDE.md](PLUGIN_DEVELOPMENT_GUIDE.md) | 插件开发标准接入文档 |

---

## 联系方式

- **项目维护者**：Explorer Launcher 核心团队
- **问题反馈**：GitHub Issues
- **内部沟通**：内部 Wiki / 邮件 plugins@explorer.example.com

---

## 许可证

Proprietary - 仅供内部开发使用。未经授权不得用于商业用途。

---

**最后更新**：2024-01 | **文档版本**：1.0.0
