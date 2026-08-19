// host/src/main/java/com/explorer/launcher/host/task/TaskPriority.kt
package com.explorer.launcher.host.task

/**
 * 任务优先级
 */
enum class TaskPriority(val weight: Int) {
    LOW(0),       // 后台维护、缓存清理
    NORMAL(5),    // 普通 UI 刷新、搜索索引
    HIGH(10),     // 用户交互响应、面板打开
    CRITICAL(20)  // 关键路径：启动插件、权限申请
}