// IPluginService.aidl
package com.explorercore.plugin;

import com.explorercore.plugin.IHostCallbacks;

/**
 * Interface for plugin service that hosts implement and plugins bind to.
 * This is the main communication channel between host and plugin.
 */
interface IPluginService {
    /**
     * Initialize the plugin with host callbacks.
     * @param callbacks Host-provided callbacks for plugin to communicate with host
     */
    void initialize(in IHostCallbacks callbacks);

    /**
     * Called by host to notify plugin of an event.
     * @param eventCode Event code defined by host/plugin contract
     * @param data Optional data bundle
     */
    void onHostEvent(in int eventCode, in Bundle data);
}