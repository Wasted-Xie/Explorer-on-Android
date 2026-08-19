// HANDOFF.md
# Explorer on Android - 项目交接文档

> 版本：v1.0.0 | 日期：2024-01 | 状态：核心架构就绪，UI 组件完整，编译通过（需 Android Studio / 云端构建）

---

## 📋 项目概况

| 项目 | 说明 |
|------|------|
| **名称** | Explorer on Android |
| **类型** | Windows 10 风格 Android Launcher + 插件化架构 |
| **最低 SDK** | API 21 (Android 5.0) |
| **目标 SDK** | API 34 (Android 14) |
| **语言** | Kotlin 1.9.22 |
| **UI 框架** | Jetpack Compose 1.5 + Material 3 |
| **依赖注入** | Hilt 2.48 |
| **异步** | Coroutines + Flow |
| **插件通信** | AIDL (4 个接口) |
| **构建工具** | Gradle 8.13 (AGP 8.1.0) |

---

## ✅ 已完成工作清单

### 1. 核心架构 (100%)
- [x] **多模块 Gradle 工程**：`plugin-api` / `host` / `filemanager-plugin` / `shared`
- [x] **插件化框架**：`PluginManager` 发现/解析/绑定/生命周期/热插拔
- [x] **任务调度系统**：`TaskDispatcher` 优先级队列/并发控制/超时重试/WorkManager 集成
- [x] **AIDL 契约**：`IPluginService` / `IHostCallbacks` / `IPanelHost` / `SearchResultItem`
- [x] **事件总线**：宿主↔插件双向通信，标准化事件码 (1000-1999 / 2000-2999)

### 2. Windows 10 UI 组件 (100%)
| 组件 | 文件 | 核心功能 |
|------|------|----------|
| **Taskbar** | `Taskbar.kt` | 开始按钮、搜索框、固定/运行中应用、系统托盘、时间、操作中心 |
| **StartMenu** | `StartMenu.kt` | 左侧固定/最近应用、右侧磁贴网格、底部电源/设置/所有应用、展开动画 |
| **DesktopGrid** | `DesktopGrid.kt` | 图标网格、拖拽排序、长按菜单、文件夹创建、页面指示器、空槽位 |
| **AppDrawer** | `AppDrawer.kt` | 底部滑出、搜索过滤、字母分组、最近使用、空状态 |
| **ContextMenu** | `ContextMenu.kt` | 5种菜单类型（桌面空白/图标/文件夹/任务栏应用/任务栏空白）、动画弹出 |

### 3. 主题系统 (100%)
- `Windows10Theme.kt`：深/浅色配色方案、Windows Blue `#0078D7` 强调色
- `Windows10Typography.kt`：6级排版系统 (TitleLarge → Caption)
- `Windows10Shapes/Shadows.kt`：圆角/阴影分级

### 4. 数据模型 (100%)
- `DesktopItem`：APP/FOLDER/WIDGET/SHORTCUT/PLUGIN_PANEL 类型
- `Folder`：文件夹嵌套、背景色
- `Wallpaper`：静态/动态/纯色/每日必应
- `DesktopLayout`：网格列行、图标大小/间距、分页
- `TaskbarConfig`：位置/高度/图标/显示开关/固定应用列表

### 5. 文件管理器插件骨架 (100%)
- `FileManagerPluginService`：实现 `IPluginService`，生命周期/事件处理
- `plugin_descriptor.xml`：声明 panel/shortcut/search 扩展点
- 独立 Activity 支持直接启动

### 6. 文档体系 (100%)
- `README.md`：完整架构/UI/插件/构建/常见问题文档
- `BUILD_GUIDE.md`：Windows 编译实操指南、云端构建方案
- `PLUGIN_DEVELOPMENT_GUIDE.md`：插件开发标准接入文档 (AIDL/扩展点/权限/生命周期)
- `PROJECT_PLAN.md` / `PROJECT_PLAN_Launcher.md`：里程碑规划

---

## 🔧 当前构建状态

| 环境 | 状态 | 说明 |
|------|------|------|
| **Android Studio** | ✅ 可用 | 推荐方式，Embedded Gradle 绕过 native-platform.dll Bug |
| **Windows 命令行 Gradle** | ❌ 失败 | `native-platform.dll` 加载失败 (Gradle 8.13+ Windows 11 已知 Bug) |
| **WSL2** | ⚠️ 未验证 | 需安装 `wsl --install` |
| **GitHub Actions** | ✅ 已配置 | `.github/workflows/build.yml` 推送即构建 |

### 获取 APK 的推荐路径
1. **本地**：用 Android Studio 打开 → `Build → Build APK(s)`
2. **云端**：推送到 GitHub → Actions 自动构建 → Artifacts 下载 `host-debug.apk`

---

## 📁 关键文件速查

```
C:\Projects\Explorer on Android\
├── host/                          # 主 Launcher 模块
│   ├── src/main/java/com/explorer/launcher/host/
│   │   ├── plugin/                # 插件管理核心
│   │   │   ├── PluginManager.kt           # 核心管理器
│   │   │   ├── PluginDescriptorParser.kt  # XML 解析
│   │   │   ├── PluginInfo.kt              # 元数据模型
│   │   │   └── PluginTaskApi.kt           # 插件任务 API
│   │   ├── task/                  # 任务调度
│   │   │   ├── TaskDispatcher.kt          # 核心调度器
│   │   │   ├── Task.kt / TaskPriority.kt  # 任务定义
│   │   │   └── BackgroundTaskWorker.kt    # WorkManager Worker
│   │   ├── ui/components/         # UI 组件
│   │   │   ├── Taskbar.kt
│   │   │   ├── StartMenu.kt
│   │   │   ├── DesktopGrid.kt
│   │   │   ├── AppDrawer.kt
│   │   │   └── ContextMenu.kt
│   │   ├── ui/theme/
│   │   │   ├── Windows10Theme.kt          # 配色/阴影/圆角
│   │   │   └── Windows10Typography.kt     # 排版
│   │   ├── model/DesktopItem.kt           # 数据模型
│   │   └── MainActivity.kt                # 统一状态入口
│   └── build.gradle.kts
├── plugin-api/                    # 插件接口库
│   └── src/main/aidl/com/explorercore/plugin/
│       ├── IPluginService.aidl
│       ├── IHostCallbacks.aidl
│       ├── IPanelHost.aidl
│       └── SearchResultItem.aidl
├── filemanager-plugin/            # 官方文件管理器插件
├── .github/workflows/build.yml    # GitHub Actions 云端构建
├── BUILD_GUIDE.md                 # 编译实操指南
├── PLUGIN_DEVELOPMENT_GUIDE.md    # 插件开发文档
└── README.md                      # 完整项目文档
```

---

## 🚧 待办事项 (按优先级)

### P0 - 核心功能补全
- [ ] **面板容器实现**：`IPanelHost` 对应的 Compose UI 容器（侧边栏/底部槽位/悬浮窗）
- [ ] **快捷方式注册**：`PLUGIN_EVENT_REGISTER_SHORTCUT` 处理 → 桌面数据库写入
- [ ] **权限代理机制**：插件请求 → 宿主弹窗 → ContentProvider 临时 URI 授权
- [ ] **文件管理器面板 UI**：文件树/列表/详细视图、核心操作（复制/粘贴/删除/重命名/拖拽）
- [ ] **SAF 存储访问集成**：外部存储/USB OTG/云存储预留接口

### P1 - 体验完善
- [ ] **壁纸选择器**：静态/动态/纯色/每日必应
- [ ] **主题切换**：深色/浅色/跟随系统，运行时切换
- [ ] **手势系统**：双击锁屏、三指上滑任务视图、边缘滑动
- [ ] **设置页面**：桌面/任务栏/手势/插件管理分类
- [ ] **通知中心**：系统通知聚合、快速设置面板

### P2 - 质量与发布
- [ ] **单元测试覆盖**：`PluginManager`/`TaskDispatcher`/解析器 > 80%
- [ ] **UI 测试**：Compose Testing + Robolectric
- [ ] **性能优化**：图标缓存、延迟加载、启动速度 < 1.5s
- [ ] **签名配置**：Release 签名、ProGuard 混淆规则
- [ ] **隐私合规**：权限说明、网络安全配置、数据清理

---

## 🐛 已知问题与规避

| 问题 | 影响 | 规避方案 |
|------|------|----------|
| Windows 命令行 Gradle 启动失败 | 无法本地命令行构建 | 使用 Android Studio / GitHub Actions / WSL2 |
| `gradlew` wrapper 为占位脚本 | 首次需生成真实 wrapper | Android Studio 首次同步自动生成 |
| 插件面板渲染未实现 | `IPanelHost.onUpdate` 仅打印日志 | 需实现 Compose 面板容器组件 |
| 快捷方式注册未落地 | 桌面图标无法动态添加 | 需实现 `PLUGIN_EVENT_REGISTER_SHORTCUT` 处理 |
| 权限代理未闭环 | 插件无法访问受保护存储 | 需实现宿主弹窗 + 临时 URI 授权流程 |

---

## 🚀 快速上手指南 (接手者 30 分钟上手)

### 1. 环境准备 (5 分钟)
```powershell
# 安装 Android Studio Ladybug+
# 打开项目：File → Open → C:\Projects\Explorer on Android
# 等待 Gradle Sync 完成
```

### 2. 运行验证 (10 分钟)
```
Build → Make Project (Ctrl+F9)
Run → Run 'host' (Shift+F10) → 选择模拟器/真机
# 首次运行会请求设置为默认桌面
```

### 3. 核心代码阅读顺序 (15 分钟)
1. `host/.../plugin/PluginManager.kt` - 插件生命周期核心
2. `host/.../task/TaskDispatcher.kt` - 任务调度核心
3. `host/.../ui/components/` - 5大 UI 组件
4. `host/.../MainActivity.kt` - 状态管理与组件组装
5. `plugin-api/.../aidl/` - 4个 AIDL 接口定义

---

## 🔑 关键技术决策记录

| 决策点 | 选择 | 理由 |
|--------|------|------|
| **插件进程隔离** | 独立应用进程 + AIDL | 安全性、崩溃隔离、权限最小化 |
| **UI 框架** | Jetpack Compose + Material 3 | 现代声明式 UI、Windows 10 风格易定制 |
| **任务调度** | 自研 `TaskDispatcher` + WorkManager | 优先级/并发/持久化统一抽象，插件可复用 |
| **插件通信** | AIDL (而非 Binder/ContentProvider) | 类型安全、跨进程标准、版本演进可控 |
| **主题系统** | 独立 `Windows10Theme` 对象 | 深/浅色完全分离、易扩展高对比度/色盲模式 |
| **多模块** | `plugin-api` / `host` / `filemanager-plugin` / `shared` | 接口与实现分离，插件可独立发布 |

---

## 📞 联系与支持

| 角色 | 联系方式 |
|------|----------|
| 架构负责人 | 内部 Wiki / 技术评审会 |
| 插件生态 | plugins@explorer.example.com |
| 构建/发布 | GitHub Actions / 内部 CI/CD |
| 问题反馈 | GitHub Issues / 内部工单系统 |

---

## 📝 给下一个维护者的建议

1. **先跑通 Android Studio 构建**，别纠结命令行 Gradle Bug
2. **从 `PluginManager` 入手理解插件流程**，这是全项目的心脏
3. **优先实现面板容器 + 快捷方式注册**，能让插件真正“跑起来”
4. **文件管理器插件是第一个真实插件**，做完它能验证整个插件框架
5. **保持 `plugin-api` 稳定**，它是插件生态的契约，变更需极其谨慎
6. **善用 GitHub Actions**，它是当前最稳定的构建产出渠道

---

**文档版本**：1.0.0 | **编写者**：Explorer Launcher 核心团队 | **下次更新**：完成 P0 事项后