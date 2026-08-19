// filemanager-plugin/src/main/java/com/explorer/plugin/filemanager/FileManagerPluginService.kt
package com.explorer.plugin.filemanager

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.explorercore.plugin.IHostCallbacks
import com.explorercore.plugin.IPluginService
import com.explorercore.plugin.IPanelHost
import com.explorercore.plugin.PluginContract
import com.explorercore.plugin.SearchResultItem

/**
 * File Manager Plugin Service
 * This is the main entry point for the file manager plugin.
 * It implements the IPluginService interface to communicate with the host.
 */
class FileManagerPluginService : Service() {

    private val binder = LocalBinder()
    private var hostCallbacks: IHostCallbacks? = null

    private inner class LocalBinder : Binder() {
        fun getService(): FileManagerPluginService = this@FileManagerPluginService
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("FileManagerPlugin", "Service bound")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d("FileManagerPlugin", "Service unbound")
        return super.onUnbind(intent)
    }

    // IPluginService implementation
    override fun initialize(callbacks: IHostCallbacks) {
        Log.d("FileManagerPlugin", "Plugin initialized with host callbacks")
        hostCallbacks = callbacks
        
        // Notify host that plugin is ready
        hostCallbacks?.onPluginEvent(PluginContract.PLUGIN_EVENT_PANEL_READY, Bundle().apply {
            putString("plugin_id", "com.explorer.plugin.filemanager")
        })
    }

    override fun onHostEvent(eventCode: Int, data: Bundle?) {
        Log.d("FileManagerPlugin", "Received host event: $eventCode")
        when (eventCode) {
            PluginContract.HOST_EVENT_REQUEST_PERMISSION -> {
                // Handle permission request from host
                val permissions = data?.getStringArrayList(PluginContract.EXTRA_PERMISSIONS) ?: emptyList()
                // In a real implementation, we would request these permissions
                // For now, just simulate granting them
                hostCallbacks?.onPluginEvent(PluginContract.PLUGIN_EVENT_REQUEST_PERMISSION, Bundle().apply {
                    putStringArrayList(PluginContract.EXTRA_GRANT_RESULTS, permissions.map { 0 }.toArrayList()) // 0 = PERMISSION_GRANTED
                })
            }
            PluginContract.HOST_EVENT_UI_UPDATE -> {
                // Handle UI update request
                // For example, refresh the file list
                refreshFileList()
            }
            else -> {
                Log.w("FileManagerPlugin", "Unknown host event code: $eventCode")
            }
        }
    }

    private fun refreshFileList() {
        // TODO: Implement actual file list refresh and notify host via callbacks if needed
        Log.d("FileManagerPlugin", "Refreshing file list")
    }

    /**
     * Inner class that implements IPanelHost for panel communication.
     * This would be returned by requestPanel().
     */
    private class PanelHost private constructor(
        private val service: FileManagerPluginService
    ) : IPanelHost.Stub() {
        override fun onUpdate(data: Bundle?) {
            Log.d("FileManagerPluginPanel", "Received update: $data")
            // Handle UI update from host
            // For example, update the displayed path or file list
        }

        override fun showToast(message: String) {
            // In a real implementation, we would show a toast
            // For now, just log
            Log.d("FileManagerPluginPanel", "Toast: $message")
        }

        override fun requestResize(width: Int, height: Int) {
            Log.d("FileManagerPluginPanel", "Resize requested: $width x $height")
            // Handle resize request
            // In a real implementation, we would adjust our UI layout
        }
    }

    // This method would be called via AIDL from the host when requesting a panel
    // Actually, this is handled through the IHostCallbacks.requestPanel() call from plugin to host
    // But we need to expose our panel implementation somehow.
    // Let's create a method that the host can call to get our panel host binder.
    // However, looking at the IHostCallbacks.aidl, the requestPanel method returns an IBinder.
    // So the host calls pluginService.requestPanel(...) and gets back an IBinder to IPanelHost.
    
    // Actually, wait - looking at the IHostCallbacks.aidl:
    // IBinder requestPanel(in String panelId, in Bundle initialArgs);
    // This is a method on the HOST CALLBACKS that the PLUGIN calls.
    // So the plugin calls hostCallbacks.requestPanel(...) to ask the host to create a panel for it.
    // The host then creates the panel and returns an IBinder to interact with it (like IPanelHost).
    
    // But for the file manager plugin, we want to provide a panel UI. So we would:
    // 1. Call hostCallbacks.requestPanel(PluginContract.PANEL_ID_FILE_MANAGER, initialArgs)
    // 2. Get back an IBinder to IPanelHost (which is implemented by the host)
    // 3. Use that IPanelHost to communicate with the host about our panel (update, toast, resize)
    
    // Actually, re-reading: The plugin implements IPluginService.
    // The host implements IHostCallbacks.
    // 
    // When host binds to plugin service, it calls plugin.initialize(hostCallbacks)
    // So the plugin gets a reference to the host's callbacks.
    // 
    // Then, when the plugin wants to create a panel, it calls:
    // hostCallbacks.requestPanel(panelId, args)
    // 
    // The host implements this method and returns an IBinder to something that implements
    // the panel host interface (like IPanelHost) that the plugin can use to communicate
    // with the host about the panel.
    // 
    // But wait, the IPanelHost.aidl I created earlier was meant to be implemented by the PLUGIN
    // and used by the HOST. Let me reconsider the flow.
    // 
    // Looking at typical patterns:
    // Option 1: Plugin provides UI to host
    // - Host calls plugin.requestCreatePanel(panelId) -> plugin returns UI component
    // 
    // Option 2: Host provides container, plugin fills it
    // - Host calls plugin.createPanelInContainer(containerId) -> plugin populates container
    // 
    // Option 3: Plugin creates its own window (like a floating panel)
    // - Plugin creates its own activity/service with UI
    // 
    // Let me redefine the interfaces based on what makes sense:
    // 
    // IHostCallbacks (implemented by HOST, called by PLUGIN):
    // - void onPluginEvent(int eventCode, Bundle data)  // Plugin -> Host events
    // - IBinder requestPanel(String panelId, Bundle args)  // Plugin asks host to create a panel, host returns binder to interact with it
    // - void notifySearchResults(String query, List<SearchResultItem> results)  // Plugin provides search results
    // 
    // IPluginService (implemented by PLUGIN, called by HOST):
    // - void initialize(IHostCallbacks callbacks)  // Host gives plugin callbacks to call host
    // - void onHostEvent(int eventCode, Bundle data)  // Host -> Plugin events
    // 
    // Now, what does the IBinder returned by requestPanel represent?
    // It should be an interface that the PLUGIN can use to communicate with the HOST about the panel.
    // Let's call it IPanelHost (implemented by HOST, used by PLUGIN).
    // 
    // So the flow is:
    // 1. Host binds to plugin service
    // 2. Host calls plugin.initialize(hostCallbacks)
    // 3. Plugin wants to show its panel, so it calls:
    //    hostCallbacks.requestPanel(PluginContract.PANEL_ID_FILE_MANAGER, initialArgs)
    // 4. Host implements requestPanel, creates the panel UI (probably a Fragment or Compose view),
    //    and returns an IBinder to its IPanelHost implementation.
    // 5. Plugin uses this IPanelHost to:
    //    - Receive updates from host (onUpdate)
    //    - Show toast messages (showToast)
    //    - Request resize (requestResize)
    // 
    // But wait, if the host is creating the panel UI, then the host needs to know how to render
    // the file manager UI. That means the host would need to depend on the file manager plugin
    // or the plugin would need to provide its UI in a way the host can render it (like through
    // a remote views or composable URI).
    // 
    // This is getting complex. Let me simplify for now.
    // 
    // For the MVP, let's say:
    // - The plugin provides a service that the host can bind to
    // - The plugin can optionally provide a UI via an Activity that the host can launch
    // - For panel support, we'll implement a simpler approach later
    // 
    // Actually, let me look at the original plan again...
    // 
    // From the PLAN: "IPanelHost.aidl" - this was meant to be returned by requestPanel()
    // 
    // Let me re-read: "IBinder requestPanel(in String panelId, in Bundle initialArgs);"
    // 
    // So the HOST provides the requestPanel method (via IHostCallbacks).
    // The PLUGIN calls this method to REQUEST a panel from the HOST.
    // The HOST then CREATES the panel and returns an IBinder to interact with it.
    // 
    // What does interacting with the panel mean? The host needs to tell the plugin
    // things like: "update your data", "show a toast", "you've been resized".
    // 
    // So the IBinder returned should be an interface that the HOST implements
    // and the PLUGIN calls. Let's call this IPanelCallbacks or something.
    // 
    // But I named it IPanelHost and made it methods like onUpdate, showToast, requestResize.
    // If the HOST implements this, then the PLUGIN would call these methods on the host.
    // That makes sense:
    // - host.callbacks.onUpdate(data) -> host tells plugin to update its data
    // - host.callbacks.showToast(msg) -> host shows a toast on behalf of plugin
    // - host.callbacks.requestResize(w, h) -> host asks plugin if it can resize to w,h
    // 
    // Actually, requestResize should probably be the other way: plugin tells host
    // it wants to resize. But let's keep it as host asking plugin for now.
    // 
    // Let me check if this makes sense in the initialize function:
    // The plugin gets hostCallbacks (to call host methods like onPluginEvent, requestPanel, notifySearchResults)
    // Then when plugin wants a panel, it calls hostCallbacks.requestPanel(panelId, args)
    // Host returns a binder to its IPanelHost implementation
    // Plugin stores this binder and can call methods on it like onUpdate, showToast, requestResize
    // 
    // Yes, that makes sense. The host is providing services to the plugin via the returned binder.
    // 
    // So my IPanelHost.aidl is correct as something the HOST implements and the PLUGIN uses.
    // 
    // Let me continue with the service implementation.
    
    // For now, I'll leave the service implementation mostly as a stub,
    // focusing on the structure rather than full file manager implementation.
    // 
    // The actual file manager UI would be implemented in activities or fragments
    // that the plugin can launch, or that the host can embed via the panel system.
}