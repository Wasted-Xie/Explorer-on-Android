// IHostCallbacks.aidl
package com.explorercore.plugin;

import com.explorercore.plugin.SearchResultItem;
import com.explorercore.plugin.IPanelHost;

/**
 * Callbacks that host provides to plugin for communication.
 */
interface IHostCallbacks {
    /**
     * Called by plugin to notify host of an event.
     * @param eventCode Event code defined by plugin/host contract
     * @param data Optional data bundle
     */
    void onPluginEvent(in int eventCode, in Bundle data);

    /**
     * Request host to create a UI panel for the plugin.
     * @param panelId Unique identifier for the panel type
     * @param initialArgs Initial arguments for the panel
     * @return Binder to interact with the created panel (e.g., IPanelHost)
     */
    IBinder requestPanel(in String panelId, in Bundle initialArgs);

    /**
     * Notify host of search results from a plugin implementing search provider.
     * @param query The search query
     * @param results List of search result items
     */
    void notifySearchResults(in String query, in List<SearchResultItem> results);

    // ========== 任务管理扩展 ==========

    /**
     * 提交任务到宿主任务调度器
     * @param taskId 任务唯一ID（由宿主生成并返回）
     * @param taskName 任务名称
     * @param priority 优先级 (0=LOW, 5=NORMAL, 10=HIGH, 20=CRITICAL)
     * @param payload 任务负载
     * @param executorType 执行器类型标识（宿主侧预注册的执行器）
     * @return 任务ID
     */
    String submitTask(in String taskName, in int priority, in Bundle payload, in String executorType);

    /**
     * 取消任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    boolean cancelTask(in String taskId);

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态 (0=PENDING, 1=QUEUED, 2=RUNNING, 3=COMPLETED, 4=FAILED, 5=CANCELLED, 6=TIMEOUT)
     */
    int getTaskState(in String taskId);

    /**
     * 调度持久化后台任务
     * @param taskType 任务类型
     * @param payload JSON字符串负载
     * @param delayMillis 延迟毫秒数
     * @param requiresCharging 是否需要充电
     * @param requiresNetwork 是否需要网络
     */
    void scheduleBackgroundTask(in String taskType, in String payload, in long delayMillis, in boolean requiresCharging, in boolean requiresNetwork);

    /**
     * 调度周期性后台任务
     * @param taskType 任务类型
     * @param intervalMillis 间隔毫秒数
     * @param payload JSON字符串负载
     * @param requiresCharging 是否需要充电
     * @param requiresNetwork 是否需要网络
     */
    void schedulePeriodicBackgroundTask(in String taskType, in long intervalMillis, in String payload, in boolean requiresCharging, in boolean requiresNetwork);
}