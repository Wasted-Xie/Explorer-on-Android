// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

// 通用配置
allprojects {
    // 这里可以添加所有项目通用的配置
    // 例如：添加统一的仓库配置、版本管理等
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
