// host/src/main/java/com/explorer/launcher/host/plugin/PluginModule.kt
package com.explorer.launcher.host.plugin

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PluginModule {

    @Provides
    @Singleton
    fun providePluginManager(application: Application): PluginManager {
        return PluginManager(application)
    }
}