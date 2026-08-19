// host/src/main/java/com/explorer/launcher/host/plugin/PluginTaskApi.kt
package com.explorer.launcher.host.plugin

import android.os.Bundle
import com.explorer.launcher.host.task.TaskDispatcher
import com.explorer.launcher.host.task.TaskPriority
import com.explorer.launcher.host.task.TaskResult
import kotlinx.coroutines.Deferred

/**
 * 插件任务 API
 * 宿主通过 IHostCallbacks 暴露给插件，允许插件提交后台任务到宿主的 TaskDispatcher
 * 实现插件与宿主的任务卸载、并发控制
 */
interface PluginTaskApi {
    /**
     * 提交任务到宿主调度器
     * @param taskName 任务名称
     * @param priority 优先级
     * @param payload 负载数据
     * @param executor 任务执行器（在宿主进程中运行）
     * @return 可等待的结果
     */
    fun submitTask(
        taskName: String,
        priority: TaskPriority,
        payload: Bundle,
        executor: PluginTaskExecutor
    ): Deferred<TaskResult<Any>>

    /**
     * 提交简单任务（使用默认优先级 NORMAL）
     */
    fun submitSimpleTask(
        taskName: String,
        payload: Bundle,
        executor: PluginTaskExecutor
    ): Deferred<TaskResult<Any>>

    /**
     * 取消任务
     */
    fun cancelTask(taskId: String): Boolean

    /**
     * 获取任务状态
     */
    fun getTaskState(taskId: String): TaskState?

    /**
     * 获取任务结果
     */
    fun getTaskResult(taskId: String): TaskResult<Any>?

    /**
     * 调度持久化后台任务（WorkManager）
     */
    fun scheduleBackgroundTask(
        taskType: String,
        payload: String,
        delayMillis: Long,
        requiresCharging: Boolean,
        requiresNetwork: Boolean
    )

    /**
     * 调度周期性后台任务
     */
    fun schedulePeriodicBackgroundTask(
        taskType: String,
        intervalMillis: Long,
        payload: String,
        requiresCharging: Boolean,
        requiresNetwork: Boolean
    )
}

/**
 * 插件任务执行器接口
 * 插件实现此接口，代码在宿主进程中运行（通过 AIDL 传递或共享库）
 * 注意：由于跨进程限制，实际实现建议使用共享库或将逻辑放在宿主侧
 */
interface PluginTaskExecutor {
    /** 执行任务 */
    fun execute(context: PluginTaskContext): TaskResult<Any>
}

/**
 * 插件任务上下文（提供给插件任务执行器）
 */
interface PluginTaskContext {
    val taskId: String
    val taskName: String
    val payload: Bundle
    val isCancelled: Boolean
    fun updateProgress(progress: Int, message: String?)
    fun shouldYield(): Boolean
}

/**
 * 任务状态枚举（对外暴露）
 */
enum class TaskState {
    PENDING, QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT
}

/**
 * 任务优先级（对外暴露）
 */
enum class TaskPriority(val weight: Int) {
    LOW(0), NORMAL(5), HIGH(10), CRITICAL(20)
}

/**
 * 任务结果（对外暴露）
 */
sealed class TaskResult<out T> {
    data class Success<T>(val data: T) : TaskResult<T>()
    data class Failure(val error: String, val isRetryable: Boolean = true) : TaskResult<Nothing>()
    object Cancelled : TaskResult<Nothing>()

    companion object {
        fun <T> success(data: T) = Success(data)
        fun <T> failure(error: String, isRetryable: Boolean = true) = Failure(error, isRetryable)
        fun <T> cancelled() = Cancelled
    }
}