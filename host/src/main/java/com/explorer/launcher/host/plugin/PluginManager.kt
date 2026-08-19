// host/src/main/java/com/explorer/launcher/host/plugin/PluginManager.kt
package com.explorer.launcher.host.plugin

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.explorer.launcher.host.task.TaskDispatcher
import com.explorer.launcher.host.task.TaskPriority
import com.explorercore.plugin.IHostCallbacks
import com.explorercore.plugin.IPluginService
import com.explorercore.plugin.PluginContract
import com.explorercore.plugin.SearchResultItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 插件管理器：负责插件发现、加载、绑定、生命周期管理、事件分发
 * 单例，由 Hilt 注入管理
 */
class PluginManager @javax.inject.Inject constructor(
    private val application: Application,
    private val taskDispatcher: TaskDispatcher
) {
    private const val TAG = "PluginManager"
    private const val PLUGIN_SERVICE_ACTION = "com.explorercore.plugin.BIND_PLUGIN_SERVICE"

    // 插件缓存
    private val plugins = ConcurrentHashMap<String, PluginInfo>()
    private val pluginStates = ConcurrentHashMap<String, PluginRuntimeState>()
    // 存储 ServiceConnection 以便正确解绑
    private val serviceConnections = ConcurrentHashMap<String, ServiceConnection>()

    // 事件监听器
    private val listeners = CopyOnWriteArrayList<PluginEventListener>()

    // 协程作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 宿主回调实现（插件调用此接口与宿主通信）
    private val hostCallbacks = object : IHostCallbacks.Stub() {
        override fun onPluginEvent(eventCode: Int, data: Bundle?) {
            handlePluginEvent(eventCode, data)
        }

        override fun requestPanel(panelId: String, initialArgs: Bundle?): IBinder {
            return PanelHostBinder(panelId, initialArgs).asBinder()
        }

        override fun notifySearchResults(query: String, results: java.util.List<SearchResultItem>) {
            notifySearchResultsReceived(query, results)
        }

        // ========== 任务管理扩展实现 ==========

        override fun submitTask(taskName: String, priority: Int, payload: Bundle, executorType: String): String {
            val taskPriority = when (priority) {
                0 -> TaskPriority.LOW
                5 -> TaskPriority.NORMAL
                10 -> TaskPriority.HIGH
                20 -> TaskPriority.CRITICAL
                else -> TaskPriority.NORMAL
            }

            // 根据 executorType 选择执行器
            val executor = getExecutorByType(executorType)

            val taskId = java.util.UUID.randomUUID().toString()
            taskDispatcher.submit(
                com.explorer.launcher.host.task.Task(
                    id = taskId,
                    name = taskName,
                    priority = taskPriority,
                    payload = payload
                ),
                executor
            )

            // 通知插件任务已提交
            val resultBundle = Bundle().apply {
                putString(PluginContract.EXTRA_TASK_ID, taskId)
                putString(PluginContract.EXTRA_TASK_NAME, taskName)
            }
            // 这里需要通过插件的回调通知，但 IPluginService 是单向的
            // 实际实现中可以通过 eventCode 通知
            return taskId
        }

        override fun cancelTask(taskId: String): Boolean {
            return taskDispatcher.cancel(taskId, "Cancelled by plugin")
        }

        override fun getTaskState(taskId: String): Int {
            val state = taskDispatcher.getState(taskId)
            return when (state) {
                com.explorer.launcher.host.task.TaskState.PENDING -> 0
                com.explorer.launcher.host.task.TaskState.QUEUED -> 1
                com.explorer.launcher.host.task.TaskState.RUNNING -> 2
                com.explorer.launcher.host.task.TaskState.COMPLETED -> 3
                com.explorer.launcher.host.task.TaskState.FAILED -> 4
                com.explorer.launcher.host.task.TaskState.CANCELLED -> 5
                com.explorer.launcher.host.task.TaskState.TIMEOUT -> 6
                else -> -1
            }
        }

        override fun scheduleBackgroundTask(
            taskType: String,
            payload: String,
            delayMillis: Long,
            requiresCharging: Boolean,
            requiresNetwork: Boolean
        ) {
            com.explorer.launcher.host.task.BackgroundTaskWorker.enqueueUnique(
                application,
                taskType,
                payload,
                delayMillis,
                requiresCharging,
                requiresNetwork
            )
        }

        override fun schedulePeriodicBackgroundTask(
            taskType: String,
            intervalMillis: Long,
            payload: String,
            requiresCharging: Boolean,
            requiresNetwork: Boolean
        ) {
            com.explorer.launcher.host.task.BackgroundTaskWorker.enqueuePeriodic(
                application,
                taskType,
                intervalMillis,
                intervalMillis / 10,
                payload,
                requiresCharging,
                requiresNetwork
            )
        }

        private fun getExecutorByType(type: String): com.explorer.launcher.host.task.TaskExecutor {
            return when (type) {
                "plugin_search" -> object : com.explorer.launcher.host.task.TaskExecutor {
                    override fun execute(context: com.explorer.launcher.host.task.TaskContext) =
                        com.explorer.launcher.host.task.TaskResult.success(Unit)
                }
                "plugin_index" -> object : com.explorer.launcher.host.task.TaskExecutor {
                    override fun execute(context: com.explorer.launcher.host.task.TaskContext) =
                        com.explorer.launcher.host.task.TaskResult.success(Unit)
                }
                "plugin_io" -> object : com.explorer.launcher.host.task.TaskExecutor {
                    override fun execute(context: com.explorer.launcher.host.task.TaskContext) =
                        com.explorer.launcher.host.task.TaskResult.success(Unit)
                }
                else -> object : com.explorer.launcher.host.task.TaskExecutor {
                    override fun execute(context: com.explorer.launcher.host.task.TaskContext) =
                        com.explorer.launcher.host.task.TaskResult.failure(
                            Exception("Unknown executor type: $type"))
                }
            }
        }
    }

    /**
     * 初始化：扫描并加载所有已安装插件
     */
    fun initialize() {
        // 使用 TaskDispatcher 进行插件发现，设为 HIGH 优先级
        taskDispatcher.submitSimple(
            name = "PluginDiscovery",
            priority = TaskPriority.HIGH,
            executor = { discoverAndLoadPlugins() }
        )
    }

    /**
     * 扫描已安装应用，发现并解析插件（在后台线程执行）
     */
    private suspend fun discoverAndLoadPlugins(): TaskResult<Any> {
        return try {
            val pm = application.packageManager
            val intent = Intent(PLUGIN_SERVICE_ACTION)
            val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)

            val discoveredPlugins = mutableListOf<PluginInfo>()

            // 并发解析每个插件的描述符
            val parseResults = resolveInfos.map { resolveInfo ->
                taskDispatcher.submitSimple(
                    name = "ParsePluginDescriptor",
                    priority = TaskPriority.NORMAL,
                    executor = { ctx ->
                        val serviceInfo = resolveInfo.serviceInfo
                        val packageName = serviceInfo.packageName
                        val serviceName = serviceInfo.name

                        try {
                            val pluginInfo = PluginDescriptorParser.parse(application, packageName)
                            // 版本兼容性检查
                            if (!pluginInfo.satisfiesVersion(BuildConfig.VERSION_NAME)) {
                                Log.w(TAG, "Plugin ${pluginInfo.id} requires host version ${pluginInfo.minHostVersion}, current ${BuildConfig.VERSION_NAME}")
                                TaskResult.success<Any>(null)
                            } else {
                                TaskResult.success(pluginInfo)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse plugin descriptor for $packageName", e)
                            TaskResult.failure(e)
                        }
                    }
                )
            }.awaitAll()

            for (result in parseResults) {
                if (result is TaskResult.Success && result.data != null) {
                    discoveredPlugins.add(result.data as PluginInfo)
                    Log.d(TAG, "Discovered plugin: ${(result.data as PluginInfo).id} v${(result.data as PluginInfo).version}")
                }
            }

            // 更新缓存（在主线程安全更新）
            plugins.clear()
            for (plugin in discoveredPlugins) {
                plugins[plugin.id] = plugin
                pluginStates[plugin.id] = PluginRuntimeState(plugin.id)
            }

            // 通知监听器
            notifyPluginsChanged()

            // 并发绑定启用的插件
            val bindTasks = discoveredPlugins.filter { it.isEnabled }.map { plugin ->
                taskDispatcher.submitSimple(
                    name = "BindPlugin_${plugin.id}",
                    priority = TaskPriority.HIGH,
                    executor = { bindPluginServiceAsync(plugin) }
                )
            }.awaitAll()

            TaskResult.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Plugin discovery failed", e)
            TaskResult.failure(e)
        }
    }

    /**
     * 绑定插件服务（同步版本，用于内部调用）
     */
    private fun bindPluginService(plugin: PluginInfo) {
        scope.launch { bindPluginServiceAsync(plugin) }
    }

    /**
     * 绑定插件服务（异步版本，用于 TaskDispatcher）
     */
    private suspend fun bindPluginServiceAsync(plugin: PluginInfo): TaskResult<Any> {
        val state = pluginStates[plugin.id] ?: return TaskResult.success(Unit)
        if (state.isBound) return TaskResult.success(Unit)

        return suspendCancellableCoroutine { cont ->
            val intent = Intent(PLUGIN_SERVICE_ACTION)
            intent.component = ComponentName(plugin.packageName, plugin.serviceName)

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    val pluginService = IPluginService.Stub.asInterface(service)
                    state.service = pluginService
                    state.isBound = true
                    state.boundTime = System.currentTimeMillis()
                    state.lastError = null

                    // 初始化插件：传递宿主回调
                    try {
                        pluginService.initialize(hostCallbacks)
                        Log.d(TAG, "Plugin ${plugin.id} initialized")
                    } catch (e: RemoteException) {
                        Log.e(TAG, "Failed to initialize plugin ${plugin.id}", e)
                        state.lastError = e.message
                        state.isBound = false
                        cont.resume(TaskResult.failure(e))
                        return@ServiceConnection
                    }

                    // 更新插件信息
                    plugin.isLoaded = true
                    notifyPluginStateChanged(plugin.id)
                    cont.resume(TaskResult.success(Unit))
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    state.service = null
                    state.isBound = false
                    plugin.isLoaded = false
                    Log.w(TAG, "Plugin ${plugin.id} disconnected")
                    notifyPluginStateChanged(plugin.id)
                }

                override fun onBindingDied(name: ComponentName) {
                    onServiceDisconnected(name)
                }
            }

            // 保存 connection 引用以便后续解绑
            serviceConnections[plugin.id] = connection

            val success = application.bindService(
                intent,
                connection,
                Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE
            )

            if (!success) {
                Log.e(TAG, "Failed to bind to plugin ${plugin.id}")
                state.lastError = "Bind service returned false"
                serviceConnections.remove(plugin.id)
                cont.resume(TaskResult.failure(Exception("Bind service returned false")))
            }

            // 取消时解绑
            cont.invokeOnCancellation {
                application.unbindService(connection)
                serviceConnections.remove(plugin.id)
            }
        }
    }

    /**
     * 解绑插件服务
     */
    fun unbindPlugin(pluginId: String) {
        val state = pluginStates[pluginId] ?: return
        if (!state.isBound) return

        val connection = serviceConnections.remove(pluginId)
        if (connection != null) {
            application.unbindService(connection)
        }

        state.service = null
        state.isBound = false
        val plugin = plugins[pluginId]
        plugin?.isLoaded = false
        notifyPluginStateChanged(pluginId)
    }

    /**
     * 处理插件发来的事件
     */
    private fun handlePluginEvent(eventCode: Int, data: Bundle?) {
        when (eventCode) {
            PluginContract.PLUGIN_EVENT_PANEL_READY -> {
                val pluginId = data?.getString("plugin_id") ?: ""
                Log.d(TAG, "Plugin panel ready: $pluginId")
                notifyPanelReady(pluginId)
            }
            PluginContract.PLUGIN_EVENT_SHOW_TOAST -> {
                val msg = data?.getString("message") ?: ""
                notifyToast(msg)
            }
            PluginContract.PLUGIN_EVENT_REQUEST_PERMISSION -> {
                val pluginId = data?.getString("plugin_id") ?: ""
                val perms = data?.getStringArrayList(PluginContract.EXTRA_PERMISSIONS) ?: emptyList()
                notifyPermissionRequested(pluginId, perms)
            }
            PluginContract.PLUGIN_EVENT_PANEL_RESIZE_REQUEST -> {
                val pluginId = data?.getString("plugin_id") ?: ""
                val width = data?.getInt("width") ?: -1
                val height = data?.getInt("height") ?: -1
                notifyPanelResizeRequest(pluginId, width, height)
            }
            PluginContract.PLUGIN_EVENT_SEARCH_QUERY -> {
                val pluginId = data?.getString("plugin_id") ?: ""
                val query = data?.getString(PluginContract.EXTRA_SEARCH_QUERY) ?: ""
                notifyPluginSearchQuery(pluginId, query)
            }
        }
    }

    /**
     * 向插件发送宿主事件
     */
    fun sendHostEvent(pluginId: String, eventCode: Int, data: Bundle? = null) {
        val state = pluginStates[pluginId]
        val service = state?.service
        if (service != null && state.isBound) {
            try {
                service.onHostEvent(eventCode, data)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to send event to plugin $pluginId", e)
            }
        }
    }

    /**
     * 请求插件创建面板
     */
    fun requestPanel(pluginId: String, panelId: String, args: Bundle? = null): IBinder? {
        val state = pluginStates[pluginId]
        val service = state?.service
        if (service != null && state.isBound) {
            try {
                // 插件通过 hostCallbacks.requestPanel 请求宿主创建面板
                // 这里宿主直接返回一个 PanelHostBinder
                return PanelHostBinder(panelId, args).asBinder()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request panel for $pluginId", e)
            }
        }
        return null
    }

    // ============ 公共查询 API ============

    /** 获取所有已发现的插件 */
    fun getAllPlugins(): List<PluginInfo> = plugins.values.toList()

    /** 获取已启用的插件 */
    fun getEnabledPlugins(): List<PluginInfo> = plugins.values.filter { it.isEnabled }.toList()

    /** 获取实现了特定扩展点的插件 */
    fun getPluginsWithExtension(ext: PluginInfo.ExtensionPoint): List<PluginInfo> {
        return plugins.values.filter { it.hasExtension(ext) }.toList()
    }

    /** 根据 ID 获取插件 */
    fun getPlugin(pluginId: String): PluginInfo? = plugins[pluginId]

    /** 获取插件运行时状态 */
    fun getPluginState(pluginId: String): PluginRuntimeState? = pluginStates[pluginId]

    /** 检查插件是否已绑定 */
    fun isPluginBound(pluginId: String): Boolean = pluginStates[pluginId]?.isBound == true

    // ============ 事件监听器 ============

    interface PluginEventListener {
        fun onPluginsChanged() {}
        fun onPluginStateChanged(pluginId: String) {}
        fun onPanelReady(pluginId: String) {}
        fun onToast(message: String) {}
        fun onPermissionRequested(pluginId: String, permissions: List<String>) {}
        fun onPanelResizeRequest(pluginId: String, width: Int, height: Int) {}
        fun onPluginSearchQuery(pluginId: String, query: String) {}
        fun onSearchResultsReceived(query: String, results: List<SearchResultItem>) {}
    }

    fun addListener(listener: PluginEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: PluginEventListener) {
        listeners.remove(listener)
    }

    // ============ 内部通知方法 ============

    private fun notifyPluginsChanged() {
        for (listener in listeners) {
            try { listener.onPluginsChanged() } catch (e: Exception) { Log.e(TAG, "Listener error", e) }
        }
    }

    private fun notifyPluginStateChanged(pluginId: String) {
        for (listener in listeners) {
            try { listener.onPluginStateChanged(pluginId) } catch (e: Exception) { Log.e(TAG, "Listener error", e) }
        }
    }

    private fun notifyPanelReady(pluginId: String) {
        for (listener in listeners) {
            try { listener.onPanelReady(pluginId) } catch (e: Exception) { Log.e(TAG, "Listener error", e) }
        }
    }

    private fun notifyToast(message: String) {
        for (listener in listeners) {
            try { listener.onToast(message) } catch (e: Exception) { Log.e(TAG, "Listener error", e) }
        }
    }

    private fun notifyPermissionRequested(pluginId: String, permissions: List<String>) {
        for (listener in listeners) {
            try { listener.onPermissionRequested(pluginId, permissions) } catch (e: Exception) { Log.e(TAG, "Listener error", e) }
        }
    }

    private fun notifyPanelResizeRequest(pluginId: String, width: Int, height: Int) {
        for (listener in listeners) {
            try { listener.onPanelResizeRequest(pluginId, width, height) } catch (e: Exception) { Log.e(TAG, "Listener error", e) }
        }
    }

    private fun notifyPluginSearchQuery(pluginId: String, query: String) {
        for (listener in listeners) {
            try { listener.onPluginSearchQuery(pluginId, query) } catch (e: Exception) { Log.e(TAG, "Listener error", e) }
        }
    }

    private fun notifySearchResultsReceived(query: String, results: java.util.List<SearchResultItem>) {
        for (listener in listeners) {
            try { listener.onSearchResultsReceived(query, results.toList()) } catch (e: Exception) { Log.e(TAG, "Listener error", e) }
        }
    }

    // ============ 面板宿主 Binder 实现 ============

    private inner class PanelHostBinder(
        private val panelId: String,
        private val initialArgs: Bundle?
    ) : com.explorercore.plugin.IPanelHost.Stub() {

        override fun onUpdate(data: Bundle?) {
            // 宿主收到插件请求更新面板 UI
            Log.d(TAG, "PanelHostBinder.onUpdate: $panelId")
            // TODO: 实际更新对应面板的 UI
        }

        override fun showToast(message: String) {
            notifyToast(message)
        }

        override fun requestResize(width: Int, height: Int) {
            notifyPanelResizeRequest(panelId, width, height)
        }
    }

    /**
     * 清理资源
     */
    fun shutdown() {
        scope.coroutineContext.cancelChildren()
        // 解绑所有插件
        for (pluginId in plugins.keys) {
            unbindPlugin(pluginId)
        }
        serviceConnections.clear()
        plugins.clear()
        pluginStates.clear()
    }

    /**
     * 刷新所有插件（重新扫描）
     */
    fun refreshPlugins() {
        taskDispatcher.submitSimple(
            name = "PluginRefresh",
            priority = TaskPriority.HIGH,
            executor = { discoverAndLoadPlugins() }
        )
    }

    /**
     * 刷新特定包名的插件
     */
    fun refreshPlugin(packageName: String) {
        taskDispatcher.submitSimple(
            name = "PluginRefresh_$packageName",
            priority = TaskPriority.NORMAL,
            executor = { ctx ->
                try {
                    val pluginInfo = PluginDescriptorParser.parse(application, packageName)
                    if (pluginInfo.satisfiesVersion(BuildConfig.VERSION_NAME)) {
                        // 如果已存在，先解绑旧的
                        val existingPlugin = plugins[pluginInfo.id]
                        if (existingPlugin != null) {
                            unbindPlugin(pluginInfo.id)
                        }
                        plugins[pluginInfo.id] = pluginInfo
                        pluginStates[pluginInfo.id] = PluginRuntimeState(pluginInfo.id)
                        if (pluginInfo.isEnabled) {
                            bindPluginService(pluginInfo)
                        }
                        notifyPluginsChanged()
                        Log.d(TAG, "Refreshed plugin: ${pluginInfo.id}")
                        TaskResult.success(Unit)
                    } else {
                        TaskResult.failure(Exception("Version mismatch"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh plugin for $packageName", e)
                    TaskResult.failure(e)
                }
            }
        )
    }

    /**
     * 移除插件（包被卸载）
     */
    fun removePlugin(packageName: String) {
        // 找到对应的插件 ID
        val pluginId = plugins.entries.find { it.value.packageName == packageName }?.key
        pluginId?.let {
            unbindPlugin(it)
            plugins.remove(it)
            pluginStates.remove(it)
            serviceConnections.remove(it)
            notifyPluginsChanged()
            Log.d(TAG, "Removed plugin: $it")
        }
    }
}