// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // 如果需要 Kotlin，取消注释下面这行
    // alias(libs.plugins.kotlin.android) apply false
}

// 依赖仓库配置（替代已废弃的 allprojects）
// 在 settings.gradle.kts 中配置 repositoryMode 后，这里不需要重复配置

// 清理任务
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
