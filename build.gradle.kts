// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // 如果需要 Kotlin，取消注释
    // alias(libs.plugins.kotlin.android) apply false
}

// 配置所有项目的仓库和依赖解析
allprojects {
    repositories {
        google()
        mavenCentral()
        // 国内镜像（如需要）
        // maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

// 清理任务
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
