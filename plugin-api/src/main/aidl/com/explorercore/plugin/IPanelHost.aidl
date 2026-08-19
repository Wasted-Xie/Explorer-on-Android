// IPanelHost.aidl
package com.explorercore.plugin;

/**
 * Interface for host to communicate with a plugin's panel UI.
 * Returned by PluginService.requestPanel().
 */
interface IPanelHost {
    /**
     * Notify the panel that it should update its UI based on new data.
     * @param data Bundle containing update data
     */
    void onUpdate(in Bundle data);

    /**
     * Notify the panel to show a toast message.
     * @param message Text to show
     */
    void showToast(in String message);

    /**
     * Request the panel to be resized.
     * @param width  New width in pixels, or -1 for no change
     * @param height New height in pixels, or -1 for no change
     */
    void requestResize(in int width, in int height);
}