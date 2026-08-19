// host/src/main/java/com/explorer/launcher/host/task/TaskModule.kt
package com.explorer.launcher.host.task

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object TaskModule {

    @Provides
    @Singleton
    fun provideTaskDispatcher(
        application: Application,
        scope: CoroutineScope
    ): TaskDispatcher {
        return TaskDispatcher(application, scope)
    }

    @Provides
    @Singleton
    fun provideTaskScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    /**
     * WorkManager 配置（可选：自定义线程池等）
     */
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(Executors.newFixedThreadPool(4))
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        application: Application,
        config: Configuration
    ): WorkManager {
        WorkManager.initialize(application, config)
        return WorkManager.getInstance(application)
    }
}