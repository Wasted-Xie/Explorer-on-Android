// PluginContract.java
package com.explorercore.plugin;

/**
 * Constants and contracts for the plugin system.
 */
public final class PluginContract {
    private PluginContract() { /* Utility class - prevent instantiation */ }

    // Event codes for host -> plugin communication (1000-1999)
    public static final int HOST_EVENT_REQUEST_PERMISSION = 1000;
    public static final int HOST_EVENT_UI_UPDATE = 1001;
    public static final int HOST_EVENT_THEME_CHANGED = 1002;
    public static final int HOST_EVENT_LOW_MEMORY = 1003;
    public static final int HOST_EVENT_SEARCH_QUERY = 1004;
    public static final int HOST_EVENT_GESTURE_DETECTED = 1005;
    // Task management events (1100-1199)
    public static final int HOST_EVENT_TASK_SUBMITTED = 1100;
    public static final int HOST_EVENT_TASK_PROGRESS = 1101;
    public static final int HOST_EVENT_TASK_COMPLETED = 1102;
    public static final int HOST_EVENT_TASK_CANCELLED = 1103;

    // Event codes for plugin -> host communication (2000-2999)
    public static final int PLUGIN_EVENT_PANEL_READY = 2000;
    public static final int PLUGIN_EVENT_SHOW_TOAST = 2001;
    public static final int PLUGIN_EVENT_REQUEST_PERMISSION = 2002;
    public static final int PLUGIN_EVENT_PANEL_RESIZE_REQUEST = 2003;
    public static final int PLUGIN_EVENT_SEARCH_QUERY = 2004;
    public static final int PLUGIN_EVENT_REGISTER_SHORTCUT = 2005;
    public static final int PLUGIN_EVENT_UNREGISTER_SHORTCUT = 2006;
    // Task management events (2100-2199)
    public static final int PLUGIN_EVENT_SUBMIT_TASK = 2100;
    public static final int PLUGIN_EVENT_CANCEL_TASK = 2101;
    public static final int PLUGIN_EVENT_GET_TASK_STATE = 2102;
    public static final int PLUGIN_EVENT_GET_TASK_RESULT = 2103;
    public static final int PLUGIN_EVENT_SCHEDULE_BACKGROUND_TASK = 2104;
    public static final int PLUGIN_EVENT_SCHEDULE_PERIODIC_TASK = 2105;

    // Panel IDs
    public static final String PANEL_ID_FILE_MANAGER = "file_manager";
    public static final String PANEL_ID_TASKBAR = "taskbar";
    public static final String PANEL_ID_START_MENU = "start_menu";
    public static final String PANEL_ID_WIDGETS = "widgets";

    // Extra keys for bundles
    public static final String EXTRA_PERMISSIONS = "extra_permissions";
    public static final String EXTRA_GRANT_RESULTS = "extra_grant_results";
    public static final String EXTRA_SEARCH_QUERY = "extra_search_query";
    public static final String EXTRA_PANEL_ID = "extra_panel_id";
    // Task extras
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TASK_NAME = "extra_task_name";
    public static final String EXTRA_TASK_PRIORITY = "extra_task_priority";
    public static final String EXTRA_TASK_PAYLOAD = "extra_task_payload";
    public static final String EXTRA_TASK_EXECUTOR_TYPE = "extra_task_executor_type";
    public static final String EXTRA_TASK_PROGRESS = "extra_task_progress";
    public static final String EXTRA_TASK_MESSAGE = "extra_task_message";
    public static final String EXTRA_TASK_RESULT = "extra_task_result";
    public static final String EXTRA_TASK_ERROR = "extra_task_error";
    public static final String EXTRA_TASK_IS_RETRYABLE = "extra_task_is_retryable";
    public static final String EXTRA_BACKGROUND_TASK_TYPE = "extra_background_task_type";
    public static final String EXTRA_BACKGROUND_TASK_DELAY = "extra_background_task_delay";
    public static final String EXTRA_BACKGROUND_TASK_INTERVAL = "extra_background_task_interval";
    public static final String EXTRA_BACKGROUND_REQUIRES_CHARGING = "extra_background_requires_charging";
    public static final String EXTRA_BACKGROUND_REQUIRES_NETWORK = "extra_background_requires_network";
}