// host/src/main/java/com/explorer/launcher/host/task/BackgroundTaskWorker.kt
package com.explorer.launcher.host.task

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager 后台任务 Worker
 * 用于需要持久化、延迟执行、重试的任务（如插件索引构建、缓存清理、数据同步）
 */
class BackgroundTaskWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private const val TAG = "BackgroundTaskWorker"
    private const val KEY_TASK_TYPE = "task_type"
    private const val KEY_TASK_PAYLOAD = "task_payload"

    override suspend fun doWork(): Result {
        val taskType = inputData.getString(KEY_TASK_TYPE) ?: return Result.failure()
        val payloadString = inputData.getString(KEY_TASK_PAYLOAD) ?: "{}"

        Log.d(TAG, "Starting background task: $taskType")

        return try {
            withContext(Dispatchers.IO) {
                when (taskType) {
                    "plugin_index_rebuild" -> rebuildPluginIndex()
                    "cache_cleanup" -> cleanupCaches()
                    "search_index_update" -> updateSearchIndex()
                    "plugin_health_check" -> pluginHealthCheck()
                    else -> {
                        Log.w(TAG, "Unknown task type: $taskType")
                        Result.failure()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background task $taskType failed", e)
            // 返回 retry 让 WorkManager 按退避策略重试
            Result.retry()
        }
    }

    private suspend fun rebuildPluginIndex(): Result {
        // TODO: 实际重建插件索引逻辑
        Log.d(TAG, "Rebuilding plugin index...")
        delay(1000) // 模拟耗时操作
        return Result.success()
    }

    private suspend fun cleanupCaches(): Result {
        // TODO: 清理过期缓存
        Log.d(TAG, "Cleaning up caches...")
        delay(500)
        return Result.success()
    }

    private suspend fun updateSearchIndex(): Result {
        // TODO: 更新全局搜索索引
        Log.d(TAG, "Updating search index...")
        delay(2000)
        return Result.success()
    }

    private suspend fun pluginHealthCheck(): Result {
        // TODO: 检查插件健康状态，重启异常插件
        Log.d(TAG, "Running plugin health check...")
        delay(500)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME_PREFIX = "ExplorerBackgroundTask_"

        /**
         * 调度一次性后台任务
         */
        fun enqueueUnique(
            context: Context,
            taskType: String,
            payload: String = "{}",
            delayMillis: Long = 0,
            requiresCharging: Boolean = false,
            requiresNetwork: Boolean = false
        ) {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
                .setInputData(androidx.work.Data.Builder()
                    .putString(KEY_TASK_TYPE, taskType)
                    .putString(KEY_TASK_PAYLOAD, payload)
                    .build())
                .apply {
                    if (delayMillis > 0) {
                        setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                    if (requiresCharging) {
                        setRequiresCharging(true)
                    }
                    if (requiresNetwork) {
                        setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    }
                }
                .build()

            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                "$UNIQUE_WORK_NAME_PREFIX$taskType",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        /**
         * 调度周期性后台任务
         */
        fun enqueuePeriodic(
            context: Context,
            taskType: String,
            intervalMillis: Long,
            flexMillis: Long = intervalMillis / 10,
            payload: String = "{}",
            requiresCharging: Boolean = false,
            requiresNetwork: Boolean = false
        ) {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<BackgroundTaskWorker>(
                intervalMillis, java.util.concurrent.TimeUnit.MILLISECONDS,
                flexMillis, java.util.concurrent.TimeUnit.MILLISECONDS
            )
                .setInputData(androidx.work.Data.Builder()
                    .putString(KEY_TASK_TYPE, taskType)
                    .putString(KEY_TASK_PAYLOAD, payload)
                    .build())
                .apply {
                    if (requiresCharging) {
                        setRequiresCharging(true)
                    }
                    if (requiresNetwork) {
                        setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    }
                }
                .build()

            val workManager = androidx.work.WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                "$UNIQUE_WORK_NAME_PREFIX$taskType",
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }

        /**
         * 取消任务
         */
        fun cancel(context: Context, taskType: String) {
            androidx.work.WorkManager.getInstance(context)
                .cancelUniqueWork("$UNIQUE_WORK_NAME_PREFIX$taskType")
        }

        /**
         * 获取任务状态
         */
        fun getWorkInfo(context: Context, taskType: String): androidx.work.WorkInfo? {
            return androidx.work.WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(
                    androidx.work.WorkManager.getInstance(context)
                        .getWorkInfosForUniqueWork("$UNIQUE_WORK_NAME_PREFIX$taskType")
                        .value?.firstOrNull()?.id
                ).value
        }
    }
}