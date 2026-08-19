# Explorer on Android — APK 编译教程

> 写给开发 AI 的实操指南 | 2026-08-20 | 已在本机验证可行

## 一、环境（本机已验证）

| 组件 | 路径 / 版本 |
|------|-------------|
| JDK | **必须用 17**：`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot` |
| Android SDK | `C:\Android`（ANDROID_HOME 已设置，platforms 34/35/36，build-tools 33/35/36 已自动装） |
| Gradle | **8.13**：`C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat` |
| 项目 | `C:\Projects\Explorer on Android` |

## 二、关键前提（为什么必须这样配）

1. **JDK 必须 17**：JAVA_HOME 默认指向 JDK 21，而 Kotlin 1.9.22 不认识 JVM target 21，会报 `Unknown Kotlin JVM target: 21`。构建时必须把 JAVA_HOME 指到 JDK 17。
2. **Gradle 必须 8.13**：项目 wrapper 原要求 Gradle 8.0（本机无缓存，下载慢），已把 `gradle/wrapper/gradle-wrapper.properties` 的 distributionUrl 改为 `gradle-8.13-bin.zip`（本地缓存已有，免下载）。**不要再改回去**。
3. **Kotlin 1.9.22 + Compose Compiler 1.5.10**：这是匹配组合（根 build.gradle.kts 已改）。

## 三、编译命令（Windows PowerShell / CMD 均可）

### 方式 A：直接用本地 Gradle（推荐，免 wrapper 下载）

```powershell
# PowerShell（注意：不要用 irm|iex 之类，直接跑 gradle.bat）
cd "C:\Projects\Explorer on Android"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
& "C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat" assembleDebug --no-daemon
```

### 方式 B：用 wrapper（gradlew.bat，会自动用 8.13）

```powershell
cd "C:\Projects\Explorer on Android"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
.\gradlew.bat assembleDebug
```

### 建议：输出重定向到日志，避免输出被截断

```powershell
cd "C:\Projects\Explorer on Android"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
& "...\gradle.bat" assembleDebug --no-daemon *> build.log
# 构建完成后看结果：
Select-String -Path build.log -Pattern "BUILD SUCCESSFUL|BUILD FAILED"
```

## 四、构建产物位置

| 模块 | APK 路径 |
|------|----------|
| host（主应用） | `host\build\outputs\apk\debug\host-debug.apk` |
| filemanager-plugin | `filemanager-plugin\build\outputs\apk\debug\filemanager-plugin-debug.apk` |

## 五、常见坑（已踩过，别重复）

1. **PowerShell 管道污染退出码**：`gradle ... 2>&1 | Select-Object` 会导致 `$LASTEXITCODE` 不可靠。判断成败**只看日志**里的 `BUILD SUCCESSFUL / BUILD FAILED` 字符串，或重定向到文件后 grep。
2. **PowerShell 的 `curl` 是别名**（Invoke-WebRequest），不是 curl.exe。需要 curl 时写 `curl.exe`。
3. **PowerShell 命令里 `$` 变量转义**：通过外层 shell 传 `$env:...` 时可能被吞掉，用 Python `subprocess.Popen(..., env=...)` 或直接写脚本文件最稳。
4. **首次构建慢**：要下载依赖（google/mavenCentral 仓库），国内网络可能 5-10 分钟，不是卡死。看日志尾部是否在推进。
5. **`--no-daemon` 单次构建**：避免后台 daemon 占用内存和锁。
6. **JDK 21 千万别用**：报 `Unknown Kotlin JVM target: 21`（Kotlin 1.9.22 不支持）。
7. **AGP 8.1 + compileSdk 34 警告**：`This Android Gradle plugin was tested up to compileSdk = 33` 是警告不是错误，可构建。嫌吵可在 `gradle.properties` 加 `android.suppressUnsupportedCompileSdk=34`。

## 六、快速验证是否成功

```powershell
# 构建后检查 APK 是否存在
Test-Path "C:\Projects\Explorer on Android\host\build\outputs\apk\debug\host-debug.apk"
# 用 aapt 看包信息
& "C:\Android\build-tools\36.0.0\aapt.exe" dump badging "C:\Projects\Explorer on Android\host\build\outputs\apk\debug\host-debug.apk" | Select-String "package:|application-label"
```

## 七、修完源码错误后的构建步骤（给开发 AI 的验收流程）

1. 按 `BUILD_ERRORS.md` 修复全部 21 个编译错误
2. 跑第三节的命令构建
3. 若再报错：看 `build.log` 里的 `e:` 开头的行（Kotlin 错误格式：`文件:行:列 错误描述`）
4. 直到 `BUILD SUCCESSFUL` + APK 存在 = 完成
