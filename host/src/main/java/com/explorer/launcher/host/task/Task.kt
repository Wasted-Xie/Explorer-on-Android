// host/src/main/java/com/explorer/launcher/host/task/Task.kt
package com.explorer.launcher.host.task

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 任务定义
 * @param id 唯一任务 ID
 * @param name 任务名称（用于调试）
 * @param priority 优先级
 * @param payload 任务负载数据
 * @param tags 标签（用于分组/过滤）
 * @param timeoutMs 超时时间（毫秒），0 表示无超时
 * @param retryCount 重试次数
 * @param requiresNetwork 是否需要网络
 * @param requiresCharging 是否需要充电状态
 */
data class Task(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val payload: Bundle = Bundle(),
    val tags: Set<String> = emptySet(),
    val timeoutMs: Long = 0,
    val retryCount: Int = 0,
    val requiresNetwork: Boolean = false,
    val requiresCharging: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString()!!,
        name = parcel.readString()!!,
        priority = TaskPriority.values()[parcel.readInt()],
        payload = parcel.readBundle()!!,
        tags = parcel.createStringArray()?.toSet() ?: emptySet(),
        timeoutMs = parcel.readLong(),
        retryCount = parcel.readInt(),
        requiresNetwork = parcel.readByte() != 0.toByte(),
        requiresCharging = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(name)
        dest.writeInt(priority.ordinal)
        dest.writeBundle(payload)
        dest.writeStringArray(tags.toTypedArray())
        dest.writeLong(timeoutMs)
        dest.writeInt(retryCount)
        dest.writeByte(if (requiresNetwork) 1 else 0)
        dest.writeByte(if (requiresCharging) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Task> = object : Parcelable.Creator<Task> {
            override fun createFromParcel(parcel: Parcel): Task = Task(parcel)
            override fun newArray(size: Int): Array<Task?> = arrayOfNulls(size)
        }
    }
}

/**
 * 任务执行结果
 */
sealed class TaskResult<out T> {
    data class Success<T>(val data: T) : TaskResult<T>()
    data class Failure(val error: Throwable, val isRetryable: Boolean = true) : TaskResult<Nothing>()
    object Cancelled : TaskResult<Nothing>()

    companion object {
        fun <T> success(data: T) = Success(data)
        fun <T> failure(error: Throwable, isRetryable: Boolean = true) = Failure(error, isRetryable)
        fun <T> cancelled() = Cancelled
    }
}

/**
 * 任务状态
 */
enum class TaskState {
    PENDING,      // 等待调度
    QUEUED,       // 已入队
    RUNNING,      // 执行中
    COMPLETED,    // 成功完成
    FAILED,       // 失败（可重试）
    CANCELLED,    // 已取消
    TIMEOUT       // 超时
}

/**
 * 任务执行上下文，提供给任务实现使用
 */
interface TaskContext {
    /** 任务 ID */
    val taskId: String
    /** 任务名称 */
    val taskName: String
    /** 任务负载 */
    val payload: Bundle
    /** 是否已被取消 */
    val isCancelled: Boolean
    /** 当前重试次数 */
    val currentRetry: Int
    /** 最大重试次数 */
    val maxRetries: Int
    /** 更新进度 (0-100) */
    fun updateProgress(progress: Int, message: String? = null)
    /** 检查是否应该退出 */
    fun shouldYield(): Boolean
    /** 添加子任务 */
    fun <R> addSubTask(subTask: Task, executor: (TaskContext) -> TaskResult<R>): Deferred<TaskResult<R>>
}

/**
 * 任务执行器接口
 * 插件或宿主组件实现此接口来定义具体任务逻辑
 */
@FunctionalInterface
interface TaskExecutor {
    /** 执行任务，返回结果 */
    fun execute(context: TaskContext): TaskResult<Any>
}

/**
 * 任务监听器
 */
interface TaskListener {
    fun onTaskStateChanged(taskId: String, oldState: TaskState, newState: TaskState)
    fun onTaskProgress(taskId: String, progress: Int, message: String?)
    fun onTaskCompleted(taskId: String, result: TaskResult<Any>)
}