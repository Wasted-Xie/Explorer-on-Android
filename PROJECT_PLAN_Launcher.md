# Explorer on Android Launcher 项目规划（含插件化框架）

## 项目愿景
构建一个基于 Android 的全新桌面启动器（Launcher），以 Windows Explorer 的使用习惯为交互蓝本，同时提供高度可插拔的插件系统。文件管理器将作为首个官方插件内置，后续第三方开发者可通过标准插件接口构建功能插件（如任务栏、系统托盘、小工具、主题等），实现Launcher的无限扩展。

## 核心目标
1. **Launcher 基座**：提供桌面（home screen）、应用抽屉、文件夹、壁纸、图标网格、通知栏集成、搜索栏、底部任务栏等基础桌面功能。
2. **插件化架构**：定义统一的插件生命周期、通信协议和 UI 扩展点（如面板、托盘、快捷方式、壁纸、设置页面），使用 AIDL 或基于 ContentProvider + Broadcast 的轻量级机制，确保插件能够安全地运行在独立进程或同进程中。
3. **文件管理器插件**：作为首个内置插件实现，提供类似 Windows Explorer 的文件浏览、文件操作、右键菜单、拖拽等功能，可通过插件入口（如桌面图标、任务栏或搜索结果）启动。
4. **可定制性**：支持主题、图标包、手势、网格大小、文件夹行为等用户自定义。
5. **性能与安全**：采用现代 Android 架构（Jetpack Compose、Coroutine、Hilt），遵守后台执行限制和隐私政策，确保流畅运行。

## 总体架构
```
+--------------------------------------------------+
|                Launcher Host (宿主进程)          |
|  +-------------------+   +---------------------+  |
|  |  UI Shell (Compose) |   | Plugin Manager     |  |
|  +-------------------+   +---------------------+  |
|          ^                     ^                |
|          |                     |                |
|   +---------------+   +-----------------+      |
|   |  Extension    |   |  Plugin Runtime |      |
|   |  Points (UI)  |   | (Host/Isolated) |      |
|   +---------------+   +-----------------+      |
|          |                     |                |
+----------|---------------------|----------------+
           |                     |
  +--------v--------+  +--------v--------+
  |  官方插件 (如文件管理器)  |  第三方插件（待开发） |
  +-----------------+  +-----------------+
```
- **Launcher Host**：负责Launcher的核心功能（桌面、抽屉、搜索、通知、设置）以及插件框架的初始化、生命周期管理。
- **Extension Points**：宿主向插件暴露的扩展点，例如：
  - **Panel Extension**：可停靠在侧边或底部的面板（如文件管理器主面板、任务栏、日历面板）。
  - **Tray Extension**：系统托盘区图标（如音量、网络、操作中心）。
  - **Shortcut Extension**：可放在桌面或抽屉的快捷方式（可有自定义图标和点击行为）。
  - **Wallpaper Extension**：提供动态壁纸或锁屏壁纸实现。
  - **Settings Page Extension**：在Launcher设置中添加子页面。
  - **Search Provider Extension**：向全局搜索提供结果（如文件搜索、联系人、网页）。
  - **Gesture Handler Extension**：自定义手势处理（如双击桌面、三指上滑）。
- **Plugin Manager**：负责插件的发现、加载、版本检查、权限授予、沙箱隔离（如使用不同的 Linux UID 或 Virtualization）。
- **插件本体**：每个插件是一个独立的 Android 应用（可选择是否有 UI），通过声明 `meta-data` 和实现特定的 `Service` 或 `BroadcastReceiver` 与宿主通信。

### 插件接口规范（基于 AIDL + JSON 元数据）

1. **插件清单（AndroidManifest.xml 中的自定义元数据）**
   ```xml
   <meta-data
       android:name="com.explorercore.plugin"
       android:resource="@xml/plugin_descriptor" />
   ```
   `plugin_descriptor.xml` 示例：
   ```xml
   <plugin
       id="com.explorer.plugin.filemanager"
       name="文件管理器"
       version="1.0.0"
       entrypoint="com.explorer.plugin.filemanager.FileManagerPluginService"
       requestedPermissions="android.permission.MANAGE_EXTERNAL_STORAGE,android.permission.READ_MEDIA_IMAGES"
       >
       <extension point="panel"/>
       <extension point="shortcut"/>
       <extension point="search"/>
   </plugin>
   ```

2. **插件服务契约（AIDL）**
   ```aidl
   // IPluginService.aidl
   package com.explorercore.plugin;
   interface IPluginService {
       // 初始化，宿主传入回调对象用于向插件发送事件
       void initialize(in IHostCallbacks callbacks);
       // 插件可通过此方法向宿主申请UI容器或通知事件
       void onHostEvent(in int eventCode, in Bundle data);
   }
   ```
   对应的宿主回调：
   ```aidl
   // IHostCallbacks.aidl
   package com.explorercore.plugin;
   interface IHostCallbacks {
       void onPluginEvent(in int eventCode, in Bundle data);
       // 供插件请求创建面板等
       IBinder requestPanel(in String panelId, in Bundle initialArgs);
       void notifySearchResults(in String query, in List<SearchResultItem> results);
   }
   ```

3. **事件约定（事件码可自行定义，建议使用范围划分）**
   - 1000-1999：宿主 → 插件（如 `HOST_EVENT_REQUEST_PERMISSION`, `HOST_EVENT_UI_UPDATE`）
   - 2000-2999：插件 → 宿主（如 `PLUGIN_EVENT_PANEL_READY`, `PLUGIN_EVENT_SHOW_TOAST`）

4. **UI 扩展点实现方式**
   - **Panel Extension**：插件服务返回一个包含 Compose UI 或传统 View 的 `IBinder`（通过另一个 AIDL `IPanelHost`），宿主在桌面或抽屉中以悬浮窗、侧边栏或底部槽位形式展示。
   - **Shortcut Extension**：插件提供一个 `PendingIntent` 和图标资源，宿主生成可放置的快捷方式。
   - **Wallpaper Extension**：插件实现 `WallpaperService`，宿主在壁纸选择列表中展示。

5. **沙箱与安全**
   - 插件默认运行在其自身的应用沙箱中（不同的 `userId`），仅通过受控的 AIDL 接口与宿主通信。
   - 若插件需要访问敏感数据（如读取所有文件），宿主在插件初始化时会弹出系统权限对话框（通过 `ActivityCompat.requestPermissions`），并仅在授权后才把相应权限代理给插件（使用临时授权 URI 或 `ContentProvider` 授权）。

6. **插件市场与更新（预留）**
   - 插件可通过常规应用商店更新，宿主在启动时检查已安装插件的版本元数据，若有更新则提示用户。
   - 宿主提供隐式广播 `ACTION_PLUGIN_UPDATE_REQUESTED`，第三方插件可自行实现更新检测逻辑。

## 功能模块（宿主端）

| 模块 | 主要职责 |
|------|----------|
| **Launcher Core** | 桌面布局、应用抽屉、文件夹、壁纸、网格手势、应用图标图标缓存、通知栏集成 |
| **搜索栏** | 全局搜索入口，调度插件搜索提供者并合并结果 |
| **任务栏（底部）** | 展示已启动的插件面板、系统状态图标、开始菜单（可插件化） |
| **开始菜单** | 类似 Windows 开始菜单的弹出面板，可放置插件快捷方式、最近使用、电源按钮 |
| **设置页面** | Launcher 全局设置（主题、图标大小、网格数、手势）以及插件设置入口 |
| **插件框架** | 插件发现、加载、生命周期管理、权限代理、通信通道 |
| **通知与快捷操作** | 将系统通知与快捷操作整合到任务栏或托盘 |
| **文件管理器插件**（内置） | 详见下文 |

## 文件管理器插件（内置插件）概述
- 作为普通的 Android 应用，声明插件元数据，入口为 `FileManagerPluginService`。
- 提供以下扩展点实现：
  - **Panel Extension**：主文件管理器面板（可停靠在侧边、底部或作为独立窗口）。
  - **Shortcut Extension**：桌面图标“文件管理器”，点击启动面板。
  - **Search Provider Extension**：实现文件名、内容（简单）搜索，返回 `SearchResultItem`。
  - **Tray Extension**（可选）：在系统托盘显示当前位置或快速访问常用文件夹。
- 核心功能与之前文档中的文件管理器保持一致（文件树、列表视图、右键菜单、拖拽、复制/粘贴等），但 UI 使用 Jetpack Compose 并通过插件宿主的面板容器渲染。
- 插件内部仍可使用相同的域层和数据层（通过共享库或 AIDL）来保持代码复用。

## 技术栈（宿主 & 插件通用）
- **语言**：Kotlin 1.8+
- **UI**：Jetpack Compose + Material3（宿主），插件可自行选择 Compose 或 View。
- **架派**：MVVM + Clean Architecture（宿主），插件内部推荐同样结构。
- **依赖注入**：Hilt（宿主），插件若为独立应用可自行决定是否使用 Hilt。
- **异步**：Kotlin Coroutines + Flow
- **插件通信**：AIDL（跨进程），可选本地 Service 绑定（同进程调试）。
- **数据持久**：DataStore（宿主配置）、插件自行使用偏好设置或 Room。
- **权限**：运行时权限框架（Manifest + ActivityCompat）。
- **测试**：JUnit5, Mockito, Turbine, Compose Test，插件单独测试。
- **CI/CD**：GitHub Actions（构建宿主及插件APK/AAB，运行单元测试，生成插件元数据校验）。

## 项目里程碑（约 30 周）

| 里程碑 | 目标 | 预计时长 |
|--------|------|----------|
| M0：项目初始化 & 插件框架设计 | 创建多模块 Gradle 项目（host、plugin-api、filemanager-plugin、共享库），定义 AIDL 接口和插件元数据格式 | 3周 |
| M1：Launcher 基座 – 桌面与抽屉 | 实现基本的桌面图标网格、应用抽屉、壁纸、文件夹创建、长按编辑 | 4周 |
| M2：搜索栏与全局搜索 | 添加搜索 UI，插件搜索提供者契约，合并结果展示 | 2周 |
| M3：任务栏与开始菜单 | 底部任务栏（图标、系统托盘位置）、弹出开始菜单面板 | 3周 |
| M4：插件框架实现（加载、生命周期、通信） | 插件管理器、沙箱模拟（不同applicationId）、AIDL绑定、事件分发 | 4周 |
| M5：官方文件管理器插件 – 基础面板 | 实现文件管理器插件的面板扩展（侧边栏式文件浏览器）、基本目录遍历 | 5周 |
| M6：文件管理器插件 – 核心操作 | 复制/粘贴/删除/重命名、新建文件/文件夹、属性查看、拖拽支持 | 4周 |
| M7：文件管理器插件 – 搜索与托盘 | 实现搜索提供者、系统托盘图标、快捷方式入口 | 3周 |
| M8：设置与个性化（宿主 & 插件） | Launcher 主题、图标大小、手势；插件设置页入口 | 2周 |
| M9：第三方插件示例（如任务栏插件、天气小件） | 提供示例插件模板，验证扩展点完整性 | 3周 |
| M10：性能优化与内存管理 | 延迟加载、图标缓存（LruCache / Coil）、面板复用、后台服务优化 | 3周 |
| M11：安全与权限治理 | 运行时权限代理机制、隐私合规说明、Scoped Storage 适配 | 2周 |
| M12：发布准备 | 生成签名 AAB/APK、撰写隐私政策、应用商店页面描述、内测分发 | 2周 |
| **总计** | 约 **30周**（约 7.5 个月） |  |

## 风险与应对
- **跨进程通信开销**：使用 AIDL 会有 IPC 开销，面板频繁刷新时可考虑共享内存（通过 `AssetFileDescriptor` 或 `MediaProjection`）或在插件需要高频 UI 时允许同进程调试变体（通过不同的 `android:process` 配置）。
- **插件兼容性**：制定清晰的版本契约（语义化版本），在 `plugin-descriptor` 中声明最低宿主版本，宿主在加载时检查并拒绝不兼容插件。
- **后台限制**：插件若需要长时间后台工作（如文件复制），应使用宿主提供的前台服务代理或后台工作管理器（WorkManager），并在通知中报告进度。
- **安全沙箱逃逸**：坚持插件仅通过受控 AIDL 接口通信，不暴露文件系统直接读写；若插件需要文件访问，由宿主通过内容提供者或授权 URI 临时共享。

## 下一步行动
1. 创建多模块 Gradle 项目结构：
   - `:host`（Launcher 主应用）
   - `:plugin-api`（AIDL 接口、数据模型、插件元数据解析）
   - `:filemanager-plugin`（官方文件管理器插件示例）
   - `:shared`（可选：域层、网络、工具类等共享代码）。
2. 在 `:plugin-api` 中定义 `IPluginService.aidl`、`IHostCallbacks.aidl`、`IPanelHost.aidl` 等以及 `plugin-descriptor.xsd`（或简单的 XML 解析约定）。
3. 实现宿主的 `PluginManager` 负责读取已安装应用的插件元数据、验证签名、绑定服务。
4. 开发 `:host` 的基础桌面 UI（图标网格、抽屉、壁纸）。
5. 同时启动 `:filemanager-plugin` 的面板实现，确保能够通过插件框架成功加载并显示在宿主侧边栏。

---

**已生成的项目规划文件**：  
`C:\Projects\Explorer on Android\PROJECT_PLAN_Launcher.md`

此规划已把Launcher作为宿主，文件管理器作为首个内置插件，并给出了标准化的插件接口（AIDL + 元数据），方便未来第三方插件的构建与集成。如需进一步细化某个模块（例如插件安全沙箱、UI 扩展点的具体 Compose API），请告诉我！祝项目顺利。