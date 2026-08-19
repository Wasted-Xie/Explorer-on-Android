// host/src/main/java/com/explorer/launcher/host/task/TaskDispatcher.kt
package com.explorer.launcher.host.task

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 任务调度器：核心组件，负责任务队列管理、并发控制、优先级调度、生命周期绑定
 * 单例，由 Hilt 注入
 */
class TaskDispatcher @javax.inject.Inject constructor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private const val TAG = "TaskDispatcher"

    // 配置参数
    private val maxConcurrentTasks = 8                    // 最大并发任务数
    private val maxConcurrentHighPriority = 4             // 高优先级最大并发
    private val defaultTimeoutMs = 30_000L                // 默认 30 秒超时
    private val cleanupIntervalMs = 5 * 60 * 1000L        // 5 分钟清理一次完成任务

    // 状态
    private val pendingQueue = MutablePriorityQueue<TaskWrapper>()
    private val runningTasks = ConcurrentHashMap<String, TaskWrapper>()
    private val completedTasks = ConcurrentHashMap<String, TaskResult<Any>>()
    private val taskListeners = MutableList<TaskListener>()
    private val listenerMutex = Mutex()

    // 统计
    private val submittedCount = AtomicLong(0)
    private val completedCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)

    // 信号量控制并发
    private val globalSemaphore = Semaphore(maxConcurrentTasks)
    private val highPrioritySemaphore = Semaphore(maxConcurrentHighPriority)

    // 生命周期感知
    private var isShutdown = false

    init {
        // 启动调度循环
        scope.launch(Dispatchers.Default) { schedulerLoop() }
        // 定期清理
        scope.launch(Dispatchers.IO) { cleanupLoop() }
    }

    /**
     * 提交任务执行
     * @return Deferred<TaskResult<Any>> 可用于等待结果
     */
    fun submit(task: Task, executor: TaskExecutor): Deferred<TaskResult<Any>> {
        if (isShutdown) {
            return CoroutineScope(Dispatchers.IO).async { TaskResult.failure(IllegalStateException("Dispatcher shutdown")) }
        }

        val wrapper = TaskWrapper(task, executor)
        submittedCount.incrementAndGet()

        // 入队
        pendingQueue.add(wrapper)
        wrapper.state = TaskState.QUEUED
        notifyStateChanged(wrapper, TaskState.PENDING, TaskState.QUEUED)

        Log.d(TAG, "Task submitted: ${task.name} [${task.id}] priority=${task.priority}")

        // 返回可等待的 Deferred
        return wrapper.deferred
    }

    /**
     * 便捷方法：提交简单任务
     */
    fun submitSimple(
        name: String,
        priority: TaskPriority = TaskPriority.NORMAL,
        payload: Bundle = Bundle(),
        executor: suspend TaskContext.() -> TaskResult<Any>
    ): Deferred<TaskResult<Any>> {
        val task = Task(name = name, priority = priority, payload = payload)
        return submit(task, object : TaskExecutor {
            override fun execute(context: TaskContext): TaskResult<Any> {
                // 将 suspend 函数包装为同步执行
                return runBlocking { context.runSuspendTask(executor) }
            }
        })
    }

    /**
     * 取消任务
     */
    fun cancel(taskId: String, reason: String = "Cancelled by user"): Boolean {
        // 尝试从待执行队列移除
        val removed = pendingQueue.removeIf { it.task.id == taskId }
        if (removed) {
            notifyStateChanged(TaskWrapper(Task(id = taskId, name = ""), object : TaskExecutor {
                override fun execute(context: TaskContext) = TaskResult.cancelled()
            }), TaskState.QUEUED, TaskState.CANCELLED)
            return true
        }

        // 尝试取消正在运行的任务
        val wrapper = runningTasks[taskId]
        if (wrapper != null) {
            wrapper.cancel(reason)
            return true
        }
        return false
    }

    /**
     * 取消指定标签的所有任务
     */
    fun cancelByTag(tag: String) {
        pendingQueue.removeIf { wrapper ->
            if (tag in wrapper.task.tags) {
                notifyStateChanged(wrapper, TaskState.QUEUED, TaskState.CANCELLED)
                true
            } else false
        }
        runningTasks.values.forEach { wrapper ->
            if (tag in wrapper.task.tags) {
                wrapper.cancel("Cancelled by tag: $tag")
            }
        }
    }

    /**
     * 获取任务状态
     */
    fun getState(taskId: String): TaskState? {
        return pendingQueue.find { it.task.id == taskId }?.state
            ?: runningTasks[taskId]?.state
            ?: completedTasks[taskId]?.let { 
                when (it) {
                    is TaskResult.Success -> TaskState.COMPLETED
                    is TaskResult.Failure -> TaskState.FAILED
                    is TaskResult.Cancelled -> TaskState.CANCELLED
                }
            }
    }

    /**
     * 获取任务结果（如果已完成）
     */
    fun getResult(taskId: String): TaskResult<Any>? = completedTasks[taskId]

    /**
     * 获取所有运行中任务
     */
    fun getRunningTasks(): List<Task> = runningTasks.values.map { it.task }

    /**
     * 获取队列大小
     */
    fun getQueueSize(): Int = pendingQueue.size

    /**
     * 添加监听器
     */
    fun addListener(listener: TaskListener) {
        listenerMutex.withLock { taskListeners.add(listener) }
    }

    /**
     * 移除监听器
     */
    fun removeListener(listener: TaskListener) {
        listenerMutex.withLock { taskListeners.remove(listener) }
    }

    /**
     * 绑定到生命周期（自动取消任务）
     */
    fun bindToLifecycle(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    cancelAll("Lifecycle destroyed")
                }
            }
        })
    }

    /**
     * 取消所有任务
     */
    fun cancelAll(reason: String = "Shutdown") {
        isShutdown = true
        pendingQueue.clear()
        runningTasks.values.forEach { it.cancel(reason) }
        globalSemaphore.close()
        highPrioritySemaphore.close()
    }

    /**
     * 获取统计信息
     */
    fun getStats(): TaskStats {
        return TaskStats(
            submitted = submittedCount.get(),
            completed = completedCount.get(),
            failed = failedCount.get(),
            running = runningTasks.size,
            queued = pendingQueue.size
        )
    }

    // ========== 内部调度循环 ==========

    private suspend fun schedulerLoop() {
        while (!isShutdown) {
            // 等待有任务且有可用槽位
            val wrapper = pendingQueue.pollOrWait()
            if (wrapper == null) {
                // 队列关闭或 shutdown
                break
            }

            // 检查是否已取消
            if (wrapper.isCancelled) {
                wrapper.complete(TaskResult.cancelled())
                continue
            }

            // 获取信号量
            val isHighPriority = wrapper.task.priority.weight >= TaskPriority.HIGH.weight
            val semaphore = if (isHighPriority) highPrioritySemaphore else globalSemaphore

            semaphore.withPermit {
                if (isShutdown || wrapper.isCancelled) {
                    wrapper.complete(TaskResult.cancelled())
                    return@withPermit
                }

                // 标记为运行中
                wrapper.state = TaskState.RUNNING
                runningTasks[wrapper.task.id] = wrapper
                notifyStateChanged(wrapper, TaskState.QUEUED, TaskState.RUNNING)

                // 执行任务（在 IO 线程池）
                scope.launch(Dispatchers.IO) {
                    executeTask(wrapper)
                }
            }
        }
    }

    private suspend fun executeTask(wrapper: TaskWrapper) {
        val task = wrapper.task
        val context = TaskContextImpl(wrapper)

        // 超时控制
        val timeout = if (task.timeoutMs > 0) task.timeoutMs else defaultTimeoutMs
        val result = withTimeoutOrNull(timeout) {
            wrapper.executor.execute(context)
        } ?: TaskResult.failure(TimeoutException("Task timeout after ${timeout}ms"), isRetryable = true)

        // 处理重试
        if (result is TaskResult.Failure && result.isRetryable && context.currentRetry < task.retryCount) {
            val retryWrapper = TaskWrapper(
                task.copy(payload = task.payload.apply { putInt("__retry", context.currentRetry + 1) }),
                wrapper.executor
            )
            pendingQueue.add(retryWrapper)
            retryWrapper.state = TaskState.QUEUED
            notifyStateChanged(retryWrapper, TaskState.FAILED, TaskState.QUEUED)
            Log.d(TAG, "Task ${task.name} retry ${context.currentRetry + 1}/${task.retryCount}")
        } else {
            // 完成
            wrapper.complete(result)
            runningTasks.remove(task.id)
            completedTasks[task.id] = result

            when (result) {
                is TaskResult.Success -> completedCount.incrementAndGet()
                is TaskResult.Failure -> failedCount.incrementAndGet()
                is TaskResult.Cancelled -> {}
            }

            notifyStateChanged(wrapper, TaskState.RUNNING, when (result) {
                is TaskResult.Success -> TaskState.COMPLETED
                is TaskResult.Failure -> TaskState.FAILED
                is TaskResult.Cancelled -> TaskState.CANCELLED
            })
            notifyCompleted(wrapper.task.id, result)
        }
    }

    private suspend fun cleanupLoop() {
        while (!isShutdown) {
            delay(cleanupIntervalMs)
            // 清理超过 1 小时的完成任务结果
            val cutoff = System.currentTimeMillis() - 3600_000
            // 实际实现需要记录完成时间，这里简化
            if (completedTasks.size > 1000) {
                // 保留最近 500 个
                val toRemove = completedTasks.keys.take(completedTasks.size - 500)
                toRemove.forEach { completedTasks.remove(it) }
            }
        }
    }

    // ========== 通知方法 ==========

    private fun notifyStateChanged(wrapper: TaskWrapper, oldState: TaskState, newState: TaskState) {
        wrapper.state = newState
        listenerMutex.withLock {
            taskListeners.forEach { listener ->
                try { listener.onTaskStateChanged(wrapper.task.id, oldState, newState) } catch (e: Exception) {
                    Log.e(TAG, "Listener error", e)
                }
            }
        }
    }

    private fun notifyProgress(taskId: String, progress: Int, message: String?) {
        listenerMutex.withLock {
            taskListeners.forEach { listener ->
                try { listener.onTaskProgress(taskId, progress, message) } catch (e: Exception) {
                    Log.e(TAG, "Listener error", e)
                }
            }
        }
    }

    private fun notifyCompleted(taskId: String, result: TaskResult<Any>) {
        listenerMutex.withLock {
            taskListeners.forEach { listener ->
                try { listener.onTaskCompleted(taskId, result) } catch (e: Exception) {
                    Log.e(TAG, "Listener error", e)
                }
            }
        }
    }

    // ========== 内部类 ==========

    private data class TaskWrapper(
        val task: Task,
        val executor: TaskExecutor
    ) {
        var state: TaskState = TaskState.PENDING
        private val _cancelled = atomic(false)
        val deferred: CompletableDeferred<TaskResult<Any>> = CompletableDeferred()
        var job: Job? = null

        val isCancelled: Boolean get() = _cancelled.get()

        fun cancel(reason: String) {
            if (_cancelled.compareAndSet(false, true)) {
                job?.cancel(reason)
                deferred.complete(TaskResult.failure(CancellationException(reason), false))
            }
        }

        fun complete(result: TaskResult<Any>) {
            deferred.complete(result)
        }
    }

    private class TaskContextImpl(private val wrapper: TaskWrapper) : TaskContext {
        private val _progress = AtomicInteger(0)
        private val _currentRetry = AtomicInteger(
            wrapper.task.payload.getInt("__retry", 0)
        )

        override val taskId: String = wrapper.task.id
        override val taskName: String = wrapper.task.name
        override val payload: Bundle = wrapper.task.payload
        override val isCancelled: Boolean = wrapper.isCancelled
        override val currentRetry: Int = _currentRetry.get()
        override val maxRetries: Int = wrapper.task.retryCount

        override fun updateProgress(progress: Int, message: String?) {
            _progress.set(progress.coerceIn(0, 100))
            notifyProgress(taskId, _progress.get(), message)
        }

        override fun shouldYield(): Boolean = isCancelled || Thread.interrupted()

        override fun <R> addSubTask(subTask: Task, executor: (TaskContext) -> TaskResult<R>): Deferred<TaskResult<R>> {
            // 这里需要访问外部的 TaskDispatcher，简化实现
            return CoroutineScope(Dispatchers.IO).async {
                val subContext = TaskContextImpl(TaskWrapper(subTask, object : TaskExecutor {
                    override fun execute(context: TaskContext) = executor(context)
                }))
                executor(subContext)
            }
        }

        /** 在 TaskContext 中运行 suspend 函数 */
        suspend fun runSuspendTask(block: suspend TaskContext.() -> TaskResult<Any>): TaskResult<Any> {
            return block(this)
        }
    }
}

/**
 * 优先级队列：按优先级排序，同优先级 FIFO
 */
private class MutablePriorityQueue<T : TaskWrapper> {
    private val queue = java.util.PriorityQueue<T>(compareByDescending { it.task.priority.weight }
        .thenBy { it.task.id }) // 同优先级按 ID 排序保证 FIFO
    private val notEmpty = Channel<Unit>(Channel.UNLIMITED)

    fun add(item: T) {
        queue.add(item)
        notEmpty.trySend(Unit)
    }

    fun pollOrWait(): T? = runBlocking {
        while (queue.isEmpty()) {
            notEmpty.receiveOrNull() ?: return@runBlocking null // channel closed
        }
        queue.poll()
    }

    fun removeIf(predicate: (T) -> Boolean): Boolean {
        return queue.removeIf(predicate)
    }

    fun clear() { queue.clear() }

    val size: Int get() = queue.size

    fun find(predicate: (T) -> Boolean): T? = queue.firstOrNull(predicate)
}

/**
 * 任务统计数据类
 */
data class TaskStats(
    val submitted: Long,
    val completed: Long,
    val failed: Long,
    val running: Int,
    val queued: Int
)