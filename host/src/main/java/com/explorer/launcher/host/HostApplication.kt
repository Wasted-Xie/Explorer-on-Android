// host/src/main/java/com/explorer/launcher/host/HostApplication.kt
package com.explorer.launcher.host

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HostApplication : Application() {
    companion object {
        lateinit var instance: HostApplication
            private set
    }

    @Inject
    lateinit var pluginManager: com.explorer.launcher.host.plugin.PluginManager

    private val pluginReceiver = PluginReceiver()

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize plugin manager
        pluginManager.initialize()
        // Register package change receiver
        registerPackageReceiver()
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(pluginReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            unregisterReceiver(pluginReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        pluginManager.shutdown()
    }

    fun getAppContext(): Context = applicationContext
}